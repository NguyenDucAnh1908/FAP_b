package com.fap.quiz.repository;

/**
 * Aggregate of quiz attempts computed in SQL by {@link QuizAttemptRepository#summarizeQuizResults}.
 * Score figures are {@code null} when no attempt has been submitted.
 */
public record QuizAttemptStats(
		Long totalAttempts,
		Long inProgressAttempts,
		Long submittedAttempts,
		Long passedAttempts,
		Double averageScore,
		Integer highestScore,
		Integer lowestScore
) {
}
