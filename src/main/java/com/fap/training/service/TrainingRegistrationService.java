package com.fap.training.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.metrics.DomainMetrics;
import com.fap.notification.service.NotificationService;
import com.fap.training.dto.TrainingParticipantsResponse;
import com.fap.training.dto.TrainingRegistrationResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrainingRegistrationService {

	private final TrainingSessionRepository trainingSessionRepository;
	private final TrainingRegistrationRepository trainingRegistrationRepository;
	private final UserRepository userRepository;
	private final TrainingRegistrationMapper trainingRegistrationMapper;
	private final AuditLogService auditLogService;
	private final NotificationService notificationService;
	private final DomainMetrics domainMetrics;

	public TrainingRegistrationService(
			TrainingSessionRepository trainingSessionRepository,
			TrainingRegistrationRepository trainingRegistrationRepository,
			UserRepository userRepository,
			TrainingRegistrationMapper trainingRegistrationMapper,
			AuditLogService auditLogService,
			NotificationService notificationService,
			DomainMetrics domainMetrics) {
		this.trainingSessionRepository = trainingSessionRepository;
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.userRepository = userRepository;
		this.trainingRegistrationMapper = trainingRegistrationMapper;
		this.auditLogService = auditLogService;
		this.notificationService = notificationService;
		this.domainMetrics = domainMetrics;
	}

	@Transactional
	public TrainingRegistrationResponse register(Long trainingSessionId, Long currentUserId) {
		TrainingSession session = findUpcomingSessionForUpdate(trainingSessionId);
		User user = findActiveUser(currentUserId);
		LocalDateTime now = LocalDateTime.now();
		TrainingRegistration registration = trainingRegistrationRepository
				.findByTrainingSessionIdAndUserId(trainingSessionId, currentUserId)
				.map(existing -> reactivateRegistration(existing, session, now))
				.orElseGet(() -> createRegistration(session, user, now));
		TrainingRegistration saved = trainingRegistrationRepository.save(registration);
		auditLogService.record("REGISTER_TRAINING_SESSION:" + saved.getStatus().name(), "training_session", trainingSessionId);
		notificationService.create(
				currentUserId,
				"Training session registration",
				"Your registration for " + session.getTitle() + " is " + saved.getStatus().name());
		return trainingRegistrationMapper.toResponse(saved);
	}

	@Transactional
	public TrainingRegistrationResponse cancelSelf(Long trainingSessionId, Long currentUserId) {
		TrainingSession session = findUpcomingSessionForUpdate(trainingSessionId);
		TrainingRegistration registration = trainingRegistrationRepository
				.findByTrainingSessionIdAndUserId(trainingSessionId, currentUserId)
				.orElseThrow(() -> new NotFoundException("Training registration not found"));
		if (registration.getStatus() == TrainingRegistrationStatus.Registered) {
			registration.setStatus(TrainingRegistrationStatus.Cancelled);
			registration.setCancelledAt(LocalDateTime.now());
			session.setEnrolledCount(Math.max(0, session.getEnrolledCount() - 1));
			promoteFirstWaitlisted(session);
		}
		else if (registration.getStatus() == TrainingRegistrationStatus.Waitlist) {
			registration.setStatus(TrainingRegistrationStatus.Cancelled);
			registration.setCancelledAt(LocalDateTime.now());
		}
		else {
			throw new ConflictException("TRAINING_REGISTRATION_NOT_CANCELABLE", "Only registered or waitlisted registration can be cancelled");
		}
		auditLogService.record("CANCEL_TRAINING_REGISTRATION", "training_session", trainingSessionId);
		notificationService.create(
				currentUserId,
				"Training registration cancelled",
				"Your registration for " + session.getTitle() + " has been cancelled");
		return trainingRegistrationMapper.toResponse(registration);
	}

	@Transactional(readOnly = true)
	public TrainingParticipantsResponse participants(Long trainingSessionId) {
		TrainingSession session = trainingSessionRepository.findWithClassAndTrainerById(trainingSessionId)
				.orElseThrow(() -> new NotFoundException("Training session not found"));
		List<TrainingRegistration> registrations = trainingRegistrationRepository
				.findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
						trainingSessionId,
						List.of(TrainingRegistrationStatus.Registered, TrainingRegistrationStatus.Waitlist));
		List<TrainingRegistrationResponse> registered = registrations.stream()
				.filter(registration -> registration.getStatus() == TrainingRegistrationStatus.Registered)
				.map(trainingRegistrationMapper::toResponse)
				.toList();
		List<TrainingRegistrationResponse> waitlist = registrations.stream()
				.filter(registration -> registration.getStatus() == TrainingRegistrationStatus.Waitlist)
				.map(trainingRegistrationMapper::toResponse)
				.toList();
		return new TrainingParticipantsResponse(
				session.getId(),
				session.getCapacity(),
				session.getEnrolledCount(),
				registered,
				waitlist);
	}

	private TrainingRegistration createRegistration(TrainingSession session, User user, LocalDateTime now) {
		TrainingRegistration registration = new TrainingRegistration();
		registration.setTrainingSession(session);
		registration.setUser(user);
		registration.setRegisteredAt(now);
		assignRegistrationStatus(registration, session);
		return registration;
	}

	private TrainingRegistration reactivateRegistration(TrainingRegistration registration, TrainingSession session, LocalDateTime now) {
		if (registration.getStatus() != TrainingRegistrationStatus.Cancelled) {
			domainMetrics.recordRegistrationOutcome(DomainMetrics.RegistrationOutcome.CONFLICT);
			throw new ConflictException("TRAINING_REGISTRATION_EXISTS", "User already registered for this training session");
		}
		registration.setRegisteredAt(now);
		registration.setCancelledAt(null);
		registration.setCompletedAt(null);
		assignRegistrationStatus(registration, session);
		return registration;
	}

	private void assignRegistrationStatus(TrainingRegistration registration, TrainingSession session) {
		if (session.getEnrolledCount() < session.getCapacity()) {
			registration.setStatus(TrainingRegistrationStatus.Registered);
			session.setEnrolledCount(session.getEnrolledCount() + 1);
			domainMetrics.recordRegistrationOutcome(DomainMetrics.RegistrationOutcome.REGISTERED);
		}
		else {
			registration.setStatus(TrainingRegistrationStatus.Waitlist);
			domainMetrics.recordRegistrationOutcome(DomainMetrics.RegistrationOutcome.WAITLISTED);
		}
	}

	private void promoteFirstWaitlisted(TrainingSession session) {
		if (session.getEnrolledCount() >= session.getCapacity()) {
			return;
		}
		trainingRegistrationRepository
				.findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(
						session.getId(),
						TrainingRegistrationStatus.Waitlist)
				.ifPresent(waitlisted -> {
					waitlisted.setStatus(TrainingRegistrationStatus.Registered);
					waitlisted.setCancelledAt(null);
					waitlisted.setCompletedAt(null);
					session.setEnrolledCount(session.getEnrolledCount() + 1);
					domainMetrics.recordRegistrationOutcome(DomainMetrics.RegistrationOutcome.PROMOTED);
					notificationService.create(
							waitlisted.getUser().getId(),
							"Waitlist promoted",
							"You have been moved from waitlist to registered for " + session.getTitle());
				});
	}

	private TrainingSession findUpcomingSessionForUpdate(Long trainingSessionId) {
		TrainingSession session = trainingSessionRepository.findWithClassAndTrainerByIdForUpdate(trainingSessionId)
				.orElseThrow(() -> new NotFoundException("Training session not found"));
		if (session.getStatus() != TrainingSessionStatus.Upcoming) {
			throw new ConflictException("TRAINING_SESSION_NOT_OPEN_FOR_REGISTRATION", "Registration is allowed only for upcoming training sessions");
		}
		return session;
	}

	private User findActiveUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException("User not found"));
		if (user.getStatus() != UserStatus.Active) {
			throw new ConflictException("USER_NOT_ACTIVE", "Only active user can register for training session");
		}
		return user;
	}
}
