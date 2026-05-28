package com.fap.training.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.training.dto.CreateTrainingFeedbackRequest;
import com.fap.training.dto.TrainingFeedbackResponse;
import com.fap.training.dto.TrainingFeedbackSummaryResponse;
import com.fap.training.entity.TrainingFeedback;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.mapper.TrainingFeedbackMapper;
import com.fap.training.repository.TrainingFeedbackRepository;
import com.fap.training.repository.TrainingRegistrationRepository;
import com.fap.training.repository.TrainingSessionRepository;
import com.fap.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TrainingFeedbackService {

	private final TrainingFeedbackRepository trainingFeedbackRepository;
	private final TrainingSessionRepository trainingSessionRepository;
	private final TrainingRegistrationRepository trainingRegistrationRepository;
	private final TrainingFeedbackMapper trainingFeedbackMapper;
	private final AuditLogService auditLogService;

	public TrainingFeedbackService(
			TrainingFeedbackRepository trainingFeedbackRepository,
			TrainingSessionRepository trainingSessionRepository,
			TrainingRegistrationRepository trainingRegistrationRepository,
			TrainingFeedbackMapper trainingFeedbackMapper,
			AuditLogService auditLogService) {
		this.trainingFeedbackRepository = trainingFeedbackRepository;
		this.trainingSessionRepository = trainingSessionRepository;
		this.trainingRegistrationRepository = trainingRegistrationRepository;
		this.trainingFeedbackMapper = trainingFeedbackMapper;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public TrainingFeedbackResponse submit(Long trainingSessionId, Long currentUserId, CreateTrainingFeedbackRequest request) {
		TrainingSession session = trainingSessionRepository.findWithClassAndTrainerById(trainingSessionId)
				.orElseThrow(() -> new NotFoundException("Training session not found"));
		if (session.getStatus() != TrainingSessionStatus.Completed) {
			throw new ConflictException("FEEDBACK_SESSION_NOT_COMPLETED", "Feedback is allowed only for completed training sessions");
		}
		if (trainingFeedbackRepository.existsByTrainingSessionIdAndUserId(trainingSessionId, currentUserId)) {
			throw new ConflictException("FEEDBACK_ALREADY_SUBMITTED", "Feedback has already been submitted for this training session");
		}
		TrainingRegistration registration = trainingRegistrationRepository
				.findByTrainingSessionIdAndUserId(trainingSessionId, currentUserId)
				.orElseThrow(() -> new ConflictException("FEEDBACK_REGISTRATION_REQUIRED", "Only registered participants can submit feedback"));
		if (registration.getStatus() != TrainingRegistrationStatus.Registered
				&& registration.getStatus() != TrainingRegistrationStatus.Completed) {
			throw new ConflictException("FEEDBACK_REGISTRATION_NOT_ELIGIBLE", "Only registered or completed participants can submit feedback");
		}

		LocalDateTime now = LocalDateTime.now();
		User user = registration.getUser();
		TrainingFeedback feedback = new TrainingFeedback();
		feedback.setTrainingSession(session);
		feedback.setUser(user);
		feedback.setRatingContent(request.ratingContent());
		feedback.setRatingTrainer(request.ratingTrainer());
		feedback.setRatingOrganization(request.ratingOrganization());
		feedback.setComment(normalize(request.comment()));
		feedback.setCreatedAt(now);
		feedback.setUpdatedAt(now);
		TrainingFeedback saved = trainingFeedbackRepository.save(feedback);
		auditLogService.record("SUBMIT_TRAINING_FEEDBACK", "training_session", trainingSessionId);
		return trainingFeedbackMapper.toResponse(saved);
	}

	@Transactional(readOnly = true)
	public TrainingFeedbackSummaryResponse summary(Long trainingSessionId) {
		if (!trainingSessionRepository.existsById(trainingSessionId)) {
			throw new NotFoundException("Training session not found");
		}
		TrainingFeedbackRepository.FeedbackSummary summary = trainingFeedbackRepository
				.summarizeByTrainingSessionId(trainingSessionId);
		long feedbackCount = summary.getFeedbackCount() == null ? 0 : summary.getFeedbackCount();
		Double averageContentRating = averageOrZero(summary.getAverageContentRating());
		Double averageTrainerRating = averageOrZero(summary.getAverageTrainerRating());
		Double averageOrganizationRating = averageOrZero(summary.getAverageOrganizationRating());
		Double overallAverageRating = feedbackCount == 0
				? 0.0
				: (averageContentRating + averageTrainerRating + averageOrganizationRating) / 3;
		return new TrainingFeedbackSummaryResponse(
				trainingSessionId,
				feedbackCount,
				averageContentRating,
				averageTrainerRating,
				averageOrganizationRating,
				overallAverageRating);
	}

	@Transactional(readOnly = true)
	public Page<TrainingFeedbackResponse> listMine(Long currentUserId, int page, int limit) {
		return trainingFeedbackRepository
				.findByUserIdOrderByCreatedAtDesc(currentUserId, PageRequest.of(page, limit))
				.map(trainingFeedbackMapper::toResponse);
	}

	private Double averageOrZero(Double value) {
		return value == null ? 0.0 : value;
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
