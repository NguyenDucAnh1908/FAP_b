package com.fap.quiz.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fap.quiz.enums.QuestionDifficulty;
import com.fap.quiz.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateQuestionRequest(
		@NotBlank String content,
		@NotNull QuestionType questionType,
		@NotBlank @Size(max = 100) String category,
		@NotNull QuestionDifficulty difficulty,
		@NotNull JsonNode optionsJson,
		@NotNull JsonNode correctAnswersJson,
		String explanation
) {
}
