package com.fap.training.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.metrics.DomainMetrics;
import com.fap.notification.service.NotificationService;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.TrainingRegistrationMapper;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import com.fap.user.enums.UserStatus;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Capacity and waitlist arithmetic for training registration. The row-level lock that makes these
 * transitions safe under concurrency lives in the repository query, so what is worth testing here
 * is that {@code enrolledCount} and the promotion rule stay consistent on every path — an off-by-one
 * either oversells a session or strands a waitlisted trainee forever.
 */
class TrainingRegistrationServiceTest {

	private static final long SESSION_ID = 61L;
	private static final long USER_ID = 500L;

	private final TrainingSessionRepository trainingSessionRepository = mock(TrainingSessionRepository.class);
	private final TrainingRegistrationRepository trainingRegistrationRepository =
			mock(TrainingRegistrationRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final TrainingRegistrationMapper trainingRegistrationMapper = mock(TrainingRegistrationMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final NotificationService notificationService = mock(NotificationService.class);
	private final DomainMetrics domainMetrics = mock(DomainMetrics.class);

	private final TrainingRegistrationService service = new TrainingRegistrationService(
			trainingSessionRepository,
			trainingRegistrationRepository,
			userRepository,
			trainingRegistrationMapper,
			auditLogService,
			notificationService,
			domainMetrics);

	@BeforeEach
	void returnSavedEntity() {
		when(trainingRegistrationRepository.save(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void registersTraineeAndConsumesOneSeatWhenCapacityRemains() {
		TrainingSession session = givenUpcomingSession(30, 5);
		givenActiveUser();
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.empty());

		service.register(SESSION_ID, USER_ID);

		TrainingRegistration saved = captureSaved();
		assertThat(saved.getStatus()).isEqualTo(TrainingRegistrationStatus.Registered);
		assertThat(saved.getRegisteredAt()).isNotNull();
		assertThat(session.getEnrolledCount()).isEqualTo(6);
		verify(auditLogService).record(
				"REGISTER_TRAINING_SESSION:Registered", "training_session", SESSION_ID);
		verify(notificationService).create(eq(USER_ID), anyString(), anyString());
	}

	/**
	 * A full session must waitlist rather than reject, and must not touch {@code enrolledCount} —
	 * incrementing it here is what would let the session oversell on the next cancellation.
	 */
	@Test
	void waitlistsTraineeWhenSessionIsFull() {
		TrainingSession session = givenUpcomingSession(10, 10);
		givenActiveUser();
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.empty());

		service.register(SESSION_ID, USER_ID);

		assertThat(captureSaved().getStatus()).isEqualTo(TrainingRegistrationStatus.Waitlist);
		assertThat(session.getEnrolledCount()).isEqualTo(10);
		verify(auditLogService).record(
				"REGISTER_TRAINING_SESSION:Waitlist", "training_session", SESSION_ID);
	}

	@Test
	void reactivatesCancelledRegistrationAndClearsCancellationTimestamp() {
		TrainingSession session = givenUpcomingSession(30, 4);
		givenActiveUser();
		TrainingRegistration cancelled = registration(TrainingRegistrationStatus.Cancelled);
		cancelled.setCancelledAt(LocalDateTime.now().minusDays(2));
		cancelled.setCompletedAt(LocalDateTime.now().minusDays(2));
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.of(cancelled));

		service.register(SESSION_ID, USER_ID);

		assertThat(cancelled.getStatus()).isEqualTo(TrainingRegistrationStatus.Registered);
		assertThat(cancelled.getCancelledAt()).isNull();
		assertThat(cancelled.getCompletedAt()).isNull();
		assertThat(session.getEnrolledCount()).isEqualTo(5);
	}

	@ParameterizedTest(name = "cannot re-register while {0}")
	@EnumSource(value = TrainingRegistrationStatus.class,
			names = {"Registered", "Waitlist", "Completed"})
	void rejectsDuplicateRegistration(TrainingRegistrationStatus status) {
		TrainingSession session = givenUpcomingSession(30, 4);
		givenActiveUser();
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.of(registration(status)));

		assertThatThrownBy(() -> service.register(SESSION_ID, USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("TRAINING_REGISTRATION_EXISTS");

		assertThat(session.getEnrolledCount()).isEqualTo(4);
		verify(trainingRegistrationRepository, never()).save(any());
		verifyNoInteractions(notificationService);
	}

	@ParameterizedTest(name = "cannot register for a {0} session")
	@EnumSource(value = TrainingSessionStatus.class, names = {"Completed", "Canceled"})
	void rejectsRegistrationForSessionThatIsNotUpcoming(TrainingSessionStatus status) {
		TrainingSession session = new TrainingSession();
		session.setId(SESSION_ID);
		session.setStatus(status);
		session.setCapacity(30);
		session.setEnrolledCount(1);
		when(trainingSessionRepository.findWithClassAndTrainerByIdForUpdate(SESSION_ID))
				.thenReturn(Optional.of(session));

		assertThatThrownBy(() -> service.register(SESSION_ID, USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("TRAINING_SESSION_NOT_OPEN_FOR_REGISTRATION");

		assertThat(session.getEnrolledCount()).isEqualTo(1);
	}

	@ParameterizedTest(name = "{0} user cannot register")
	@EnumSource(value = UserStatus.class, names = {"Active"}, mode = EnumSource.Mode.EXCLUDE)
	void rejectsRegistrationForUserThatIsNotActive(UserStatus status) {
		TrainingSession session = givenUpcomingSession(30, 2);
		User user = new User();
		user.setId(USER_ID);
		user.setStatus(status);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> service.register(SESSION_ID, USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("USER_NOT_ACTIVE");

		assertThat(session.getEnrolledCount()).isEqualTo(2);
	}

	@Test
	void rejectsRegistrationForUnknownSession() {
		when(trainingSessionRepository.findWithClassAndTrainerByIdForUpdate(SESSION_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.register(SESSION_ID, USER_ID))
				.isInstanceOf(NotFoundException.class);
	}

	/**
	 * The seat a cancelling trainee gives up must go straight to the head of the waitlist, and the
	 * count must land back where it started rather than double-counting the promotion.
	 */
	@Test
	void cancellingRegisteredSeatPromotesFirstWaitlistedTrainee() {
		TrainingSession session = givenUpcomingSession(10, 10);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.of(registration(TrainingRegistrationStatus.Registered)));
		TrainingRegistration waitlisted = registration(TrainingRegistrationStatus.Waitlist);
		waitlisted.getUser().setId(777L);
		when(trainingRegistrationRepository.findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(
				SESSION_ID, TrainingRegistrationStatus.Waitlist))
				.thenReturn(Optional.of(waitlisted));

		service.cancelSelf(SESSION_ID, USER_ID);

		assertThat(waitlisted.getStatus()).isEqualTo(TrainingRegistrationStatus.Registered);
		assertThat(session.getEnrolledCount()).isEqualTo(10);
		verify(notificationService).create(eq(777L), anyString(), anyString());
		verify(auditLogService).record("CANCEL_TRAINING_REGISTRATION", "training_session", SESSION_ID);
	}

	@Test
	void cancellingRegisteredSeatFreesCapacityWhenWaitlistIsEmpty() {
		TrainingSession session = givenUpcomingSession(10, 10);
		TrainingRegistration registered = registration(TrainingRegistrationStatus.Registered);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.of(registered));
		when(trainingRegistrationRepository.findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(
				SESSION_ID, TrainingRegistrationStatus.Waitlist))
				.thenReturn(Optional.empty());

		service.cancelSelf(SESSION_ID, USER_ID);

		assertThat(registered.getStatus()).isEqualTo(TrainingRegistrationStatus.Cancelled);
		assertThat(registered.getCancelledAt()).isNotNull();
		assertThat(session.getEnrolledCount()).isEqualTo(9);
	}

	/**
	 * A waitlisted trainee holds no seat, so cancelling must not decrement the count and must not
	 * trigger a promotion — doing either would hand out a seat that was never occupied.
	 */
	@Test
	void cancellingWaitlistEntryLeavesCapacityUntouched() {
		TrainingSession session = givenUpcomingSession(10, 10);
		TrainingRegistration waitlisted = registration(TrainingRegistrationStatus.Waitlist);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.of(waitlisted));

		service.cancelSelf(SESSION_ID, USER_ID);

		assertThat(waitlisted.getStatus()).isEqualTo(TrainingRegistrationStatus.Cancelled);
		assertThat(session.getEnrolledCount()).isEqualTo(10);
		verify(trainingRegistrationRepository, never())
				.findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(anyLong(), any());
	}

	@ParameterizedTest(name = "cannot cancel a {0} registration")
	@EnumSource(value = TrainingRegistrationStatus.class, names = {"Cancelled", "Completed"})
	void rejectsCancellationOfRegistrationThatHoldsNothing(TrainingRegistrationStatus status) {
		givenUpcomingSession(10, 4);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.of(registration(status)));

		assertThatThrownBy(() -> service.cancelSelf(SESSION_ID, USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("TRAINING_REGISTRATION_NOT_CANCELABLE");

		verifyNoInteractions(notificationService);
	}

	@Test
	void rejectsCancellationWhenNoRegistrationExists() {
		givenUpcomingSession(10, 4);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.cancelSelf(SESSION_ID, USER_ID))
				.isInstanceOf(NotFoundException.class);
	}

	private TrainingSession givenUpcomingSession(int capacity, int enrolledCount) {
		TrainingSession session = new TrainingSession();
		session.setId(SESSION_ID);
		session.setTitle("Kubernetes basics");
		session.setStatus(TrainingSessionStatus.Upcoming);
		session.setCapacity(capacity);
		session.setEnrolledCount(enrolledCount);
		when(trainingSessionRepository.findWithClassAndTrainerByIdForUpdate(SESSION_ID))
				.thenReturn(Optional.of(session));
		return session;
	}

	private void givenActiveUser() {
		User user = new User();
		user.setId(USER_ID);
		user.setFullName("Trainee");
		user.setStatus(UserStatus.Active);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
	}

	private TrainingRegistration registration(TrainingRegistrationStatus status) {
		User user = new User();
		user.setId(USER_ID);
		user.setFullName("Trainee");
		TrainingRegistration registration = new TrainingRegistration();
		registration.setId(400L);
		registration.setUser(user);
		registration.setStatus(status);
		registration.setRegisteredAt(LocalDateTime.now().minusDays(1));
		return registration;
	}

	private TrainingRegistration captureSaved() {
		org.mockito.ArgumentCaptor<TrainingRegistration> captor =
				org.mockito.ArgumentCaptor.forClass(TrainingRegistration.class);
		verify(trainingRegistrationRepository).save(captor.capture());
		return captor.getValue();
	}
}
