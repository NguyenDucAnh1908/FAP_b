package com.fap.result.dto;

import com.fap.result.enums.CourseResultStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CourseResultResponse(
		Long id,
		Long classId,
		Long enrollmentId,
		Long userId,
		String traineeName,
		String traineeEmail,
		CourseResultStatus status,
		CourseResultStatus calculatedStatus,
		CourseResultStatus overrideStatus,
		BigDecimal attendanceRate,
		Integer attendedSessions,
		Integer totalSessions,
		Integer requiredQuizCount,
		Integer passedQuizCount,
		String overrideReason,
		Long overriddenBy,
		LocalDateTime overriddenAt,
		boolean published,
		LocalDateTime publishedAt,
		LocalDateTime calculatedAt,
		Long version,
		List<CourseResultQuizResponse> quizzes,
		List<CourseResultAdjustmentResponse> adjustments
) {
}
