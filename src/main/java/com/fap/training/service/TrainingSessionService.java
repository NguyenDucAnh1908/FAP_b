package com.fap.training.service;

import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.security.FapUserPrincipal;
import com.fap.notification.service.NotificationService;
import com.fap.training.dto.CreateTrainingSessionRequest;
import com.fap.training.dto.TrainingSessionResponse;
import com.fap.training.dto.UpdateTrainingSessionRequest;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.TrainingSessionMapper;
import com.fap.training.repository.AttendanceRecordRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import com.fap.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class TrainingSessionService {

	private final TrainingSessionRepository trainingSessionRepository;
	private final TrainingRegistrationRepository trainingRegistrationRepository;
	private final AttendanceRecordRepository attendanceRecordRepository;
	private final ClassRepository classRepository;
	private final ClassTrainerRepository classTrainerRepository;
	private final UserRepository userRepository;
	private final TrainingSessionMapper trainingSessionMapper;
	private final AuditLogService auditLogService;
	private final NotificationService notificationService;

	public TrainingSessionService(
			TrainingSessionRepository trainingSessionRepository,
			TrainingRegistrationRepository trainingRegistrationRepository,
			AttendanceRecordRepository attendanceRecordRepository,
			ClassRepository classRepository,
			ClassTrainerRepository classTrainerRepository,
			UserRepository userRepository,
			TrainingSessionMapper trainingSessionMapper,
			AuditLogService auditLogService,
			NotificationService notificationService) {
		this.trainingSessionRepository = trainingSessionRepository;
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.attendanceRecordRepository = attendanceRecordRepository;
		this.classRepository = classRepository;
		this.classTrainerRepository = classTrainerRepository;
		this.userRepository = userRepository;
		this.trainingSessionMapper = trainingSessionMapper;
		this.auditLogService = auditLogService;
		this.notificationService = notificationService;
	}

	@Transactional(readOnly = true)
	public Page<TrainingSessionResponse> list(
			TrainingSessionStatus status,
			Long classId,
			Long trainerId,
			LocalDate fromDate,
			LocalDate toDate,
			String keyword,
			int page,
			int limit) {
		return listScoped(null, status, classId, trainerId, fromDate, toDate, keyword, page, limit);
	}

	@Transactional(readOnly = true)
	public Page<TrainingSessionResponse> listScoped(
			FapUserPrincipal principal,
			TrainingSessionStatus status,
			Long classId,
			Long trainerId,
			LocalDate fromDate,
			LocalDate toDate,
			String keyword,
			int page,
			int limit) {
		validateDateFilter(fromDate, toDate);
		PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.ASC, "sessionDate", "startTime"));
		return trainingSessionRepository.searchScoped(scopeUserId(principal), status, classId, trainerId, fromDate, toDate, normalize(keyword), pageRequest)
				.map(trainingSessionMapper::toResponse);
	}

	@Transactional
	public TrainingSessionResponse create(CreateTrainingSessionRequest request, Long currentUserId) {
		validateTimeRange(request.sessionDate(), request.startTime(), request.endTime());
		FapClass fapClass = findActiveClass(request.classId());
		User trainer = findAssignedTrainer(fapClass.getId(), request.trainerId());
		validateWithinClassDates(fapClass, request.sessionDate());

		LocalDateTime now = LocalDateTime.now();
		TrainingSession session = new TrainingSession();
		session.setFapClass(fapClass);
		session.setStatus(TrainingSessionStatus.Upcoming);
		session.setEnrolledCount(0);
		applyFields(session, request, trainer);
		session.setCreatedAt(now);
		session.setUpdatedAt(now);
		session.setCreatedBy(currentUserId);
		session.setUpdatedBy(currentUserId);
		TrainingSession saved = trainingSessionRepository.save(session);
		auditLogService.record("CREATE_TRAINING_SESSION", "training_session", saved.getId());
		return trainingSessionMapper.toResponse(saved);
	}

	@Transactional(readOnly = true)
	public TrainingSessionResponse get(Long id) {
		return trainingSessionMapper.toResponse(findSession(id));
	}

	@Transactional
	public TrainingSessionResponse update(Long id, UpdateTrainingSessionRequest request, Long currentUserId) {
		TrainingSession session = findSession(id);
		ensureUpcoming(session);
		validateTimeRange(request.sessionDate(), request.startTime(), request.endTime());
		FapClass fapClass = session.getFapClass();
		validateWithinClassDates(fapClass, request.sessionDate());
		User trainer = findAssignedTrainer(fapClass.getId(), request.trainerId());
		applyFields(session, request, trainer);
		session.setUpdatedAt(LocalDateTime.now());
		session.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_TRAINING_SESSION", "training_session", session.getId());
		return trainingSessionMapper.toResponse(session);
	}

	@Transactional
	public TrainingSessionResponse updateStatus(Long id, TrainingSessionStatus status, Long currentUserId) {
		TrainingSession session = findSession(id);
		validateTransition(session.getStatus(), status);
		if (session.getStatus() == TrainingSessionStatus.Upcoming && status == TrainingSessionStatus.Completed) {
			validateAttendanceReadyForCompletion(session.getId());
			finalizeRegisteredParticipants(session.getId());
		}
		session.setStatus(status);
		session.setUpdatedAt(LocalDateTime.now());
		session.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_TRAINING_SESSION_STATUS:" + status.name(), "training_session", session.getId());
		notifyRegisteredParticipants(session, status);
		return trainingSessionMapper.toResponse(session);
	}

	private TrainingSession findSession(Long id) {
		return trainingSessionRepository.findWithClassAndTrainerById(id)
				.orElseThrow(() -> new NotFoundException("Training session not found"));
	}

	private FapClass findActiveClass(Long classId) {
		FapClass fapClass = classRepository.findWithTrainingProgramById(classId)
				.orElseThrow(() -> new NotFoundException("Class not found"));
		if (fapClass.getStatus() != ClassStatus.Active) {
			throw new ConflictException("TRAINING_SESSION_CLASS_NOT_ACTIVE", "Training session requires an active class");
		}
		return fapClass;
	}

	private User findAssignedTrainer(Long classId, Long trainerId) {
		if (!classTrainerRepository.existsByFapClassIdAndUserId(classId, trainerId)) {
			throw new ConflictException("TRAINING_SESSION_TRAINER_NOT_ASSIGNED", "Trainer must be assigned to the class");
		}
		return userRepository.findById(trainerId)
				.orElseThrow(() -> new NotFoundException("Trainer not found"));
	}

	private void applyFields(TrainingSession session, CreateTrainingSessionRequest request, User trainer) {
		session.setTitle(request.title().trim());
		session.setDescription(request.description());
		session.setTrainer(trainer);
		session.setRoom(request.room());
		session.setSessionDate(request.sessionDate());
		session.setStartTime(request.startTime());
		session.setEndTime(request.endTime());
		session.setSessionType(request.sessionType());
		session.setMeetingLink(request.meetingLink());
		session.setCapacity(request.capacity());
	}

	private void applyFields(TrainingSession session, UpdateTrainingSessionRequest request, User trainer) {
		session.setTitle(request.title().trim());
		session.setDescription(request.description());
		session.setTrainer(trainer);
		session.setRoom(request.room());
		session.setSessionDate(request.sessionDate());
		session.setStartTime(request.startTime());
		session.setEndTime(request.endTime());
		session.setSessionType(request.sessionType());
		session.setMeetingLink(request.meetingLink());
		session.setCapacity(request.capacity());
	}

	private void validateTransition(TrainingSessionStatus current, TrainingSessionStatus target) {
		if (current == target) {
			return;
		}
		if (current != TrainingSessionStatus.Upcoming
				|| (target != TrainingSessionStatus.Completed && target != TrainingSessionStatus.Canceled)) {
			throw new ConflictException("INVALID_TRAINING_SESSION_STATUS_TRANSITION", "Invalid training session status transition");
		}
	}

	private void validateAttendanceReadyForCompletion(Long trainingSessionId) {
		long registeredCount = trainingRegistrationRepository.countByTrainingSessionIdAndStatus(
				trainingSessionId,
				TrainingRegistrationStatus.Registered);
		if (registeredCount == 0) {
			return;
		}
		long attendanceCount = attendanceRecordRepository.countByTrainingSessionId(trainingSessionId);
		if (attendanceCount < registeredCount) {
			throw new ConflictException("TRAINING_SESSION_ATTENDANCE_REQUIRED", "Training session requires attendance for all registered participants before completion");
		}
	}

	private void finalizeRegisteredParticipants(Long trainingSessionId) {
		LocalDateTime now = LocalDateTime.now();
		trainingRegistrationRepository
				.findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
						trainingSessionId,
						java.util.List.of(TrainingRegistrationStatus.Registered))
				.forEach(registration -> {
					registration.setStatus(TrainingRegistrationStatus.Completed);
					registration.setCompletedAt(now);
				});
	}

	private void notifyRegisteredParticipants(TrainingSession session, TrainingSessionStatus status) {
		if (status != TrainingSessionStatus.Completed && status != TrainingSessionStatus.Canceled) {
			return;
		}
		trainingRegistrationRepository
				.findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
						session.getId(),
						java.util.List.of(TrainingRegistrationStatus.Registered, TrainingRegistrationStatus.Completed))
				.forEach(registration -> notificationService.create(
						registration.getUser().getId(),
						"Training session " + status.name().toLowerCase(),
						session.getTitle() + " has been marked as " + status.name()));
	}

	private void ensureUpcoming(TrainingSession session) {
		if (session.getStatus() != TrainingSessionStatus.Upcoming) {
			throw new ConflictException("TRAINING_SESSION_NOT_EDITABLE", "Only upcoming training session can be edited");
		}
	}

	private void validateTimeRange(LocalDate sessionDate, LocalDateTime startTime, LocalDateTime endTime) {
		if (!endTime.isAfter(startTime)) {
			throw new BadRequestException("INVALID_TRAINING_SESSION_TIME_RANGE", "Training session end time must be after start time");
		}
		if (!startTime.toLocalDate().equals(sessionDate) || !endTime.toLocalDate().equals(sessionDate)) {
			throw new BadRequestException("INVALID_TRAINING_SESSION_DATE", "Training session times must match session date");
		}
	}

	private void validateWithinClassDates(FapClass fapClass, LocalDate sessionDate) {
		if (fapClass.getStartDate() != null && sessionDate.isBefore(fapClass.getStartDate())) {
			throw new ConflictException("TRAINING_SESSION_OUTSIDE_CLASS_DATES", "Training session must be within class dates");
		}
		if (fapClass.getEndDate() != null && sessionDate.isAfter(fapClass.getEndDate())) {
			throw new ConflictException("TRAINING_SESSION_OUTSIDE_CLASS_DATES", "Training session must be within class dates");
		}
	}

	private void validateDateFilter(LocalDate fromDate, LocalDate toDate) {
		if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
			throw new BadRequestException("INVALID_TRAINING_SESSION_DATE_FILTER", "From date must be before or equal to to date");
		}
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private Long scopeUserId(FapUserPrincipal principal) {
		return principal == null || principal.roles().contains("Super Admin") ? null : principal.id();
	}
}
