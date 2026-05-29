package com.fap.quiz.dto;

import com.fap.quiz.enums.QuizStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record QuizResponse(
		Long id,
		String title,
		String description,
		Integer durationMinutes,
		Integer passingScore,
		Integer maxAttempts,
		boolean randomize,
		String category,
		QuizStatus status,
		LocalDate openDate,
		LocalDate closeDate,
		long questionCount,
		Long createdBy,
		Long updatedBy,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
