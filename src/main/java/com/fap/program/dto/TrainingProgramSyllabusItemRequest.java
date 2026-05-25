package com.fap.program.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TrainingProgramSyllabusItemRequest(
		@NotNull Long syllabusId,
		@NotNull @Min(1) Integer sortOrder
) {
}
