package com.fap.clazz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateClassRequest(
		@NotBlank @Size(max = 255) String name,
		@NotBlank @Size(max = 100) String classCode,
		@NotNull Long trainingProgramId,
		@Size(max = 100) String location,
		@Size(max = 255) String locationDetail,
		@Size(max = 20) String fsu,
		@Size(max = 50) String classTime,
		LocalDate startDate,
		LocalDate endDate,
		@Size(max = 50) String duration
) {
}
