package com.fap.result.dto;

import com.fap.quiz.enums.QuizStatus;

public record CompletionPolicyQuizResponse(
		Long quizId,
		String title,
		Integer passingScore,
		QuizStatus quizStatus
) {
}
