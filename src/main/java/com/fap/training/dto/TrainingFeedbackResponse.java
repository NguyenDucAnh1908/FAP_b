package com.fap.training.dto;

import java.time.LocalDateTime;

public record TrainingFeedbackResponse(
		Long id,
		Long trainingSessionId,
		String trainingSessionTitle,
		Long userId,
		String userFullName,
		String userEmail,
		Integer ratingContent,
		Integer ratingTrainer,
		Integer ratingOrganization,
		String comment,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
