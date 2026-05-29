package com.fap.quiz.dto;

public record QuizAttemptSummaryResponse(
		Long quizId,
		String quizTitle,
		long totalAttempts,
		long inProgressAttempts,
		long submittedAttempts,
		long passedAttempts,
		long failedAttempts,
		double passRate,
		Double averageScore,
		Integer highestScore,
		Integer lowestScore
) {
}
