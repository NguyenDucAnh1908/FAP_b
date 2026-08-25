package com.fap.result.dto;

import java.math.BigDecimal;
import java.util.List;

public record CompletionPolicyResponse(
		Long classId,
		BigDecimal minimumAttendanceRate,
		List<CompletionPolicyQuizResponse> requiredQuizzes
) {
}
