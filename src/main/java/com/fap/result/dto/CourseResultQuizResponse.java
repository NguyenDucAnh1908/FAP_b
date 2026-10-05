package com.fap.result.dto;

public record CourseResultQuizResponse(
		Long quizId,
		String title,
		Integer requiredScore,
		Long bestAttemptId,
		Integer bestScore,
		boolean passed
) {
}
