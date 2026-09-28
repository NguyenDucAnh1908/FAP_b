package com.fap.clazz.service;

import com.fap.clazz.dto.ClassEnrollmentResponse;
import com.fap.clazz.dto.ClassResponse;
import com.fap.clazz.entity.ClassEnrollment;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassEnrollmentSource;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.mapper.ClassEnrollmentMapper;
import com.fap.clazz.mapper.ClassMapper;
import com.fap.clazz.repository.ClassEnrollmentRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.api.PageRequestFactory;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.security.RoleNames;
import com.fap.common.util.TextNormalizer;
import com.fap.notification.service.NotificationService;
import com.fap.training.service.ClassRosterRegistrationService;
import com.fap.user.entity.User;
import com.fap.user.enums.UserStatus;
import com.fap.user.repository.UserRepository;
import com.fap.result.service.CourseResultService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ClassEnrollmentService {

	private final ClassRepository classRepository;
	private final ClassEnrollmentRepository classEnrollmentRepository;
	private final UserRepository userRepository;
	private final ClassRosterRegistrationService classRosterRegistrationService;
	private final ClassEnrollmentMapper classEnrollmentMapper;
	private final ClassMapper classMapper;
	private final AuditLogService auditLogService;
	private final NotificationService notificationService;
	private final CourseResultService courseResultService;
	private final Clock clock;

	public ClassEnrollmentService(
			ClassRepository classRepository,
			ClassEnrollmentRepository classEnrollmentRepository,
			UserRepository userRepository,
			ClassRosterRegistrationService classRosterRegistrationService,
			ClassEnrollmentMapper classEnrollmentMapper,
			ClassMapper classMapper,
			AuditLogService auditLogService,
			NotificationService notificationService,
			CourseResultService courseResultService,
			Clock clock) {
		this.classRepository = classRepository;
		this.classEnrollmentRepository = classEnrollmentRepository;
		this.userRepository = userRepository;
		this.classRosterRegistrationService = classRosterRegistrationService;
		this.classEnrollmentMapper = classEnrollmentMapper;
		this.classMapper = classMapper;
		this.auditLogService = auditLogService;
		this.notificationService = notificationService;
		this.courseResultService = courseResultService;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Page<ClassEnrollmentResponse> list(
			Long classId,
			ClassEnrollmentStatus status,
			String keyword,
			int page,
			int limit,
			String sortBy,
			String order) {
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.ASC, "createdAt", "id"),
				"id", "createdAt", "updatedAt", "status", "enrolledAt");
		return classEnrollmentRepository.searchByClass(classId, status, TextNormalizer.blankToNull(keyword), pageRequest)
				.map(classEnrollmentMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public Page<ClassEnrollmentResponse> listMine(
			Long userId,
			ClassEnrollmentStatus status,
			String keyword,
			int page,
			int limit,
			String sortBy,
			String order) {
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.DESC, "updatedAt", "id"),
				"id", "createdAt", "updatedAt", "status", "enrolledAt");
		return classEnrollmentRepository.searchMine(userId, status, TextNormalizer.blankToNull(keyword), pageRequest)
				.map(classEnrollmentMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public Page<ClassResponse> availableClasses(
			Long userId,
			String keyword,
			int page,
			int limit,
			String sortBy,
			String order) {
		validateActiveTrainee(findTrainee(userId));
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.ASC, "startDate", "id"),
				"id", "createdAt", "name", "classCode", "startDate", "endDate");
		return classRepository.searchAvailableForUser(userId, LocalDate.now(clock), TextNormalizer.blankToNull(keyword), pageRequest)
				.map(classMapper::toResponse);
	}

	@Transactional
	public List<ClassEnrollmentResponse> add(Long classId, Set<Long> userIds, Long currentUserId) {
		FapClass fapClass = classRepository.getWithTrainingProgramForUpdateOrThrow(classId);
		ensureClassAcceptsManagedEnrollment(fapClass);
		List<ClassEnrollmentResponse> responses = new ArrayList<>();
		for (Long userId : userIds) {
			User user = findTrainee(userId);
			validateActiveTrainee(user);
			responses.add(classEnrollmentMapper.toResponse(enroll(fapClass, user, ClassEnrollmentSource.AdminAdded, currentUserId)));
		}
		return responses;
	}

	@Transactional
	public ClassEnrollmentResponse selfEnroll(Long classId, Long userId) {
		FapClass fapClass = classRepository.getWithTrainingProgramForUpdateOrThrow(classId);
		validateSelfEnrollmentWindow(fapClass);
		User user = findTrainee(userId);
		validateActiveTrainee(user);
		return classEnrollmentMapper.toResponse(requestEnrollment(fapClass, user));
	}

	@Transactional
	public ClassEnrollmentResponse approve(Long classId, Long userId, Long currentUserId) {
		FapClass fapClass = classRepository.getWithTrainingProgramForUpdateOrThrow(classId);
		ensureClassAcceptsManagedEnrollment(fapClass);
		ClassEnrollment enrollment = findPendingApproval(classId, userId);
		LocalDateTime now = LocalDateTime.now(clock);
		ClassEnrollmentStatus nextStatus = classEnrollmentRepository.countByFapClassIdAndStatus(
				classId, ClassEnrollmentStatus.Enrolled) < fapClass.getCapacity()
				? ClassEnrollmentStatus.Enrolled
				: ClassEnrollmentStatus.Waitlisted;

		enrollment.setStatus(nextStatus);
		enrollment.setEnrolledAt(nextStatus == ClassEnrollmentStatus.Enrolled ? now : null);
		enrollment.setReviewedAt(now);
		enrollment.setReviewedBy(currentUserId);
		enrollment.setUpdatedAt(now);
		enrollment.setUpdatedBy(currentUserId);
		if (nextStatus == ClassEnrollmentStatus.Enrolled) {
			courseResultService.initializeForEnrollment(enrollment, currentUserId);
			classRosterRegistrationService.registerInAutoEnrollSessions(classId, enrollment.getUser());
		}
		auditLogService.record("APPROVE_CLASS_ENROLLMENT:" + nextStatus.name(), "class_enrollment", enrollment.getId());
		notificationService.create(
				enrollment.getUser().getId(),
				"Class enrollment approved",
				nextStatus == ClassEnrollmentStatus.Enrolled
						? "Your request for " + fapClass.getName() + " has been approved"
						: "Your request for " + fapClass.getName() + " has been approved and added to the waitlist");
		return classEnrollmentMapper.toResponse(enrollment);
	}

	@Transactional
	public ClassEnrollmentResponse reject(Long classId, Long userId, Long currentUserId) {
		FapClass fapClass = classRepository.getWithTrainingProgramForUpdateOrThrow(classId);
		ensureClassAcceptsManagedEnrollment(fapClass);
		ClassEnrollment enrollment = findPendingApproval(classId, userId);
		LocalDateTime now = LocalDateTime.now(clock);
		enrollment.setStatus(ClassEnrollmentStatus.Rejected);
		enrollment.setEnrolledAt(null);
		enrollment.setReviewedAt(now);
		enrollment.setReviewedBy(currentUserId);
		enrollment.setUpdatedAt(now);
		enrollment.setUpdatedBy(currentUserId);
		auditLogService.record("REJECT_CLASS_ENROLLMENT", "class_enrollment", enrollment.getId());
		notificationService.create(
				enrollment.getUser().getId(),
				"Class enrollment rejected",
				"Your request for " + fapClass.getName() + " was not approved");
		return classEnrollmentMapper.toResponse(enrollment);
	}

	@Transactional
	public ClassEnrollmentResponse withdraw(Long classId, Long userId, Long currentUserId) {
		FapClass fapClass = classRepository.getWithTrainingProgramForUpdateOrThrow(classId);
		if (fapClass.getStatus() == ClassStatus.Closed) {
			throw new ConflictException("CLASS_ENROLLMENT_CLOSED", "Closed class enrollment cannot be changed");
		}
		ClassEnrollment enrollment = classEnrollmentRepository.getByFapClassIdAndUserIdOrThrow(classId, userId);
		if (enrollment.getStatus() != ClassEnrollmentStatus.PendingApproval
				&& enrollment.getStatus() != ClassEnrollmentStatus.Enrolled
				&& enrollment.getStatus() != ClassEnrollmentStatus.Waitlisted) {
			throw new ConflictException("CLASS_ENROLLMENT_NOT_WITHDRAWABLE", "Only pending, enrolled or waitlisted enrollment can be withdrawn");
		}

		boolean releasedSeat = enrollment.getStatus() == ClassEnrollmentStatus.Enrolled;
		boolean pendingApproval = enrollment.getStatus() == ClassEnrollmentStatus.PendingApproval;
		LocalDateTime now = LocalDateTime.now(clock);
		enrollment.setStatus(ClassEnrollmentStatus.Withdrawn);
		enrollment.setWithdrawnAt(now);
		enrollment.setCompletedAt(null);
		enrollment.setUpdatedAt(now);
		enrollment.setUpdatedBy(currentUserId);
		if (!pendingApproval) {
			courseResultService.markWithdrawn(enrollment, currentUserId);
			classRosterRegistrationService.cancelFutureSessionRegistrations(classId, userId, now);
		}
		auditLogService.record(
				pendingApproval ? "CANCEL_CLASS_ENROLLMENT_REQUEST" : "WITHDRAW_CLASS_ENROLLMENT",
				"class_enrollment",
				enrollment.getId());
		notificationService.create(
				userId,
				pendingApproval ? "Class enrollment request canceled" : "Class enrollment withdrawn",
				pendingApproval
						? "Your request for " + fapClass.getName() + " has been canceled"
						: "You have left " + fapClass.getName());

		if (releasedSeat) {
			promoteFirstWaitlisted(fapClass, currentUserId, now);
		}
		return classEnrollmentMapper.toResponse(enrollment);
	}

	@Transactional(readOnly = true)
	public void validateCapacity(Long classId, Integer capacity) {
		long enrolledCount = classEnrollmentRepository.countByFapClassIdAndStatus(
				classId, ClassEnrollmentStatus.Enrolled);
		if (enrolledCount > capacity) {
			throw new ConflictException(
					"CLASS_CAPACITY_BELOW_ENROLLED_COUNT",
					"Class capacity cannot be lower than the current enrolled count");
		}
	}

	private ClassEnrollment enroll(FapClass fapClass, User user, ClassEnrollmentSource source, Long actorId) {
		LocalDateTime now = LocalDateTime.now(clock);
		ClassEnrollment enrollment = classEnrollmentRepository.findByFapClassIdAndUserId(fapClass.getId(), user.getId())
				.orElseGet(() -> newEnrollment(fapClass, user, now));
		if (enrollment.getStatus() == ClassEnrollmentStatus.PendingApproval
				|| enrollment.getStatus() == ClassEnrollmentStatus.Enrolled
				|| enrollment.getStatus() == ClassEnrollmentStatus.Waitlisted
				|| enrollment.getStatus() == ClassEnrollmentStatus.Completed) {
			throw new ConflictException("CLASS_ENROLLMENT_EXISTS", "User already has an active or completed class enrollment");
		}

		ClassEnrollmentStatus nextStatus = classEnrollmentRepository.countByFapClassIdAndStatus(
				fapClass.getId(), ClassEnrollmentStatus.Enrolled) < fapClass.getCapacity()
				? ClassEnrollmentStatus.Enrolled
				: ClassEnrollmentStatus.Waitlisted;
		enrollment.setStatus(nextStatus);
		enrollment.setSource(source);
		enrollment.setEnrolledAt(nextStatus == ClassEnrollmentStatus.Enrolled ? now : null);
		enrollment.setWithdrawnAt(null);
		enrollment.setCompletedAt(null);
		enrollment.setReviewedAt(null);
		enrollment.setReviewedBy(null);
		enrollment.setUpdatedAt(now);
		enrollment.setUpdatedBy(actorId);
		if (enrollment.getCreatedBy() == null) {
			enrollment.setCreatedBy(actorId);
		}
		ClassEnrollment saved = classEnrollmentRepository.save(enrollment);

		if (nextStatus == ClassEnrollmentStatus.Enrolled) {
			courseResultService.initializeForEnrollment(saved, actorId);
			classRosterRegistrationService.registerInAutoEnrollSessions(fapClass.getId(), saved.getUser());
		}
		auditLogService.record("CREATE_CLASS_ENROLLMENT:" + nextStatus.name(), "class_enrollment", saved.getId());
		notificationService.create(
				user.getId(),
				"Class enrollment " + nextStatus.name().toLowerCase(),
				"Your enrollment for " + fapClass.getName() + " is " + nextStatus.name());
		return saved;
	}

	private ClassEnrollment requestEnrollment(FapClass fapClass, User user) {
		LocalDateTime now = LocalDateTime.now(clock);
		ClassEnrollment enrollment = classEnrollmentRepository.findByFapClassIdAndUserId(fapClass.getId(), user.getId())
				.orElseGet(() -> newEnrollment(fapClass, user, now));
		if (enrollment.getStatus() == ClassEnrollmentStatus.PendingApproval
				|| enrollment.getStatus() == ClassEnrollmentStatus.Enrolled
				|| enrollment.getStatus() == ClassEnrollmentStatus.Waitlisted
				|| enrollment.getStatus() == ClassEnrollmentStatus.Completed) {
			throw new ConflictException("CLASS_ENROLLMENT_EXISTS", "User already has an active, pending or completed class enrollment");
		}

		enrollment.setStatus(ClassEnrollmentStatus.PendingApproval);
		enrollment.setSource(ClassEnrollmentSource.SelfRegistered);
		enrollment.setEnrolledAt(null);
		enrollment.setWithdrawnAt(null);
		enrollment.setCompletedAt(null);
		enrollment.setReviewedAt(null);
		enrollment.setReviewedBy(null);
		enrollment.setUpdatedAt(now);
		enrollment.setUpdatedBy(user.getId());
		if (enrollment.getCreatedBy() == null) {
			enrollment.setCreatedBy(user.getId());
		}
		ClassEnrollment saved = classEnrollmentRepository.save(enrollment);
		auditLogService.record("REQUEST_CLASS_ENROLLMENT_APPROVAL", "class_enrollment", saved.getId());
		notificationService.create(
				user.getId(),
				"Class enrollment request submitted",
				"Your request for " + fapClass.getName() + " is waiting for approval");
		return saved;
	}

	private ClassEnrollment findPendingApproval(Long classId, Long userId) {
		ClassEnrollment enrollment = classEnrollmentRepository.getByFapClassIdAndUserIdOrThrow(classId, userId);
		if (enrollment.getStatus() != ClassEnrollmentStatus.PendingApproval) {
			throw new ConflictException(
					"CLASS_ENROLLMENT_NOT_REVIEWABLE",
					"Only a pending class enrollment request can be reviewed");
		}
		return enrollment;
	}

	private ClassEnrollment newEnrollment(FapClass fapClass, User user, LocalDateTime now) {
		ClassEnrollment enrollment = new ClassEnrollment();
		enrollment.setFapClass(fapClass);
		enrollment.setUser(user);
		enrollment.setCreatedAt(now);
		enrollment.setUpdatedAt(now);
		return enrollment;
	}

	private void promoteFirstWaitlisted(FapClass fapClass, Long actorId, LocalDateTime now) {
		classEnrollmentRepository
				.findFirstByFapClassIdAndStatusOrderByCreatedAtAscIdAsc(fapClass.getId(), ClassEnrollmentStatus.Waitlisted)
				.ifPresent(enrollment -> {
					enrollment.setStatus(ClassEnrollmentStatus.Enrolled);
					enrollment.setEnrolledAt(now);
					enrollment.setUpdatedAt(now);
					enrollment.setUpdatedBy(actorId);
					courseResultService.initializeForEnrollment(enrollment, actorId);
					classRosterRegistrationService.registerInAutoEnrollSessions(fapClass.getId(), enrollment.getUser());
					auditLogService.record("PROMOTE_CLASS_WAITLIST", "class_enrollment", enrollment.getId());
					notificationService.create(
							enrollment.getUser().getId(),
							"Class waitlist promoted",
							"You have been enrolled in " + fapClass.getName());
				});
	}

	private User findTrainee(Long userId) {
		return userRepository.getWithRolesOrThrow(userId);
	}

	private void validateActiveTrainee(User user) {
		if (user.getStatus() != UserStatus.Active) {
			throw new ConflictException("CLASS_ENROLLMENT_USER_NOT_ACTIVE", "Only active user can enroll in a class");
		}
		if (user.getRoles().stream().noneMatch(role -> RoleNames.TRAINEE.equals(role.getName()))) {
			throw new ConflictException("CLASS_ENROLLMENT_TRAINEE_REQUIRED", "User must have the Trainee role");
		}
	}

	private void ensureClassAcceptsManagedEnrollment(FapClass fapClass) {
		if (fapClass.getStatus() == ClassStatus.Closed) {
			throw new ConflictException("CLASS_ENROLLMENT_CLOSED", "Closed class does not accept enrollments")
					.withMessageKey("error.CLASS_ENROLLMENT_CLOSED.enroll");
		}
	}

	private void validateSelfEnrollmentWindow(FapClass fapClass) {
		LocalDate today = LocalDate.now(clock);
		if (fapClass.getStatus() != ClassStatus.Active) {
			throw new ConflictException("CLASS_NOT_OPEN_FOR_ENROLLMENT", "Only active class accepts self enrollment");
		}
		if (!fapClass.isSelfEnrollmentEnabled()) {
			throw new ConflictException("CLASS_SELF_ENROLLMENT_DISABLED", "Self enrollment is disabled for this class");
		}
		if (fapClass.getEnrollmentStartDate() != null && today.isBefore(fapClass.getEnrollmentStartDate())) {
			throw new ConflictException("CLASS_ENROLLMENT_NOT_STARTED", "Class enrollment has not started");
		}
		if (fapClass.getEnrollmentEndDate() != null && today.isAfter(fapClass.getEnrollmentEndDate())) {
			throw new ConflictException("CLASS_ENROLLMENT_ENDED", "Class enrollment has ended");
		}
	}
}
