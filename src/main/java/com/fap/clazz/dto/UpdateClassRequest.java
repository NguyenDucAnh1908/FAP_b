package com.fap.clazz.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateClassRequest(
		@Size(max = 255) String name,
		@Size(max = 100) String location,
		@Size(max = 255) String locationDetail,
		@Size(max = 20) String fsu,
		@Size(max = 50) String classTime,
		LocalDate startDate,
		LocalDate endDate,
		@Size(max = 50) String duration
) {
}
