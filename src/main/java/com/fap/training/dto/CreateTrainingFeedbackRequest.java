package com.fap.training.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTrainingFeedbackRequest(
		@NotNull @Min(1) @Max(5) Integer ratingContent,
		@NotNull @Min(1) @Max(5) Integer ratingTrainer,
		@NotNull @Min(1) @Max(5) Integer ratingOrganization,
		@Size(max = 2000) String comment
) {
}
