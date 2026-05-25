package com.fap.program.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTrainingProgramRequest(
		@NotBlank @Size(max = 255) String name,
		@Size(max = 50) String duration,
		@Min(0) Integer totalHours,
		@NotBlank @Size(max = 20) String version
) {
}
