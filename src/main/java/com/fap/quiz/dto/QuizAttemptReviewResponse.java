package com.fap.quiz.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fap.quiz.enums.QuizAttemptStatus;

import java.time.LocalDateTime;
import java.util.List;

public record QuizAttemptReviewResponse(
		Long id,
		Long quizId,
		String quizTitle,
		Integer attemptNumber,
		QuizAttemptStatus status,
		JsonNode answersJson,
		Integer score,
		Integer correctCount,
		Integer totalQuestions,
		Boolean passed,
		Integer timeTakenSeconds,
		LocalDateTime startedAt,
		LocalDateTime submittedAt,
		List<QuizAttemptReviewQuestionResponse> questions
) {
}
