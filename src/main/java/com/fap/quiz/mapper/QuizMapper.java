package com.fap.quiz.mapper;

import com.fap.quiz.dto.QuizQuestionResponse;
import com.fap.quiz.dto.QuizResponse;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizQuestion;
import org.springframework.stereotype.Component;

@Component
public class QuizMapper {

	private final QuestionMapper questionMapper;

	public QuizMapper(QuestionMapper questionMapper) {
		this.questionMapper = questionMapper;
	}

	public QuizResponse toResponse(Quiz quiz, long questionCount) {
		return new QuizResponse(
				quiz.getId(),
				quiz.getTitle(),
				quiz.getDescription(),
				quiz.getDurationMinutes(),
				quiz.getPassingScore(),
				quiz.getMaxAttempts(),
				quiz.isRandomize(),
				quiz.getCategory(),
				quiz.getStatus(),
				quiz.getOpenDate(),
				quiz.getCloseDate(),
				questionCount,
				quiz.getCreatedBy(),
				quiz.getUpdatedBy(),
				quiz.getCreatedAt(),
				quiz.getUpdatedAt());
	}

	public QuizQuestionResponse toResponse(QuizQuestion quizQuestion) {
		return new QuizQuestionResponse(
				quizQuestion.getQuestion().getId(),
				quizQuestion.getSortOrder(),
				quizQuestion.getPoints(),
				questionMapper.toResponse(quizQuestion.getQuestion()));
	}
}
