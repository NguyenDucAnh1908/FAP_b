package com.fap.result.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record UpdateCompletionPolicyRequest(
		@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal minimumAttendanceRate,
		@NotNull List<@Valid CompletionPolicyQuizRequest> requiredQuizzes
) {
}
