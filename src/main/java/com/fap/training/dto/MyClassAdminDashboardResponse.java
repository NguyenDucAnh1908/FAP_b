package com.fap.training.dto;

import com.fap.clazz.dto.ClassResponse;

import java.util.List;

public record MyClassAdminDashboardResponse(
		long assignedClasses,
		long activeClasses,
		long planningClasses,
		long upcomingSessions,
		long pendingAttendanceSessions,
		long totalTrainers,
		long totalParticipants,
		List<ClassResponse> classesStartingSoon,
		List<TrainingSessionResponse> recentSessions
) {
}
