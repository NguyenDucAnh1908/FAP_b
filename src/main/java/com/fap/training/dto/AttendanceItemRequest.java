package com.fap.training.dto;

import com.fap.training.enums.AttendanceCheckInMethod;
import com.fap.training.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AttendanceItemRequest(
		@NotNull Long userId,
		@NotNull AttendanceStatus status,
		LocalDateTime checkedInAt,
		AttendanceCheckInMethod checkInMethod,
		@Size(max = 500) String correctionReason
) {
}
