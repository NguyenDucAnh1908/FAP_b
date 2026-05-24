package com.fap.syllabus.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateSyllabusRequest(
		@NotBlank @Size(max = 255) String name,
		@NotBlank @Size(max = 20) String version,
		@NotBlank @Pattern(regexp = "Beginner|Intermediate|Advanced|All levels") String levelName,
		@NotNull @Min(1) Integer attendees,
		@Size(max = 50) String duration,
		String technicalRequirements,
		String courseObjectives,
		String rules,
		@NotNull @Min(0) @Max(100) Integer timeAllocAssignmentLab,
		@NotNull @Min(0) @Max(100) Integer timeAllocConceptLecture,
		@NotNull @Min(0) @Max(100) Integer timeAllocGuideReview,
		@NotNull @Min(0) @Max(100) Integer timeAllocTestQuiz,
		@NotNull @Min(0) @Max(100) Integer assessQuizPct,
		@NotNull @Min(0) @Max(100) Integer assessAssignmentPct,
		@NotNull @Min(0) @Max(100) Integer assessFinalPct,
		String assessmentText
) {
}
