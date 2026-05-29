package com.fap.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateQuizQuestionsRequest(
		@NotEmpty List<@Valid QuizQuestionItemRequest> questions
) {
}
