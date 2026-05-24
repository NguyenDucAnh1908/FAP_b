package com.fap.syllabus.dto;

import com.fap.syllabus.enums.SyllabusTopicStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSyllabusTopicRequest(
		@NotBlank @Size(max = 255) String name,
		@NotBlank @Pattern(regexp = "H4SD|K6SD|H1SD|C3SD|H2SD") String outputStandard,
		@NotNull Boolean online,
		@NotNull @Min(1) Integer durationMinutes,
		@NotNull SyllabusTopicStatus status,
		@NotNull @Min(1) Integer sortOrder
) {
}
