package com.fap.training.dto;

import java.util.List;

public record MyTrainerDashboardResponse(
		long assignedClasses,
		long upcomingSessions,
		long completedSessions,
		long pendingAttendanceSessions,
		List<TrainingSessionResponse> nextSessions,
		List<TrainingSessionResponse> recentCompletedSessions
) {
}
