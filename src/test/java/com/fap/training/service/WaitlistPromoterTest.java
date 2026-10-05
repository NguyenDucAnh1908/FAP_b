package com.fap.training.service;

import com.fap.common.metrics.DomainMetrics;
import com.fap.notification.service.NotificationService;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The single promotion rule behind both ways a seat frees up (a trainee's own cancellation and a
 * class withdrawal). Pinning its full effect here is what stops the two callers drifting apart
 * again: each used to carry its own copy, and they disagreed on timestamps, the metric and the
 * notification.
 */
class WaitlistPromoterTest {

	private static final Long SESSION_ID = 83L;
	private static final Long WAITLISTED_USER_ID = 800L;
	private static final LocalDateTime EARLIER = LocalDateTime.of(2026, 3, 12, 9, 0);

	private final TrainingRegistrationRepository trainingRegistrationRepository =
			mock(TrainingRegistrationRepository.class);
	private final NotificationService notificationService = mock(NotificationService.class);
	private final DomainMetrics domainMetrics = mock(DomainMetrics.class);
	private final WaitlistPromoter promoter =
			new WaitlistPromoter(trainingRegistrationRepository, notificationService, domainMetrics);

	@Test
	void fullSessionPromotesNobodyWithoutLookingAtTheWaitlist() {
		TrainingSession session = session(2, 2);

		promoter.promoteFirstWaitlisted(session);

		assertThat(session.getEnrolledCount()).isEqualTo(2);
		verify(trainingRegistrationRepository, never())
				.findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(anyLong(), any());
		verifyNoInteractions(notificationService, domainMetrics);
	}

	@Test
	void freeSeatGoesToTheLongestWaitingTrainee() {
		TrainingSession session = session(2, 1);
		TrainingRegistration waitlisted = waitlistedRegistration();
		// Stale timestamps from an earlier cancel/complete cycle must not survive the promotion.
		waitlisted.setCancelledAt(EARLIER);
		waitlisted.setCompletedAt(EARLIER);
		when(trainingRegistrationRepository.findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(
				SESSION_ID, TrainingRegistrationStatus.Waitlist)).thenReturn(Optional.of(waitlisted));

		promoter.promoteFirstWaitlisted(session);

		assertThat(waitlisted.getStatus()).isEqualTo(TrainingRegistrationStatus.Registered);
		assertThat(waitlisted.getCancelledAt()).isNull();
		assertThat(waitlisted.getCompletedAt()).isNull();
		assertThat(session.getEnrolledCount()).isEqualTo(2);
		verify(domainMetrics).recordRegistrationOutcome(DomainMetrics.RegistrationOutcome.PROMOTED);
		verify(notificationService).create(
				WAITLISTED_USER_ID,
				"notification.training_registration.promoted.title",
				"notification.training_registration.promoted.message",
				"Spring Boot");
	}

	@Test
	void freeSeatStaysFreeWhenNobodyIsWaiting() {
		TrainingSession session = session(2, 1);
		when(trainingRegistrationRepository.findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(
				SESSION_ID, TrainingRegistrationStatus.Waitlist)).thenReturn(Optional.empty());

		promoter.promoteFirstWaitlisted(session);

		assertThat(session.getEnrolledCount()).isEqualTo(1);
		verifyNoInteractions(notificationService, domainMetrics);
	}

	private static TrainingSession session(int capacity, int enrolledCount) {
		TrainingSession session = new TrainingSession();
		session.setId(SESSION_ID);
		session.setTitle("Spring Boot");
		session.setCapacity(capacity);
		session.setEnrolledCount(enrolledCount);
		return session;
	}

	private static TrainingRegistration waitlistedRegistration() {
		User user = new User();
		user.setId(WAITLISTED_USER_ID);
		TrainingRegistration registration = new TrainingRegistration();
		registration.setUser(user);
		registration.setStatus(TrainingRegistrationStatus.Waitlist);
		registration.setRegisteredAt(EARLIER.minusDays(1));
		return registration;
	}
}
