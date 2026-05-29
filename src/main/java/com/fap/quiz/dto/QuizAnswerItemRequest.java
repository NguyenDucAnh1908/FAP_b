package com.fap.quiz.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record QuizAnswerItemRequest(
		@NotNull Long questionId,
		@NotNull JsonNode selectedAnswersJson
) {
}
