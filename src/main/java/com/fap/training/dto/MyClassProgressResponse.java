package com.fap.training.dto;

import com.fap.clazz.dto.ClassResponse;

public record MyClassProgressResponse(
		ClassResponse classInfo,
		SessionProgress sessions,
		AttendanceProgress attendance,
		MaterialProgress materials,
		QuizProgress quizzes
) {

	public record SessionProgress(
			long total,
			long completed,
			long upcoming,
			long canceled
	) {
	}

	public record AttendanceProgress(
			long present,
			long late,
			long absent
	) {
	}

	public record MaterialProgress(
			long total
	) {
	}

	public record QuizProgress(
			long assigned,
			long attempted,
			long passed,
			long remaining,
			Long latestAttemptId,
			Integer latestScore,
			Boolean latestPassed
	) {
	}
}
