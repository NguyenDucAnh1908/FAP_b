package com.fap.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.api.PageRequestFactory;
import com.fap.common.exception.NotFoundException;
import com.fap.quiz.dto.CreateQuestionRequest;
import com.fap.quiz.dto.QuestionResponse;
import com.fap.quiz.dto.UpdateQuestionRequest;
import com.fap.quiz.entity.Question;
import com.fap.quiz.enums.QuestionDifficulty;
import com.fap.quiz.enums.QuestionType;
import com.fap.quiz.mapper.QuestionMapper;
import com.fap.quiz.repository.QuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class QuestionService {

	private final QuestionRepository questionRepository;
	private final QuestionMapper questionMapper;
	private final ObjectMapper objectMapper;
	private final AuditLogService auditLogService;

	public QuestionService(
			QuestionRepository questionRepository,
			QuestionMapper questionMapper,
			ObjectMapper objectMapper,
			AuditLogService auditLogService) {
		this.questionRepository = questionRepository;
		this.questionMapper = questionMapper;
		this.objectMapper = objectMapper;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public Page<QuestionResponse> list(
			QuestionType questionType,
			QuestionDifficulty difficulty,
			String category,
			String keyword,
			int page,
			int limit) {
		return list(questionType, difficulty, category, keyword, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<QuestionResponse> list(
			QuestionType questionType,
			QuestionDifficulty difficulty,
			String category,
			String keyword,
			int page,
			int limit,
			String sortBy,
			String order) {
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.DESC, "id"),
				"id", "category", "questionType", "difficulty", "createdAt");
		PageRequest nativePageRequest = PageRequestFactory.mapSortFields(pageRequest, Map.of(
				"questionType", "question_type",
				"createdAt", "created_at"));
		return questionRepository.search(
						enumName(questionType),
						enumName(difficulty),
						normalize(category),
						normalize(keyword),
						nativePageRequest)
				.map(questionMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public QuestionResponse get(Long id) {
		return questionMapper.toResponse(findQuestion(id));
	}

	@Transactional
	public QuestionResponse create(CreateQuestionRequest request, Long currentUserId) {
		LocalDateTime now = LocalDateTime.now();
		Question question = new Question();
		applyFields(question, request);
		question.setCreatedAt(now);
		question.setUpdatedAt(now);
		question.setCreatedBy(currentUserId);
		question.setUpdatedBy(currentUserId);
		Question saved = questionRepository.save(question);
		auditLogService.record("CREATE_QUESTION", "question", saved.getId());
		return questionMapper.toResponse(saved);
	}

	@Transactional
	public QuestionResponse update(Long id, UpdateQuestionRequest request, Long currentUserId) {
		Question question = findQuestion(id);
		applyFields(question, request);
		question.setUpdatedAt(LocalDateTime.now());
		question.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_QUESTION", "question", question.getId());
		return questionMapper.toResponse(question);
	}

	@Transactional
	public void delete(Long id, Long currentUserId) {
		Question question = findQuestion(id);
		LocalDateTime now = LocalDateTime.now();
		question.setDeleted(true);
		question.setDeletedAt(now);
		question.setUpdatedAt(now);
		question.setUpdatedBy(currentUserId);
		auditLogService.record("DELETE_QUESTION", "question", question.getId());
	}

	private void applyFields(Question question, CreateQuestionRequest request) {
		validateJsonFields(request.questionType(), request.optionsJson(), request.correctAnswersJson());
		question.setContent(request.content().trim());
		question.setQuestionType(request.questionType());
		question.setCategory(request.category().trim());
		question.setDifficulty(request.difficulty());
		question.setOptionsJson(writeJson(request.optionsJson(), "INVALID_QUESTION_OPTIONS_JSON", "Question options must be valid JSON"));
		question.setCorrectAnswersJson(writeJson(request.correctAnswersJson(), "INVALID_QUESTION_CORRECT_ANSWERS_JSON", "Question correct answers must be valid JSON"));
		question.setExplanation(normalize(request.explanation()));
	}

	private void applyFields(Question question, UpdateQuestionRequest request) {
		validateJsonFields(request.questionType(), request.optionsJson(), request.correctAnswersJson());
		question.setContent(request.content().trim());
		question.setQuestionType(request.questionType());
		question.setCategory(request.category().trim());
		question.setDifficulty(request.difficulty());
		question.setOptionsJson(writeJson(request.optionsJson(), "INVALID_QUESTION_OPTIONS_JSON", "Question options must be valid JSON"));
		question.setCorrectAnswersJson(writeJson(request.correctAnswersJson(), "INVALID_QUESTION_CORRECT_ANSWERS_JSON", "Question correct answers must be valid JSON"));
		question.setExplanation(normalize(request.explanation()));
	}

	private void validateJsonFields(QuestionType questionType, JsonNode optionsJson, JsonNode correctAnswersJson) {
		if (optionsJson == null || !optionsJson.isArray() || optionsJson.isEmpty()) {
			throw new BadRequestException("INVALID_QUESTION_OPTIONS_JSON", "Question options must be a non-empty JSON array");
		}
		if (correctAnswersJson == null || !correctAnswersJson.isArray() || correctAnswersJson.isEmpty()) {
			throw new BadRequestException("INVALID_QUESTION_CORRECT_ANSWERS_JSON", "Question correct answers must be a non-empty JSON array");
		}
		if (questionType == QuestionType.single && correctAnswersJson.size() != 1) {
			throw new BadRequestException("INVALID_SINGLE_QUESTION_ANSWER", "Single-choice questions must have exactly one correct answer");
		}
	}

	private String writeJson(JsonNode value, String code, String message) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new BadRequestException(code, message);
		}
	}

	private Question findQuestion(Long id) {
		return questionRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Question not found"));
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String enumName(Enum<?> value) {
		return value == null ? null : value.name();
	}
}
