package com.fap.syllabus.dto;

import com.fap.syllabus.enums.SyllabusTopicStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public record CreateFullSyllabusRequest(
		@NotBlank @Size(max = 255) String name,
		@NotBlank @Size(max = 50) String code,
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
		String assessmentText,
		@NotNull Set<@Pattern(regexp = "H4SD|K6SD|H1SD|C3SD|H2SD") String> outputStandards,
		@NotNull List<@Valid DayRequest> days
) {

	public record DayRequest(
			@NotNull @Min(1) Integer dayNumber,
			@NotNull @Min(1) Integer sortOrder,
			@NotNull List<@Valid UnitRequest> units
	) {
	}

	public record UnitRequest(
			@NotBlank @Size(max = 255) String name,
			@NotNull @Min(1) Integer sortOrder,
			@NotNull List<@Valid TopicRequest> topics
	) {
	}

	public record TopicRequest(
			@NotBlank @Size(max = 255) String name,
			@NotBlank @Pattern(regexp = "H4SD|K6SD|H1SD|C3SD|H2SD") String outputStandard,
			@NotNull Boolean online,
			@NotNull @Min(1) Integer durationMinutes,
			@NotNull SyllabusTopicStatus status,
			@NotNull @Min(1) Integer sortOrder,
			List<@Valid MaterialRequest> materials
	) {
	}

	public record MaterialRequest(
			Long id,
			@NotBlank @Size(max = 255) String fileName,
			@NotBlank @Size(max = 512) String fileUrl,
			@Min(1) Long fileSize,
			@Size(max = 100) String contentType
	) {
	}
}
