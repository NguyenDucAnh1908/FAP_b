package com.fap.quiz.dto;

import com.fap.quiz.enums.QuizStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateQuizStatusRequest(
		@NotNull QuizStatus status
) {
}
