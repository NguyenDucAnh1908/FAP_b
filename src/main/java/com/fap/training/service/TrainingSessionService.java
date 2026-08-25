package com.fap.training.service;

import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.clazz.service.ClassEnrollmentService;
import com.fap.common.audit.AuditLogService;
import com.fap.common.api.PageRequestFactory;
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
import com.fap.training.enums.TrainingSessionType;
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
import java.util.HashSet;
import java.util.Set;

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
	private final ClassEnrollmentService classEnrollmentService;

	public TrainingSessionService(
			TrainingSessionRepository trainingSessionRepository,
			TrainingRegistrationRepository trainingRegistrationRepository,
			AttendanceRecordRepository attendanceRecordRepository,
			ClassRepository classRepository,
			ClassTrainerRepository classTrainerRepository,
			UserRepository userRepository,
			TrainingSessionMapper trainingSessionMapper,
			AuditLogService auditLogService,
			NotificationService notificationService,
			ClassEnrollmentService classEnrollmentService) {
		this.trainingSessionRepository = trainingSessionRepository;
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.attendanceRecordRepository = attendanceRecordRepository;
		this.classRepository = classRepository;
		this.classTrainerRepository = classTrainerRepository;
		this.userRepository = userRepository;
		this.trainingSessionMapper = trainingSessionMapper;
		this.auditLogService = auditLogService;
		this.notificationService = notificationService;
		this.classEnrollmentService = classEnrollmentService;
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
		return listScoped(null, status, classId, trainerId, fromDate, toDate, keyword, page, limit, null, null);
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
		return listScoped(principal, status, classId, trainerId, fromDate, toDate, keyword, page, limit, null, null);
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
			int limit,
			String sortBy,
			String order) {
		validateDateFilter(fromDate, toDate);
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.ASC, "sessionDate", "startTime"),
				"id", "sessionDate", "startTime", "endTime", "status", "createdAt");
		return trainingSessionRepository.searchScoped(scopeUserId(principal), status, classId, trainerId, fromDate, toDate, normalize(keyword), pageRequest)
				.map(trainingSessionMapper::toResponse);
	}

	@Transactional
	public TrainingSessionResponse create(CreateTrainingSessionRequest request, Long currentUserId) {
		validateTimeRange(request.sessionDate(), request.startTime(), request.endTime());
		FapClass fapClass = findActiveClass(request.classId());
		User trainer = findAssignedTrainer(fapClass.getId(), request.trainerId());
		validateWithinClassDates(fapClass, request.sessionDate());
		validateScheduleConflicts(
				null,
				fapClass.getId(),
				trainer.getId(),
				request.room(),
				request.sessionType(),
				request.startTime(),
				request.endTime());

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
		classEnrollmentService.syncAutoEnrollSession(saved);
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
		validateCapacity(session, request.capacity());
		validateScheduleConflicts(
				session.getId(),
				fapClass.getId(),
				trainer.getId(),
				request.room(),
				request.sessionType(),
				request.startTime(),
				request.endTime());
		applyFields(session, request, trainer);
		classEnrollmentService.syncAutoEnrollSession(session);
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
		if (session.getStatus() == TrainingSessionStatus.Upcoming && status == TrainingSessionStatus.Canceled) {
			cancelActiveRegistrations(session);
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
		session.setRegistrationMode(request.registrationMode());
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
		session.setRegistrationMode(request.registrationMode());
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
		Set<Long> registeredUserIds = new HashSet<>(trainingRegistrationRepository
				.findUserIdsByTrainingSessionIdAndStatus(
						trainingSessionId,
						TrainingRegistrationStatus.Registered));
		if (registeredUserIds.isEmpty()) {
			return;
		}
		Set<Long> attendanceUserIds = new HashSet<>(attendanceRecordRepository
				.findUserIdsByTrainingSessionId(trainingSessionId));
		if (!attendanceUserIds.containsAll(registeredUserIds)) {
			throw new ConflictException("TRAINING_SESSION_ATTENDANCE_REQUIRED", "Training session requires attendance for all registered participants before completion");
		}
	}

	private void validateCapacity(TrainingSession session, Integer capacity) {
		int enrolledCount = session.getEnrolledCount() == null ? 0 : session.getEnrolledCount();
		if (capacity < enrolledCount) {
			throw new ConflictException(
					"TRAINING_SESSION_CAPACITY_BELOW_ENROLLED",
					"Training session capacity cannot be lower than enrolled count");
		}
	}

	private void validateScheduleConflicts(
			Long excludedId,
			Long classId,
			Long trainerId,
			String room,
			TrainingSessionType sessionType,
			LocalDateTime startTime,
			LocalDateTime endTime) {
		if (trainingSessionRepository.countClassScheduleConflicts(
				excludedId, classId, startTime, endTime, TrainingSessionStatus.Canceled) > 0) {
			throw new ConflictException(
					"TRAINING_SESSION_CLASS_SCHEDULE_CONFLICT",
					"The class already has another training session during this time");
		}
		if (trainingSessionRepository.countTrainerScheduleConflicts(
				excludedId, trainerId, startTime, endTime, TrainingSessionStatus.Canceled) > 0) {
			throw new ConflictException(
					"TRAINING_SESSION_TRAINER_SCHEDULE_CONFLICT",
					"The trainer already has another training session during this time");
		}
		String normalizedRoom = normalize(room);
		if (sessionType != TrainingSessionType.Online
				&& normalizedRoom != null
				&& trainingSessionRepository.countRoomScheduleConflicts(
						excludedId, normalizedRoom, startTime, endTime, TrainingSessionStatus.Canceled) > 0) {
			throw new ConflictException(
					"TRAINING_SESSION_ROOM_SCHEDULE_CONFLICT",
					"The room already has another training session during this time");
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

	private void cancelActiveRegistrations(TrainingSession session) {
		LocalDateTime now = LocalDateTime.now();
		trainingRegistrationRepository
				.findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
						session.getId(),
						java.util.List.of(
								TrainingRegistrationStatus.Registered,
								TrainingRegistrationStatus.Waitlist))
				.forEach(registration -> {
					registration.setStatus(TrainingRegistrationStatus.Cancelled);
					registration.setCancelledAt(now);
					registration.setCompletedAt(null);
					notificationService.create(
							registration.getUser().getId(),
							"Training session canceled",
							session.getTitle() + " has been canceled");
				});
		session.setEnrolledCount(0);
	}

	private void notifyRegisteredParticipants(TrainingSession session, TrainingSessionStatus status) {
		if (status != TrainingSessionStatus.Completed) {
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
