package com.fap.quiz.dto;

import java.time.LocalDateTime;

public record QuizAssignmentResponse(
		Long id,
		Long quizId,
		String quizTitle,
		Long classId,
		String className,
		String classCode,
		Long trainingSessionId,
		String trainingSessionTitle,
		Long assignedBy,
		String assignedByName,
		String assignedByEmail,
		LocalDateTime assignedAt
) {
}
