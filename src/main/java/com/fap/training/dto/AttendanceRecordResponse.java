package com.fap.training.dto;

import com.fap.training.enums.AttendanceCheckInMethod;
import com.fap.training.enums.AttendanceStatus;

import java.time.LocalDateTime;

public record AttendanceRecordResponse(
		Long id,
		Long trainingSessionId,
		Long userId,
		String userFullName,
		String userEmail,
		AttendanceStatus status,
		LocalDateTime checkedInAt,
		AttendanceCheckInMethod checkInMethod,
		Long updatedBy,
		String correctionReason,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
