package com.fap.quiz.dto;

import com.fap.quiz.enums.QuizAttemptStatus;

import java.time.LocalDateTime;

public record QuizAttemptResultResponse(
		Long id,
		Long quizId,
		String quizTitle,
		Long userId,
		String userFullName,
		String userEmail,
		Integer attemptNumber,
		QuizAttemptStatus status,
		Integer score,
		Integer correctCount,
		Integer totalQuestions,
		Boolean passed,
		Integer timeTakenSeconds,
		LocalDateTime startedAt,
		LocalDateTime submittedAt
) {
}
