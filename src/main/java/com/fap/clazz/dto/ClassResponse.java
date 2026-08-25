package com.fap.clazz.dto;

import com.fap.clazz.enums.ClassStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

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
		Integer capacity,
		long enrolledCount,
		long waitlistCount,
		boolean selfEnrollmentEnabled,
		LocalDate enrollmentStartDate,
		LocalDate enrollmentEndDate,
		BigDecimal minimumAttendanceRate,
		Long createdBy,
		Long updatedBy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
