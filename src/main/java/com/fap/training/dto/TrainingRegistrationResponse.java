package com.fap.training.dto;

import com.fap.training.enums.TrainingRegistrationStatus;

import java.time.LocalDateTime;

public record TrainingRegistrationResponse(
		Long id,
		Long trainingSessionId,
		Long userId,
		String userFullName,
		String userEmail,
		TrainingRegistrationStatus status,
		LocalDateTime registeredAt,
		LocalDateTime cancelledAt,
		LocalDateTime completedAt
) {
}
