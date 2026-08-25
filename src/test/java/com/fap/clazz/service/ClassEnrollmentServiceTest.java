package com.fap.clazz.service;

import com.fap.clazz.dto.ClassEnrollmentResponse;
import com.fap.clazz.entity.ClassEnrollment;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassEnrollmentSource;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.mapper.ClassEnrollmentMapper;
import com.fap.clazz.mapper.ClassMapper;
import com.fap.clazz.repository.ClassEnrollmentRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.notification.service.NotificationService;
import com.fap.result.service.CourseResultService;
import com.fap.role.entity.Role;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import com.fap.user.enums.UserStatus;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClassEnrollmentServiceTest {

	private static final Long CLASS_ID = 70L;
	private static final Long USER_ID = 700L;

	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final ClassEnrollmentRepository classEnrollmentRepository = mock(ClassEnrollmentRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final TrainingSessionRepository trainingSessionRepository = mock(TrainingSessionRepository.class);
	private final TrainingRegistrationRepository trainingRegistrationRepository = mock(TrainingRegistrationRepository.class);
	private final ClassMapper classMapper = mock(ClassMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final NotificationService notificationService = mock(NotificationService.class);
	private final CourseResultService courseResultService = mock(CourseResultService.class);
	private final ClassEnrollmentService service = new ClassEnrollmentService(
			classRepository,
			classEnrollmentRepository,
			userRepository,
			trainingSessionRepository,
			trainingRegistrationRepository,
			new ClassEnrollmentMapper(),
			classMapper,
			auditLogService,
			notificationService,
			courseResultService);

	@BeforeEach
	void saveReturnsEnrollmentWithId() {
		when(classEnrollmentRepository.save(any(ClassEnrollment.class))).thenAnswer(invocation -> {
			ClassEnrollment enrollment = invocation.getArgument(0);
			enrollment.setId(900L);
			return enrollment;
		});
	}

	@Test
	void selfEnrollmentCreatesPendingApprovalRequest() {
		givenOpenClass(2);
		givenActiveTrainee();

		ClassEnrollmentResponse response = service.selfEnroll(CLASS_ID, USER_ID);

		assertThat(response.status()).isEqualTo(ClassEnrollmentStatus.PendingApproval);
		assertThat(response.enrolledAt()).isNull();
		verify(auditLogService).record("REQUEST_CLASS_ENROLLMENT_APPROVAL", "class_enrollment", 900L);
		verify(notificationService).create(eq(USER_ID), anyString(), anyString());
	}

	@Test
	void selfEnrollmentStillRequiresApprovalWhenClassIsFull() {
		givenOpenClass(1);
		givenActiveTrainee();

		ClassEnrollmentResponse response = service.selfEnroll(CLASS_ID, USER_ID);

		assertThat(response.status()).isEqualTo(ClassEnrollmentStatus.PendingApproval);
		assertThat(response.enrolledAt()).isNull();
	}

	@Test
	void approvingPendingRequestEnrollsTraineeWhenSeatRemains() {
		FapClass fapClass = givenOpenClass(2);
		User user = givenActiveTrainee();
		ClassEnrollment pending = enrollment(fapClass, user, ClassEnrollmentStatus.PendingApproval, 901L);
		when(classEnrollmentRepository.findByFapClassIdAndUserId(CLASS_ID, USER_ID)).thenReturn(Optional.of(pending));
		when(classEnrollmentRepository.countByFapClassIdAndStatus(CLASS_ID, ClassEnrollmentStatus.Enrolled)).thenReturn(1L);

		ClassEnrollmentResponse response = service.approve(CLASS_ID, USER_ID, 1L);

		assertThat(response.status()).isEqualTo(ClassEnrollmentStatus.Enrolled);
		assertThat(response.enrolledAt()).isNotNull();
		assertThat(response.reviewedAt()).isNotNull();
		assertThat(response.reviewedBy()).isEqualTo(1L);
		verify(courseResultService).initializeForEnrollment(pending, 1L);
		verify(auditLogService).record("APPROVE_CLASS_ENROLLMENT:Enrolled", "class_enrollment", 901L);
	}

	@Test
	void approvingPendingRequestUsesWaitlistWhenClassIsFull() {
		FapClass fapClass = givenOpenClass(1);
		User user = givenActiveTrainee();
		ClassEnrollment pending = enrollment(fapClass, user, ClassEnrollmentStatus.PendingApproval, 901L);
		when(classEnrollmentRepository.findByFapClassIdAndUserId(CLASS_ID, USER_ID)).thenReturn(Optional.of(pending));
		when(classEnrollmentRepository.countByFapClassIdAndStatus(CLASS_ID, ClassEnrollmentStatus.Enrolled)).thenReturn(1L);

		ClassEnrollmentResponse response = service.approve(CLASS_ID, USER_ID, 1L);

		assertThat(response.status()).isEqualTo(ClassEnrollmentStatus.Waitlisted);
		assertThat(response.enrolledAt()).isNull();
		verify(courseResultService, never()).initializeForEnrollment(any(), anyLong());
	}

	@Test
	void rejectingPendingRequestKeepsTraineeOutOfClass() {
		FapClass fapClass = givenOpenClass(30);
		User user = givenActiveTrainee();
		ClassEnrollment pending = enrollment(fapClass, user, ClassEnrollmentStatus.PendingApproval, 901L);
		when(classEnrollmentRepository.findByFapClassIdAndUserId(CLASS_ID, USER_ID)).thenReturn(Optional.of(pending));

		ClassEnrollmentResponse response = service.reject(CLASS_ID, USER_ID, 1L);

		assertThat(response.status()).isEqualTo(ClassEnrollmentStatus.Rejected);
		assertThat(response.reviewedBy()).isEqualTo(1L);
		verify(auditLogService).record("REJECT_CLASS_ENROLLMENT", "class_enrollment", 901L);
	}

	@Test
	void rejectsReviewWhenRequestIsNotPending() {
		FapClass fapClass = givenOpenClass(30);
		User user = givenActiveTrainee();
		ClassEnrollment enrolled = enrollment(fapClass, user, ClassEnrollmentStatus.Enrolled, 901L);
		when(classEnrollmentRepository.findByFapClassIdAndUserId(CLASS_ID, USER_ID)).thenReturn(Optional.of(enrolled));

		assertThatThrownBy(() -> service.approve(CLASS_ID, USER_ID, 1L))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_ENROLLMENT_NOT_REVIEWABLE");
	}

	@Test
	void pendingRequestCanBeCancelledWithoutCreatingCourseResult() {
		FapClass fapClass = givenOpenClass(30);
		User user = givenActiveTrainee();
		ClassEnrollment pending = enrollment(fapClass, user, ClassEnrollmentStatus.PendingApproval, 901L);
		when(classEnrollmentRepository.findByFapClassIdAndUserId(CLASS_ID, USER_ID)).thenReturn(Optional.of(pending));

		ClassEnrollmentResponse response = service.withdraw(CLASS_ID, USER_ID, USER_ID);

		assertThat(response.status()).isEqualTo(ClassEnrollmentStatus.Withdrawn);
		assertThat(response.withdrawnAt()).isNotNull();
		verify(courseResultService, never()).markWithdrawn(any(), anyLong());
		verify(trainingRegistrationRepository, never()).findFutureByClassAndUser(anyLong(), anyLong(), any(), any());
	}

	@Test
	void rejectsInactiveUser() {
		givenOpenClass(30);
		User user = trainee(UserStatus.Inactive);
		when(userRepository.findWithRolesById(USER_ID)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> service.selfEnroll(CLASS_ID, USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_ENROLLMENT_USER_NOT_ACTIVE");
		verify(classEnrollmentRepository, never()).save(any());
	}

	@Test
	void rejectsUserWithoutTraineeRole() {
		givenOpenClass(30);
		User user = new User();
		user.setId(USER_ID);
		user.setStatus(UserStatus.Active);
		when(userRepository.findWithRolesById(USER_ID)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> service.selfEnroll(CLASS_ID, USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_ENROLLMENT_TRAINEE_REQUIRED");
	}

	@Test
	void rejectsSelfEnrollmentOutsideWindow() {
		FapClass fapClass = givenOpenClass(30);
		fapClass.setEnrollmentStartDate(LocalDate.now().plusDays(1));

		assertThatThrownBy(() -> service.selfEnroll(CLASS_ID, USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_ENROLLMENT_NOT_STARTED");
		verify(userRepository, never()).findWithRolesById(anyLong());
	}

	@Test
	void rejectsSelfEnrollmentAfterWindow() {
		FapClass fapClass = givenOpenClass(30);
		fapClass.setEnrollmentEndDate(LocalDate.now().minusDays(1));

		assertThatThrownBy(() -> service.selfEnroll(CLASS_ID, USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_ENROLLMENT_ENDED");
		verify(userRepository, never()).findWithRolesById(anyLong());
	}

	@Test
	void rejectsSelfEnrollmentWhenClassIsNotActive() {
		FapClass fapClass = givenOpenClass(30);
		fapClass.setStatus(ClassStatus.Planning);

		assertThatThrownBy(() -> service.selfEnroll(CLASS_ID, USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_NOT_OPEN_FOR_ENROLLMENT");
		verify(userRepository, never()).findWithRolesById(anyLong());
	}

	@Test
	void rejectsSelfEnrollmentWhenDisabled() {
		FapClass fapClass = givenOpenClass(30);
		fapClass.setSelfEnrollmentEnabled(false);

		assertThatThrownBy(() -> service.selfEnroll(CLASS_ID, USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_SELF_ENROLLMENT_DISABLED");
		verify(userRepository, never()).findWithRolesById(anyLong());
	}

	@Test
	void allowsSelfEnrollmentWithoutConfiguredWindow() {
		FapClass fapClass = givenOpenClass(30);
		fapClass.setEnrollmentStartDate(null);
		fapClass.setEnrollmentEndDate(null);
		givenActiveTrainee();
		ClassEnrollmentResponse response = service.selfEnroll(CLASS_ID, USER_ID);

		assertThat(response.status()).isEqualTo(ClassEnrollmentStatus.PendingApproval);
	}

	@Test
	void rejectsDuplicateEnrollment() {
		FapClass fapClass = givenOpenClass(30);
		User user = givenActiveTrainee();
		ClassEnrollment existing = enrollment(fapClass, user, ClassEnrollmentStatus.Enrolled, 901L);
		when(classEnrollmentRepository.findByFapClassIdAndUserId(CLASS_ID, USER_ID))
				.thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.selfEnroll(CLASS_ID, USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_ENROLLMENT_EXISTS");
	}

	@Test
	void withdrawnTraineeCanSelfEnrollAgain() {
		FapClass fapClass = givenOpenClass(30);
		User user = givenActiveTrainee();
		ClassEnrollment withdrawn = enrollment(fapClass, user, ClassEnrollmentStatus.Withdrawn, 904L);
		withdrawn.setWithdrawnAt(LocalDateTime.now().minusHours(1));
		when(classEnrollmentRepository.findByFapClassIdAndUserId(CLASS_ID, USER_ID))
				.thenReturn(Optional.of(withdrawn));
		when(classEnrollmentRepository.countByFapClassIdAndStatus(CLASS_ID, ClassEnrollmentStatus.Enrolled))
				.thenReturn(0L);

		ClassEnrollmentResponse response = service.selfEnroll(CLASS_ID, USER_ID);

		assertThat(response.status()).isEqualTo(ClassEnrollmentStatus.PendingApproval);
		assertThat(response.source()).isEqualTo(ClassEnrollmentSource.SelfRegistered);
		assertThat(response.withdrawnAt()).isNull();
		assertThat(response.enrolledAt()).isNull();
	}

	@Test
	void managedEnrollmentDoesNotRequireSelfEnrollmentToBeEnabled() {
		FapClass fapClass = givenOpenClass(30);
		fapClass.setStatus(ClassStatus.Planning);
		fapClass.setSelfEnrollmentEnabled(false);
		givenActiveTrainee();
		when(classEnrollmentRepository.countByFapClassIdAndStatus(CLASS_ID, ClassEnrollmentStatus.Enrolled))
				.thenReturn(0L);

		ClassEnrollmentResponse response = service.add(CLASS_ID, Set.of(USER_ID), 1L).getFirst();

		assertThat(response.status()).isEqualTo(ClassEnrollmentStatus.Enrolled);
		assertThat(response.source()).isEqualTo(ClassEnrollmentSource.AdminAdded);
	}

	@Test
	void withdrawingEnrolledUserPromotesFirstWaitlistedUser() {
		FapClass fapClass = givenOpenClass(1);
		User enrolledUser = givenActiveTrainee();
		User waitlistedUser = trainee(UserStatus.Active);
		waitlistedUser.setId(701L);
		waitlistedUser.setEmail("waitlisted@fap.local");
		ClassEnrollment enrolled = enrollment(fapClass, enrolledUser, ClassEnrollmentStatus.Enrolled, 902L);
		ClassEnrollment waitlisted = enrollment(fapClass, waitlistedUser, ClassEnrollmentStatus.Waitlisted, 903L);
		when(classEnrollmentRepository.findByFapClassIdAndUserId(CLASS_ID, USER_ID))
				.thenReturn(Optional.of(enrolled));
		when(classEnrollmentRepository.findFirstByFapClassIdAndStatusOrderByCreatedAtAscIdAsc(
				CLASS_ID, ClassEnrollmentStatus.Waitlisted)).thenReturn(Optional.of(waitlisted));

		service.withdraw(CLASS_ID, USER_ID, 1L);

		assertThat(enrolled.getStatus()).isEqualTo(ClassEnrollmentStatus.Withdrawn);
		assertThat(waitlisted.getStatus()).isEqualTo(ClassEnrollmentStatus.Enrolled);
		assertThat(waitlisted.getEnrolledAt()).isNotNull();
		verify(auditLogService).record("PROMOTE_CLASS_WAITLIST", "class_enrollment", 903L);
		verify(notificationService).create(eq(701L), anyString(), anyString());
	}

	private FapClass givenOpenClass(int capacity) {
		FapClass fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setName("Java Backend");
		fapClass.setClassCode("JAVA-01");
		fapClass.setStatus(ClassStatus.Active);
		fapClass.setCapacity(capacity);
		fapClass.setSelfEnrollmentEnabled(true);
		fapClass.setEnrollmentStartDate(LocalDate.now().minusDays(1));
		fapClass.setEnrollmentEndDate(LocalDate.now().plusDays(1));
		when(classRepository.findWithTrainingProgramByIdForUpdate(CLASS_ID)).thenReturn(Optional.of(fapClass));
		return fapClass;
	}

	private User givenActiveTrainee() {
		User user = trainee(UserStatus.Active);
		when(userRepository.findWithRolesById(USER_ID)).thenReturn(Optional.of(user));
		return user;
	}

	private User trainee(UserStatus status) {
		Role role = new Role();
		role.setName("Trainee");
		User user = new User();
		user.setId(USER_ID);
		user.setFullName("Trainee User");
		user.setEmail("trainee@fap.local");
		user.setStatus(status);
		user.getRoles().add(role);
		return user;
	}

	private ClassEnrollment enrollment(
			FapClass fapClass,
			User user,
			ClassEnrollmentStatus status,
			Long id) {
		ClassEnrollment enrollment = new ClassEnrollment();
		enrollment.setId(id);
		enrollment.setFapClass(fapClass);
		enrollment.setUser(user);
		enrollment.setStatus(status);
		enrollment.setCreatedAt(LocalDateTime.now().minusDays(1));
		enrollment.setUpdatedAt(LocalDateTime.now().minusDays(1));
		return enrollment;
	}
}
