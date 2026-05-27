package com.fap.training.dto;

import com.fap.training.enums.TrainingSessionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTrainingSessionStatusRequest(
		@NotNull TrainingSessionStatus status
) {
}
