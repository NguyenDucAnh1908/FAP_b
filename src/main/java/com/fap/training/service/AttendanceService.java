package com.fap.training.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.ForbiddenException;
import com.fap.common.exception.NotFoundException;
import com.fap.training.dto.AttendanceItemRequest;
import com.fap.training.dto.AttendanceRecordResponse;
import com.fap.training.dto.UpdateAttendanceRequest;
import com.fap.training.entity.AttendanceRecord;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.AttendanceCheckInMethod;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.AttendanceRecordMapper;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import com.fap.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

	private final TrainingSessionRepository trainingSessionRepository;
	private final TrainingRegistrationRepository trainingRegistrationRepository;
	private final AttendanceRecordRepository attendanceRecordRepository;
	private final UserRepository userRepository;
	private final AttendanceRecordMapper attendanceRecordMapper;
	private final AuditLogService auditLogService;

	public AttendanceService(
			TrainingSessionRepository trainingSessionRepository,
			TrainingRegistrationRepository trainingRegistrationRepository,
			AttendanceRecordRepository attendanceRecordRepository,
			UserRepository userRepository,
			AttendanceRecordMapper attendanceRecordMapper,
			AuditLogService auditLogService) {
		this.trainingSessionRepository = trainingSessionRepository;
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.attendanceRecordRepository = attendanceRecordRepository;
		this.userRepository = userRepository;
		this.attendanceRecordMapper = attendanceRecordMapper;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public List<AttendanceRecordResponse> list(Long trainingSessionId) {
		ensureSessionExists(trainingSessionId);
		return attendanceRecordRepository.findByTrainingSessionIdOrderByUserFullNameAsc(trainingSessionId).stream()
				.map(attendanceRecordMapper::toResponse)
				.toList();
	}

	@Transactional
	public List<AttendanceRecordResponse> upsert(Long trainingSessionId, UpdateAttendanceRequest request, Long currentUserId) {
		TrainingSession session = trainingSessionRepository.findWithClassAndTrainerById(trainingSessionId)
				.orElseThrow(() -> new NotFoundException("Training session not found"));
		if (session.getStatus() == TrainingSessionStatus.Canceled) {
			throw new ConflictException("ATTENDANCE_SESSION_CANCELED", "Attendance cannot be updated for canceled training session");
		}
		// Post-completion corrections require an explanation on every record for audit trail (§6.5/§7.5)
		boolean isPostCompletion = session.getStatus() == TrainingSessionStatus.Completed;
		if (isPostCompletion) {
			validateCorrectionReasons(request.records());
		}
		validateNoDuplicateUsers(request.records());
		Map<Long, TrainingRegistration> registeredUsers = loadRegisteredUsers(trainingSessionId);
		List<AttendanceRecord> records = request.records().stream()
				.map(item -> upsertRecord(session, item, registeredUsers, currentUserId))
				.toList();
		attendanceRecordRepository.saveAll(records);
		String auditAction = isPostCompletion ? "UPSERT_ATTENDANCE_CORRECTION" : "UPSERT_ATTENDANCE";
		auditLogService.record(auditAction, "training_session", trainingSessionId);
		return records.stream()
				.map(attendanceRecordMapper::toResponse)
				.toList();
	}

	private AttendanceRecord upsertRecord(
			TrainingSession session,
			AttendanceItemRequest item,
			Map<Long, TrainingRegistration> registeredUsers,
			Long currentUserId) {
		TrainingRegistration registration = registeredUsers.get(item.userId());
		if (registration == null) {
			throw new ConflictException("ATTENDANCE_USER_NOT_REGISTERED", "Attendance user must be registered in the training session");
		}
		validateAttendanceItem(item);
		User user = userRepository.findById(item.userId())
				.orElseThrow(() -> new NotFoundException("User not found"));
		LocalDateTime now = LocalDateTime.now();
		AttendanceRecord record = attendanceRecordRepository
				.findByTrainingSessionIdAndUserId(session.getId(), item.userId())
				.orElseGet(() -> createRecord(session, user, now));
		record.setStatus(item.status());
		record.setCheckInMethod(item.checkInMethod() == null ? AttendanceCheckInMethod.Manual : item.checkInMethod());
		record.setCheckedInAt(resolveCheckedInAt(item, now));
		record.setCorrectionReason(item.correctionReason());
		record.setUpdatedBy(currentUserId);
		record.setUpdatedAt(now);
		return record;
	}

	private AttendanceRecord createRecord(TrainingSession session, User user, LocalDateTime now) {
		AttendanceRecord record = new AttendanceRecord();
		record.setTrainingSession(session);
		record.setUser(user);
		record.setCreatedAt(now);
		return record;
	}

	private Map<Long, TrainingRegistration> loadRegisteredUsers(Long trainingSessionId) {
		return trainingRegistrationRepository
				.findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
						trainingSessionId,
						List.of(TrainingRegistrationStatus.Registered))
				.stream()
				.collect(Collectors.toMap(registration -> registration.getUser().getId(), Function.identity()));
	}

	/**
	 * Self-service QR check-in for a trainee. Only trainees whose registration is in {@code Registered}
	 * status are allowed — {@code Completed} and {@code Canceled} are explicitly rejected so a former
	 * participant cannot scan in after the fact.
	 *
	 * <p>The operation is idempotent: scanning twice simply keeps the existing {@code Present} record.
	 */
	@Transactional
	public AttendanceRecordResponse checkIn(Long trainingSessionId, Long currentUserId) {
		TrainingSession session = trainingSessionRepository.findWithClassAndTrainerById(trainingSessionId)
				.orElseThrow(() -> new NotFoundException("Training session not found"));
		if (session.getStatus() == TrainingSessionStatus.Canceled) {
			throw new ConflictException("ATTENDANCE_SESSION_CANCELED", "Cannot check in to a canceled training session");
		}
		if (session.getStatus() == TrainingSessionStatus.Completed) {
			throw new ConflictException("ATTENDANCE_SESSION_COMPLETED", "Cannot check in to a completed training session");
		}
		TrainingRegistration registration = trainingRegistrationRepository
				.findByTrainingSessionIdAndUserIdAndStatus(
						trainingSessionId, currentUserId, TrainingRegistrationStatus.Registered)
				.orElseThrow(() -> new ForbiddenException("You are not registered for this training session"));
		User user = registration.getUser();
		LocalDateTime now = LocalDateTime.now();
		AttendanceRecord record = attendanceRecordRepository
				.findByTrainingSessionIdAndUserId(trainingSessionId, currentUserId)
				.orElseGet(() -> createRecord(session, user, now));
		record.setStatus(AttendanceStatus.Present);
		record.setCheckInMethod(AttendanceCheckInMethod.QR);
		record.setCheckedInAt(now);
		record.setUpdatedBy(currentUserId);
		record.setUpdatedAt(now);
		attendanceRecordRepository.save(record);
		auditLogService.record("QR_CHECK_IN", "training_session", trainingSessionId);
		return attendanceRecordMapper.toResponse(record);
	}

	private void validateCorrectionReasons(List<AttendanceItemRequest> records) {
		// Every record in a post-completion correction must carry an explanation (§6.5/§7.5)
		boolean anyMissingReason = records.stream()
				.anyMatch(item -> item.correctionReason() == null || item.correctionReason().isBlank());
		if (anyMissingReason) {
			throw new BadRequestException("ATTENDANCE_CORRECTION_REASON_REQUIRED",
					"A correction reason is required for every attendance record when the session is completed");
		}
	}

	private void validateNoDuplicateUsers(List<AttendanceItemRequest> records) {
		Set<Long> userIds = new HashSet<>();
		for (AttendanceItemRequest record : records) {
			if (!userIds.add(record.userId())) {
				throw new BadRequestException("DUPLICATE_ATTENDANCE_USER", "Duplicate attendance user");
			}
		}
	}

	private void validateAttendanceItem(AttendanceItemRequest item) {
		if (item.status() == AttendanceStatus.Absent && item.checkedInAt() != null) {
			throw new BadRequestException("ABSENT_ATTENDANCE_CHECK_IN_NOT_ALLOWED", "Absent attendance cannot have checked in time");
		}
	}

	private LocalDateTime resolveCheckedInAt(AttendanceItemRequest item, LocalDateTime now) {
		if (item.status() == AttendanceStatus.Absent) {
			return null;
		}
		return item.checkedInAt() == null ? now : item.checkedInAt();
	}

	private void ensureSessionExists(Long trainingSessionId) {
		if (!trainingSessionRepository.existsById(trainingSessionId)) {
			throw new NotFoundException("Training session not found");
		}
	}
}
