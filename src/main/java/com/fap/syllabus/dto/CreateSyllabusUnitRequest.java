package com.fap.syllabus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSyllabusUnitRequest(
		@NotBlank @Size(max = 255) String name,
		@NotNull @Min(1) Integer sortOrder
) {
}
