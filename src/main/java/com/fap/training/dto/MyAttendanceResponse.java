package com.fap.training.dto;

import com.fap.training.enums.AttendanceCheckInMethod;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.enums.TrainingSessionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MyAttendanceResponse(
		Long attendanceId,
		AttendanceStatus attendanceStatus,
		LocalDateTime checkedInAt,
		AttendanceCheckInMethod checkInMethod,
		String correctionReason,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
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
