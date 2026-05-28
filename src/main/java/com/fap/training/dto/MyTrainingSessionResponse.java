package com.fap.training.dto;

import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.enums.TrainingSessionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MyTrainingSessionResponse(
		Long trainingSessionId,
		String title,
		String description,
		TrainingSessionStatus status,
		TrainingSessionType sessionType,
		LocalDate sessionDate,
		LocalDateTime startTime,
		LocalDateTime endTime,
		String room,
		String meetingLink,
		Integer capacity,
		Integer enrolledCount,
		Long classId,
		String className,
		String classCode,
		Long trainerId,
		String trainerFullName,
		String trainerEmail,
		Long registrationId,
		TrainingRegistrationStatus registrationStatus,
		LocalDateTime registeredAt,
		LocalDateTime cancelledAt,
		LocalDateTime completedAt
) {
}
