package com.fap.program.dto;

import com.fap.program.enums.TrainingProgramStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTrainingProgramStatusRequest(
		@NotNull TrainingProgramStatus status
) {
}
