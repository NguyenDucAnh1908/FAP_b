package com.fap.training.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateAttendanceRequest(
		@NotEmpty List<@Valid AttendanceItemRequest> records
) {
}
