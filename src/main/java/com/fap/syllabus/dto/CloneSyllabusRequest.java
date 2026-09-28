package com.fap.syllabus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloneSyllabusRequest(
		@NotBlank @Size(max = 255) String name,
		@NotBlank @Size(max = 50) String code,
		@NotBlank @Size(max = 20) String version
) {
}
