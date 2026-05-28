package com.fap.training.dto;

import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.enums.TrainingSessionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MyTrainingRegistrationResponse(
		Long registrationId,
		TrainingRegistrationStatus registrationStatus,
		LocalDateTime registeredAt,
		LocalDateTime cancelledAt,
		LocalDateTime completedAt,
		Long trainingSessionId,
		String trainingSessionTitle,
		TrainingSessionStatus trainingSessionStatus,
		TrainingSessionType sessionType,
		LocalDate sessionDate,
		LocalDateTime startTime,
		LocalDateTime endTime,
		String room,
		String meetingLink,
		Long classId,
		String className,
		String classCode,
		Long trainerId,
		String trainerFullName,
		String trainerEmail
) {
}
