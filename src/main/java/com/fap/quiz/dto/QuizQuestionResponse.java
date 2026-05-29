package com.fap.quiz.dto;

import java.math.BigDecimal;

public record QuizQuestionResponse(
		Long questionId,
		Integer sortOrder,
		BigDecimal points,
		QuestionResponse question
) {
}
