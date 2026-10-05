package com.fap.training.service;

import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.repository.ClassEnrollmentRepository;
import com.fap.common.exception.ConflictException;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingRegistrationMode;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keeps session registrations in step with the class roster: auto-enroll sessions follow the
 * class's enrolled trainees, and a trainee who leaves the class loses their upcoming sessions.
 *
 * <p>Kept apart from {@link TrainingRegistrationService}, which serves a trainee's own requests.
 * The roster path registers without a per-trainee capacity check (an auto-enroll session must
 * cover the class capacity up front) and records no registration metrics. A seat it frees is
 * offered to the waitlist through the same {@link WaitlistPromoter} as a trainee's own
 * cancellation, so the promoted trainee sees one behaviour whichever path released the seat.
 */
@Service
public class ClassRosterRegistrationService {

	private static final Collection<TrainingRegistrationStatus> ACTIVE_SESSION_REGISTRATION_STATUSES = List.of(
			TrainingRegistrationStatus.Registered,
			TrainingRegistrationStatus.Waitlist);

	private final TrainingSessionRepository trainingSessionRepository;
	private final TrainingRegistrationRepository trainingRegistrationRepository;
	private final ClassEnrollmentRepository classEnrollmentRepository;
	private final WaitlistPromoter waitlistPromoter;
	private final Clock clock;

	public ClassRosterRegistrationService(
			TrainingSessionRepository trainingSessionRepository,
			TrainingRegistrationRepository trainingRegistrationRepository,
			ClassEnrollmentRepository classEnrollmentRepository,
			WaitlistPromoter waitlistPromoter,
			Clock clock) {
		this.trainingSessionRepository = trainingSessionRepository;
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.classEnrollmentRepository = classEnrollmentRepository;
		this.waitlistPromoter = waitlistPromoter;
		this.clock = clock;
	}

	/**
	 * Registers every enrolled trainee of the class when {@code session} is auto-enroll.
	 *
	 * @throws ConflictException when the session has fewer seats than the class
	 */
	@Transactional
	public void syncAutoEnrollSession(TrainingSession session) {
		if (session.getRegistrationMode() != TrainingRegistrationMode.AutoEnroll) {
			return;
		}
		if (session.getCapacity() < session.getFapClass().getCapacity()) {
			throw new ConflictException("AUTO_ENROLL_SESSION_CAPACITY_TOO_SMALL", "Auto-enroll session capacity must cover the class capacity");
		}
		Map<Long, TrainingRegistration> registrationsByUser = trainingRegistrationRepository
				.findByTrainingSessionId(session.getId()).stream()
				.collect(Collectors.toMap(registration -> registration.getUser().getId(), Function.identity()));
		LocalDateTime now = LocalDateTime.now(clock);
		List<TrainingRegistration> changed = classEnrollmentRepository
				.findByFapClassIdAndStatusOrderByCreatedAtAscIdAsc(session.getFapClass().getId(), ClassEnrollmentStatus.Enrolled)
				.stream()
				.map(enrollment -> syncRegistration(
						session,
						enrollment.getUser(),
						registrationsByUser.get(enrollment.getUser().getId()),
						now))
				.filter(Objects::nonNull)
				.toList();
		trainingRegistrationRepository.saveAll(changed);
	}

	/** Registers a newly enrolled trainee for the class's upcoming auto-enroll sessions. */
	@Transactional
	public void registerInAutoEnrollSessions(Long classId, User user) {
		List<TrainingSession> sessions = trainingSessionRepository
				.findByFapClassIdAndRegistrationModeAndStatusOrderBySessionDateAscStartTimeAsc(
						classId,
						TrainingRegistrationMode.AutoEnroll,
						TrainingSessionStatus.Upcoming);
		if (sessions.isEmpty()) {
			return;
		}
		Map<Long, TrainingRegistration> registrationsBySession = trainingRegistrationRepository
				.findByUserIdAndTrainingSessionIdIn(user.getId(), sessions.stream().map(TrainingSession::getId).toList())
				.stream()
				.collect(Collectors.toMap(registration -> registration.getTrainingSession().getId(), Function.identity()));
		LocalDateTime now = LocalDateTime.now(clock);
		List<TrainingRegistration> changed = sessions.stream()
				.map(session -> syncRegistration(session, user, registrationsBySession.get(session.getId()), now))
				.filter(Objects::nonNull)
				.toList();
		trainingRegistrationRepository.saveAll(changed);
	}

	/**
	 * Cancels the trainee's registrations for the class's upcoming sessions after they leave it,
	 * freeing their seats for the waitlist of self-enroll sessions.
	 *
	 * @param cancelledAt the caller's withdrawal time, so the registrations match the enrollment
	 */
	@Transactional
	public void cancelFutureSessionRegistrations(Long classId, Long userId, LocalDateTime cancelledAt) {
		trainingRegistrationRepository.findFutureByClassAndUser(
				classId,
				userId,
				TrainingSessionStatus.Upcoming,
				ACTIVE_SESSION_REGISTRATION_STATUSES)
				.forEach(registration -> {
					TrainingSession session = registration.getTrainingSession();
					if (registration.getStatus() == TrainingRegistrationStatus.Registered) {
						session.setEnrolledCount(Math.max(0, session.getEnrolledCount() - 1));
					}
					registration.setStatus(TrainingRegistrationStatus.Cancelled);
					registration.setCancelledAt(cancelledAt);
					if (session.getRegistrationMode() == TrainingRegistrationMode.SelfEnroll) {
						waitlistPromoter.promoteFirstWaitlisted(session);
					}
				});
	}

	/**
	 * Registers {@code user} for {@code session} unless already registered or completed.
	 *
	 * @param existing the user's current registration for the session, prefetched by the caller
	 * @return the registration to save, or {@code null} when nothing changed
	 */
	private TrainingRegistration syncRegistration(
			TrainingSession session,
			User user,
			TrainingRegistration existing,
			LocalDateTime now) {
		TrainingRegistration registration = existing;
		if (registration == null) {
			registration = new TrainingRegistration();
			registration.setTrainingSession(session);
			registration.setUser(user);
		}
		if (registration.getStatus() == TrainingRegistrationStatus.Registered
				|| registration.getStatus() == TrainingRegistrationStatus.Completed) {
			return null;
		}
		registration.setStatus(TrainingRegistrationStatus.Registered);
		registration.setRegisteredAt(now);
		registration.setCancelledAt(null);
		registration.setCompletedAt(null);
		session.setEnrolledCount(session.getEnrolledCount() + 1);
		return registration;
	}
}
