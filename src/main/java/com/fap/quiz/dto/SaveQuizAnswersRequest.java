package com.fap.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaveQuizAnswersRequest(
		@NotNull List<@Valid QuizAnswerItemRequest> answers
) {
}
