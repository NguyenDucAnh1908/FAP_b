package com.fap.result.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CompletionPolicyQuizRequest(
		@NotNull Long quizId,
		@NotNull @Min(0) @Max(100) Integer passingScore
) {
}
