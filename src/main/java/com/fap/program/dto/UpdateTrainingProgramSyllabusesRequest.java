package com.fap.program.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateTrainingProgramSyllabusesRequest(
		@NotEmpty List<@Valid TrainingProgramSyllabusItemRequest> syllabuses
) {
}
