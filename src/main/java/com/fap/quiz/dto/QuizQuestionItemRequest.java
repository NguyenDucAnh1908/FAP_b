package com.fap.quiz.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record QuizQuestionItemRequest(
		@NotNull Long questionId,
		@NotNull @Min(1) Integer sortOrder,
		@NotNull @DecimalMin("0.01") @Digits(integer = 3, fraction = 2) BigDecimal points
) {
}
