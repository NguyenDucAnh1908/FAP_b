package com.fap.training.dto;

import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.enums.TrainingSessionType;
import com.fap.training.enums.TrainingRegistrationMode;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TrainingSessionResponse(
		Long id,
		Long classId,
		String className,
		String classCode,
		String title,
		String description,
		Long trainerId,
		String trainerFullName,
		String trainerEmail,
		String room,
		LocalDate sessionDate,
		LocalDateTime startTime,
		LocalDateTime endTime,
		TrainingSessionType sessionType,
		String meetingLink,
		Integer capacity,
		Integer enrolledCount,
		TrainingRegistrationMode registrationMode,
		TrainingSessionStatus status,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
