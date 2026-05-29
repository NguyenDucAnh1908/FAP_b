package com.fap.quiz.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fap.quiz.enums.QuestionDifficulty;
import com.fap.quiz.enums.QuestionType;

import java.math.BigDecimal;

public record QuizAttemptQuestionResponse(
		Long questionId,
		Integer sortOrder,
		BigDecimal points,
		String content,
		QuestionType questionType,
		String category,
		QuestionDifficulty difficulty,
		JsonNode optionsJson
) {
}
