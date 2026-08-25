package com.fap.dashboard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingAnalyticsResponse(
		String scope,
		LocalDate fromDate,
		LocalDate toDate,
		long totalSessions,
		long totalParticipants,
		long pendingAttendanceSessions,
		long totalAttendanceRecords,
		double attendanceRate,
		double completionRate,
		long feedbackResponses,
		double averageFeedbackRating,
		List<StatusCount> sessionStatuses,
		List<StatusCount> attendanceStatuses,
		List<StatusCount> registrationStatuses,
		LocalDateTime generatedAt
) {

	public record StatusCount(String status, long count) {
	}
}
