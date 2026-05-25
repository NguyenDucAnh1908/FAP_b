package com.fap.program.dto;

import com.fap.program.enums.TrainingProgramStatus;

import java.time.LocalDateTime;

public record TrainingProgramResponse(
		Long id,
		String name,
		TrainingProgramStatus status,
		String duration,
		Integer totalHours,
		String version,
		Long createdBy,
		Long updatedBy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
