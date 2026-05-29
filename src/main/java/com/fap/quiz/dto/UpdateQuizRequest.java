package com.fap.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateQuizRequest(
		@NotBlank @Size(max = 255) String title,
		String description,
		@NotNull @Min(1) Integer durationMinutes,
		@NotNull @Min(0) @Max(100) Integer passingScore,
		@NotNull @Min(1) Integer maxAttempts,
		boolean randomize,
		@NotBlank @Size(max = 100) String category,
		LocalDate openDate,
		LocalDate closeDate
) {
}
