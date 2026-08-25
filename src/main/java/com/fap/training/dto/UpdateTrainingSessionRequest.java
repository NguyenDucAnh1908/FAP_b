package com.fap.training.dto;

import com.fap.training.enums.TrainingSessionType;
import com.fap.training.enums.TrainingRegistrationMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UpdateTrainingSessionRequest(
		@NotBlank @Size(max = 255) String title,
		String description,
		@NotNull Long trainerId,
		@Size(max = 100) String room,
		@NotNull LocalDate sessionDate,
		@NotNull LocalDateTime startTime,
		@NotNull LocalDateTime endTime,
		@NotNull TrainingSessionType sessionType,
		@Size(max = 512) String meetingLink,
		@NotNull @Min(1) Integer capacity,
		@NotNull TrainingRegistrationMode registrationMode
) {
}
