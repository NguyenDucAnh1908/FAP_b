package com.fap.quiz.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fap.quiz.enums.QuestionDifficulty;
import com.fap.quiz.enums.QuestionType;

import java.time.LocalDateTime;

public record QuestionResponse(
		Long id,
		String content,
		QuestionType questionType,
		String category,
		QuestionDifficulty difficulty,
		JsonNode optionsJson,
		JsonNode correctAnswersJson,
		String explanation,
		Long createdBy,
		Long updatedBy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
