package com.fap.dashboard.dto;

import com.fap.training.dto.TrainingSessionResponse;

import java.time.LocalDateTime;
import java.util.List;

public record AdminDashboardResponse(
		UserSummary users,
		ContentSummary content,
		TrainingAnalyticsResponse training,
		AssessmentSummary assessment,
		List<TrainingSessionResponse> nextSessions,
		List<RecentActivity> recentActivities,
		LocalDateTime generatedAt
) {

	public record UserSummary(
			long totalUsers,
			long activeUsers,
			long inactiveUsers,
			long activeTrainees,
			long activeTrainers
	) {
	}

	public record ContentSummary(
			long totalSyllabuses,
			long activeSyllabuses,
			long pendingSyllabuses,
			long draftingSyllabuses,
			long totalPrograms,
			long activePrograms,
			long totalClasses,
			long activeClasses,
			long planningClasses
	) {
	}

	public record AssessmentSummary(
			long totalQuizzes,
			long publishedQuizzes,
			long submittedAttempts,
			long passedAttempts,
			double passRate
	) {
	}

	public record RecentActivity(
			Long id,
			String action,
			String entityType,
			Long entityId,
			LocalDateTime createdAt
	) {
	}
}
