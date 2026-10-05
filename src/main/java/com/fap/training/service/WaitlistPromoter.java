package com.fap.training.service;

import com.fap.common.metrics.DomainMetrics;
import com.fap.notification.service.NotificationService;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.repository.TrainingRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves the longest-waiting registration of a session into a seat that just came free.
 *
 * <p>Both ways a seat frees up, a trainee cancelling their own registration and a trainee leaving
 * the class, go through here so the promoted trainee gets the same status, timestamps, metric and
 * notification whichever path released the seat. The two services used to carry their own copies
 * of this rule and they drifted apart.
 */
@Component
public class WaitlistPromoter {

	private final TrainingRegistrationRepository trainingRegistrationRepository;
	private final NotificationService notificationService;
	private final DomainMetrics domainMetrics;

	public WaitlistPromoter(
			TrainingRegistrationRepository trainingRegistrationRepository,
			NotificationService notificationService,
			DomainMetrics domainMetrics) {
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.notificationService = notificationService;
		this.domainMetrics = domainMetrics;
	}

	/**
	 * Promotes the first waitlisted registration when {@code session} has a free seat; a no-op
	 * otherwise. The caller must already have released the seat (and hold whatever row lock it
	 * needs), because the capacity guard reads {@code enrolledCount} as it stands.
	 */
	@Transactional
	public void promoteFirstWaitlisted(TrainingSession session) {
		if (session.getEnrolledCount() >= session.getCapacity()) {
			return;
		}
		trainingRegistrationRepository
				.findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(
						session.getId(),
						TrainingRegistrationStatus.Waitlist)
				.ifPresent(waitlisted -> {
					waitlisted.setStatus(TrainingRegistrationStatus.Registered);
					// A waitlisted row can still carry timestamps from an earlier cancellation or
					// completion; a seat granted just now must not read as cancelled or completed.
					waitlisted.setCancelledAt(null);
					waitlisted.setCompletedAt(null);
					session.setEnrolledCount(session.getEnrolledCount() + 1);
					domainMetrics.recordRegistrationOutcome(DomainMetrics.RegistrationOutcome.PROMOTED);
					notificationService.create(
							waitlisted.getUser().getId(),
							"notification.training_registration.promoted.title",
							"notification.training_registration.promoted.message",
							session.getTitle());
				});
	}
}
