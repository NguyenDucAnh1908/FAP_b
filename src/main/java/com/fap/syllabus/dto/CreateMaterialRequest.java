package com.fap.syllabus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMaterialRequest(
		@NotNull Long syllabusId,
		@NotNull Long topicId,
		@NotBlank @Size(max = 255) String fileName,
		@NotBlank @Size(max = 512) String fileUrl,
		@Min(1) Long fileSize,
		@Size(max = 100) String contentType
) {
}
