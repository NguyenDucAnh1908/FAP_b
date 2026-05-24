package com.fap.syllabus.dto;

import com.fap.syllabus.enums.SyllabusStatus;

import java.time.LocalDateTime;

public record SyllabusResponse(
		Long id,
		String name,
		String code,
		String version,
		SyllabusStatus status,
		String levelName,
		Integer attendees,
		String duration,
		String technicalRequirements,
		String courseObjectives,
		String rules,
		Integer timeAllocAssignmentLab,
		Integer timeAllocConceptLecture,
		Integer timeAllocGuideReview,
		Integer timeAllocTestQuiz,
		Integer assessQuizPct,
		Integer assessAssignmentPct,
		Integer assessFinalPct,
		String assessmentText,
		Long createdBy,
		Long updatedBy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
