package com.fap.result.dto;

import com.fap.result.enums.CourseResultStatus;

import java.time.LocalDateTime;

public record CourseResultAdjustmentResponse(
		Long id,
		CourseResultStatus previousStatus,
		CourseResultStatus newStatus,
		String reason,
		Long adjustedBy,
		LocalDateTime adjustedAt
) {
}
