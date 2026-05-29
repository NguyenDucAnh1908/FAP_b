package com.fap.quiz.dto;

import com.fap.quiz.enums.QuizAttemptStatus;

import java.time.LocalDate;

public record AssignedQuizResponse(
		Long id,
		String title,
		String description,
		Integer durationMinutes,
		Integer passingScore,
		Integer maxAttempts,
		String category,
		LocalDate openDate,
		LocalDate closeDate,
		long questionCount,
		long attemptCount,
		long remainingAttempts,
		Long latestAttemptId,
		QuizAttemptStatus latestAttemptStatus,
		Integer latestScore,
		Boolean latestPassed
) {
}
