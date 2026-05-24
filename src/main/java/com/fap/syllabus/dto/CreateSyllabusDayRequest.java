package com.fap.syllabus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateSyllabusDayRequest(
		@NotNull @Min(1) Integer dayNumber,
		@NotNull @Min(1) Integer sortOrder
) {
}
