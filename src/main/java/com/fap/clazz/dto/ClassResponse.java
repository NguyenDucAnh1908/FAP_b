package com.fap.clazz.dto;

import com.fap.clazz.enums.ClassStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClassResponse(
		Long id,
		String name,
		String classCode,
		Long trainingProgramId,
		String trainingProgramName,
		ClassStatus status,
		String location,
		String locationDetail,
		String fsu,
		String classTime,
		LocalDate startDate,
		LocalDate endDate,
		String duration,
		Long createdBy,
		Long updatedBy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
