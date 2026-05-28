package com.fap.training.dto;

public record TrainingFeedbackSummaryResponse(
		Long trainingSessionId,
		long feedbackCount,
		Double averageContentRating,
		Double averageTrainerRating,
		Double averageOrganizationRating,
		Double overallAverageRating
) {
}
