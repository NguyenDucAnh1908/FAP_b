package com.fap.training.service;

import com.fap.clazz.entity.ClassEnrollment;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.repository.ClassEnrollmentRepository;
import com.fap.common.exception.ConflictException;
import com.fap.common.metrics.DomainMetrics;
import com.fap.notification.service.NotificationService;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingRegistrationMode;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Session registrations that follow the class roster. Waitlist promotion itself belongs to
 * {@link WaitlistPromoter} and is pinned in {@link WaitlistPromoterTest}; the roster-side tests
 * run a real promoter over the same mocks to show that a freed seat is offered to the waitlist
 * only for self-enroll sessions, and only after the seat has actually been released.
 */
class ClassRosterRegistrationServiceTest {

	private static final Long CLASS_ID = 70L;
	private static final Long USER_ID = 700L;
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-15T09:00:00Z"), ZoneOffset.UTC);
	private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
	private static final LocalDateTime EARLIER = NOW.minusDays(3);

	private final TrainingSessionRepository trainingSessionRepository = mock(TrainingSessionRepository.class);
	private final TrainingRegistrationRepository trainingRegistrationRepository = mock(TrainingRegistrationRepository.class);
	private final ClassEnrollmentRepository classEnrollmentRepository = mock(ClassEnrollmentRepository.class);
	private final NotificationService notificationService = mock(NotificationService.class);
	private final DomainMetrics domainMetrics = mock(DomainMetrics.class);
	private final ClassRosterRegistrationService service = new ClassRosterRegistrationService(
			trainingSessionRepository,
			trainingRegistrationRepository,
			classEnrollmentRepository,
			new WaitlistPromoter(trainingRegistrationRepository, notificationService, domainMetrics),
			CLOCK);

	@Test
	void selfEnrollSessionIsNotSyncedWithTheRoster() {
		TrainingSession session = session(80L, TrainingRegistrationMode.SelfEnroll, 30, 0);

		service.syncAutoEnrollSession(session);

		verifyNoInteractions(trainingRegistrationRepository, classEnrollmentRepository);
	}

	@Test
	void autoEnrollSessionMustCoverTheClassCapacity() {
		TrainingSession session = session(80L, TrainingRegistrationMode.AutoEnroll, 19, 0);
		session.getFapClass().setCapacity(20);

		assertThatThrownBy(() -> service.syncAutoEnrollSession(session))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("AUTO_ENROLL_SESSION_CAPACITY_TOO_SMALL");

		verifyNoInteractions(trainingRegistrationRepository, classEnrollmentRepository);
	}

	@Test
	void autoEnrollSessionRegistersEnrolledTraineesWhoAreNotRegisteredYet() {
		TrainingSession session = session(80L, TrainingRegistrationMode.AutoEnroll, 5, 1);
		User unregistered = user(701L);
		User cancelled = user(702L);
		User registered = user(703L);
		User completed = user(704L);
		TrainingRegistration cancelledRegistration = registration(session, cancelled, TrainingRegistrationStatus.Cancelled);
		cancelledRegistration.setCancelledAt(EARLIER);
		cancelledRegistration.setCompletedAt(EARLIER);
		// Registrations are prefetched once for the session, not looked up per trainee.
		when(trainingRegistrationRepository.findByTrainingSessionId(80L)).thenReturn(List.of(
				cancelledRegistration,
				registration(session, registered, TrainingRegistrationStatus.Registered),
				registration(session, completed, TrainingRegistrationStatus.Completed)));
		when(classEnrollmentRepository.findByFapClassIdAndStatusOrderByCreatedAtAscIdAsc(CLASS_ID, ClassEnrollmentStatus.Enrolled))
				.thenReturn(List.of(enrolled(unregistered), enrolled(cancelled), enrolled(registered), enrolled(completed)));

		service.syncAutoEnrollSession(session);

		List<TrainingRegistration> saved = savedRegistrations();
		assertThat(saved).extracting(TrainingRegistration::getUser).containsExactly(unregistered, cancelled);
		assertThat(saved.get(1)).isSameAs(cancelledRegistration);
		assertThat(saved).allSatisfy(registration -> assertFreshlyRegistered(registration, session));
		assertThat(session.getEnrolledCount()).isEqualTo(3);
	}

	@Test
	void newlyEnrolledTraineeIsRegisteredForUpcomingAutoEnrollSessions() {
		User user = user(USER_ID);
		TrainingSession unregistered = session(81L, TrainingRegistrationMode.AutoEnroll, 30, 4);
		TrainingSession alreadyRegistered = session(82L, TrainingRegistrationMode.AutoEnroll, 30, 4);
		TrainingSession previouslyCancelled = session(83L, TrainingRegistrationMode.AutoEnroll, 30, 4);
		TrainingRegistration cancelledRegistration = registration(previouslyCancelled, user, TrainingRegistrationStatus.Cancelled);
		cancelledRegistration.setCancelledAt(EARLIER);
		when(trainingSessionRepository.findByFapClassIdAndRegistrationModeAndStatusOrderBySessionDateAscStartTimeAsc(
				CLASS_ID, TrainingRegistrationMode.AutoEnroll, TrainingSessionStatus.Upcoming))
				.thenReturn(List.of(unregistered, alreadyRegistered, previouslyCancelled));
		// One lookup for the trainee across all sessions, not one per session.
		when(trainingRegistrationRepository.findByUserIdAndTrainingSessionIdIn(USER_ID, List.of(81L, 82L, 83L)))
				.thenReturn(List.of(
						registration(alreadyRegistered, user, TrainingRegistrationStatus.Registered),
						cancelledRegistration));

		service.registerInAutoEnrollSessions(CLASS_ID, user);

		List<TrainingRegistration> saved = savedRegistrations();
		assertThat(saved).extracting(TrainingRegistration::getTrainingSession)
				.containsExactly(unregistered, previouslyCancelled);
		assertThat(saved.get(1)).isSameAs(cancelledRegistration);
		assertThat(saved).allSatisfy(registration -> {
			assertThat(registration.getUser()).isSameAs(user);
			assertFreshlyRegistered(registration, registration.getTrainingSession());
		});
		assertThat(unregistered.getEnrolledCount()).isEqualTo(5);
		assertThat(alreadyRegistered.getEnrolledCount()).isEqualTo(4);
		assertThat(previouslyCancelled.getEnrolledCount()).isEqualTo(5);
	}

	@Test
	void classWithoutUpcomingAutoEnrollSessionsNeedsNoRegistrationLookup() {
		when(trainingSessionRepository.findByFapClassIdAndRegistrationModeAndStatusOrderBySessionDateAscStartTimeAsc(
				CLASS_ID, TrainingRegistrationMode.AutoEnroll, TrainingSessionStatus.Upcoming))
				.thenReturn(List.of());

		service.registerInAutoEnrollSessions(CLASS_ID, user(USER_ID));

		verifyNoInteractions(trainingRegistrationRepository);
	}

	@Test
	void leavingTheClassCancelsUpcomingRegistrationsAndReleasesOnlyHeldSeats() {
		User user = user(USER_ID);
		TrainingSession autoSession = session(81L, TrainingRegistrationMode.AutoEnroll, 30, 3);
		TrainingSession emptySession = session(82L, TrainingRegistrationMode.AutoEnroll, 30, 0);
		TrainingSession fullSelfSession = session(83L, TrainingRegistrationMode.SelfEnroll, 5, 5);
		TrainingRegistration registered = registration(autoSession, user, TrainingRegistrationStatus.Registered);
		TrainingRegistration registeredWithStaleCount = registration(emptySession, user, TrainingRegistrationStatus.Registered);
		TrainingRegistration waitlisted = registration(fullSelfSession, user, TrainingRegistrationStatus.Waitlist);
		givenFutureRegistrations(registered, registeredWithStaleCount, waitlisted);

		service.cancelFutureSessionRegistrations(CLASS_ID, USER_ID, NOW);

		assertThat(List.of(registered, registeredWithStaleCount, waitlisted)).allSatisfy(registration -> {
			assertThat(registration.getStatus()).isEqualTo(TrainingRegistrationStatus.Cancelled);
			assertThat(registration.getCancelledAt()).isEqualTo(NOW);
		});
		assertThat(autoSession.getEnrolledCount()).isEqualTo(2);
		assertThat(emptySession.getEnrolledCount()).isZero();
		// A waitlisted trainee held no seat, so the full session stays full and nobody is promoted.
		assertThat(fullSelfSession.getEnrolledCount()).isEqualTo(5);
		verify(trainingRegistrationRepository, never())
				.findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(anyLong(), any());
		verifyNoInteractions(notificationService, domainMetrics);
	}

	@Test
	void seatReleasedInSelfEnrollSessionPromotesTheFirstWaitlistedTrainee() {
		TrainingSession session = session(83L, TrainingRegistrationMode.SelfEnroll, 2, 2);
		session.setTitle("Spring Boot");
		TrainingRegistration leaving = registration(session, user(USER_ID), TrainingRegistrationStatus.Registered);
		TrainingRegistration waitlisted = registration(session, user(800L), TrainingRegistrationStatus.Waitlist);
		waitlisted.setCancelledAt(EARLIER);
		waitlisted.setCompletedAt(EARLIER);
		givenFutureRegistrations(leaving);
		when(trainingRegistrationRepository.findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(
				83L, TrainingRegistrationStatus.Waitlist)).thenReturn(Optional.of(waitlisted));

		service.cancelFutureSessionRegistrations(CLASS_ID, USER_ID, NOW);

		assertThat(leaving.getStatus()).isEqualTo(TrainingRegistrationStatus.Cancelled);
		assertThat(waitlisted.getStatus()).isEqualTo(TrainingRegistrationStatus.Registered);
		assertThat(waitlisted.getCancelledAt()).isNull();
		assertThat(waitlisted.getCompletedAt()).isNull();
		// Back at capacity: the seat was released before the promoter's capacity guard looked at it.
		assertThat(session.getEnrolledCount()).isEqualTo(2);
		verify(domainMetrics).recordRegistrationOutcome(DomainMetrics.RegistrationOutcome.PROMOTED);
		verify(notificationService).create(
				800L,
				"notification.training_registration.promoted.title",
				"notification.training_registration.promoted.message",
				"Spring Boot");
	}

	@Test
	void autoEnrollSessionKeepsItsWaitlistWhenASeatIsReleased() {
		TrainingSession session = session(81L, TrainingRegistrationMode.AutoEnroll, 30, 10);
		givenFutureRegistrations(registration(session, user(USER_ID), TrainingRegistrationStatus.Registered));

		service.cancelFutureSessionRegistrations(CLASS_ID, USER_ID, NOW);

		assertThat(session.getEnrolledCount()).isEqualTo(9);
		verify(trainingRegistrationRepository, never())
				.findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(anyLong(), any());
		verifyNoInteractions(notificationService, domainMetrics);
	}

	private void givenFutureRegistrations(TrainingRegistration... registrations) {
		when(trainingRegistrationRepository.findFutureByClassAndUser(
				CLASS_ID,
				USER_ID,
				TrainingSessionStatus.Upcoming,
				List.of(TrainingRegistrationStatus.Registered, TrainingRegistrationStatus.Waitlist)))
				.thenReturn(List.of(registrations));
	}

	private List<TrainingRegistration> savedRegistrations() {
		ArgumentCaptor<List<TrainingRegistration>> saved = ArgumentCaptor.captor();
		verify(trainingRegistrationRepository).saveAll(saved.capture());
		return saved.getValue();
	}

	private static void assertFreshlyRegistered(TrainingRegistration registration, TrainingSession session) {
		assertThat(registration.getTrainingSession()).isSameAs(session);
		assertThat(registration.getStatus()).isEqualTo(TrainingRegistrationStatus.Registered);
		assertThat(registration.getRegisteredAt()).isEqualTo(NOW);
		assertThat(registration.getCancelledAt()).isNull();
		assertThat(registration.getCompletedAt()).isNull();
	}

	private static TrainingSession session(Long id, TrainingRegistrationMode mode, int capacity, int enrolledCount) {
		FapClass fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setCapacity(capacity);
		TrainingSession session = new TrainingSession();
		session.setId(id);
		session.setFapClass(fapClass);
		session.setRegistrationMode(mode);
		session.setCapacity(capacity);
		session.setEnrolledCount(enrolledCount);
		return session;
	}

	private static TrainingRegistration registration(TrainingSession session, User user, TrainingRegistrationStatus status) {
		TrainingRegistration registration = new TrainingRegistration();
		registration.setTrainingSession(session);
		registration.setUser(user);
		registration.setStatus(status);
		registration.setRegisteredAt(EARLIER);
		return registration;
	}

	private static ClassEnrollment enrolled(User user) {
		ClassEnrollment enrollment = new ClassEnrollment();
		enrollment.setUser(user);
		enrollment.setStatus(ClassEnrollmentStatus.Enrolled);
		return enrollment;
	}

	private static User user(Long id) {
		User user = new User();
		user.setId(id);
		return user;
	}
}
