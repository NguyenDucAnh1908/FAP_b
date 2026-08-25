package com.fap.clazz.dto;

import com.fap.clazz.enums.ClassEnrollmentSource;
import com.fap.clazz.enums.ClassEnrollmentStatus;

import java.time.LocalDateTime;

public record ClassEnrollmentResponse(
		Long id,
		Long classId,
		String className,
		String classCode,
		Long userId,
		String userFullName,
		String userEmail,
		ClassEnrollmentStatus status,
		ClassEnrollmentSource source,
		LocalDateTime enrolledAt,
		LocalDateTime withdrawnAt,
		LocalDateTime completedAt,
		LocalDateTime reviewedAt,
		Long reviewedBy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
