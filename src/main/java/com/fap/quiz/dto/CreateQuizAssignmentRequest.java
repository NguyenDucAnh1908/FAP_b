package com.fap.quiz.dto;

public record CreateQuizAssignmentRequest(
		Long classId,
		Long trainingSessionId
) {
}
