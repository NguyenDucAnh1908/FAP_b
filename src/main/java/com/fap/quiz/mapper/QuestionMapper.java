package com.fap.quiz.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fap.quiz.dto.QuestionResponse;
import com.fap.quiz.entity.Question;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {

	private final ObjectMapper objectMapper;

	public QuestionMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public QuestionResponse toResponse(Question question) {
		return new QuestionResponse(
				question.getId(),
				question.getContent(),
				question.getQuestionType(),
				question.getCategory(),
				question.getDifficulty(),
				readJson(question.getOptionsJson()),
				readJson(question.getCorrectAnswersJson()),
				question.getExplanation(),
				question.getCreatedBy(),
				question.getUpdatedBy(),
				question.getCreatedAt(),
				question.getUpdatedAt());
	}

	private JsonNode readJson(String value) {
		try {
			return objectMapper.readTree(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Stored question JSON is invalid", exception);
		}
	}
}
