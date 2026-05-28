package com.fap.training.dto;

import java.util.List;

public record MyTrainingDashboardResponse(
		long registeredSessions,
		long upcomingSessions,
		long completedSessions,
		long waitlistedSessions,
		AttendanceSummary attendanceSummary,
		List<MyTrainingSessionResponse> nextSessions,
		List<MyAttendanceResponse> recentAttendance
) {

	public record AttendanceSummary(
			long present,
			long late,
			long absent
	) {
	}
}
