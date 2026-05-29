package com.fap.quiz.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fap.quiz.dto.AssignedQuizResponse;
import com.fap.quiz.dto.QuizAttemptQuestionResponse;
import com.fap.quiz.dto.QuizAttemptResponse;
import com.fap.quiz.entity.Question;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.entity.QuizQuestion;
import com.fap.quiz.enums.QuizAttemptStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuizAttemptMapper {

	private final ObjectMapper objectMapper;

	public QuizAttemptMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public AssignedQuizResponse toAssignedResponse(
			Quiz quiz,
			long questionCount,
			long attemptCount,
			QuizAttempt latestAttempt) {
		long remainingAttempts = Math.max(quiz.getMaxAttempts() - attemptCount, 0);
		return new AssignedQuizResponse(
				quiz.getId(),
				quiz.getTitle(),
				quiz.getDescription(),
				quiz.getDurationMinutes(),
				quiz.getPassingScore(),
				quiz.getMaxAttempts(),
				quiz.getCategory(),
				quiz.getOpenDate(),
				quiz.getCloseDate(),
				questionCount,
				attemptCount,
				remainingAttempts,
				latestAttempt == null ? null : latestAttempt.getId(),
				latestAttempt == null ? null : latestAttempt.getStatus(),
				latestAttempt == null ? null : latestAttempt.getScore(),
				latestAttempt == null ? null : latestAttempt.getPassed());
	}

	public QuizAttemptResponse toResponse(QuizAttempt attempt, List<QuizQuestion> questions) {
		return new QuizAttemptResponse(
				attempt.getId(),
				attempt.getQuiz().getId(),
				attempt.getQuiz().getTitle(),
				attempt.getAttemptNumber(),
				attempt.getStatus(),
				readJson(attempt.getAnswersJson()),
				attempt.getScore(),
				attempt.getCorrectCount(),
				attempt.getTotalQuestions(),
				attempt.getPassed(),
				attempt.getTimeTakenSeconds(),
				attempt.getStartedAt(),
				attempt.getSubmittedAt(),
				questions.stream()
						.map(this::toQuestionResponse)
						.toList());
	}

	public QuizAttemptQuestionResponse toQuestionResponse(QuizQuestion quizQuestion) {
		Question question = quizQuestion.getQuestion();
		return new QuizAttemptQuestionResponse(
				question.getId(),
				quizQuestion.getSortOrder(),
				quizQuestion.getPoints(),
				question.getContent(),
				question.getQuestionType(),
				question.getCategory(),
				question.getDifficulty(),
				readJson(question.getOptionsJson()));
	}

	private JsonNode readJson(String value) {
		try {
			return objectMapper.readTree(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Stored quiz JSON is invalid", exception);
		}
	}
}
