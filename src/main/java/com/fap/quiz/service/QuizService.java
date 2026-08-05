package com.fap.quiz.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.api.PageRequestFactory;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.quiz.dto.CreateQuizRequest;
import com.fap.quiz.dto.QuizQuestionItemRequest;
import com.fap.quiz.dto.QuizQuestionResponse;
import com.fap.quiz.dto.QuizResponse;
import com.fap.quiz.dto.UpdateQuizQuestionsRequest;
import com.fap.quiz.dto.UpdateQuizRequest;
import com.fap.quiz.dto.UpdateQuizStatusRequest;
import com.fap.quiz.entity.Question;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizQuestion;
import com.fap.quiz.entity.QuizQuestionId;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.mapper.QuizMapper;
import com.fap.quiz.repository.QuestionRepository;
import com.fap.quiz.repository.QuizQuestionRepository;
import com.fap.quiz.repository.QuizRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizService {

	private final QuizRepository quizRepository;
	private final QuizQuestionRepository quizQuestionRepository;
	private final QuestionRepository questionRepository;
	private final QuizMapper quizMapper;
	private final AuditLogService auditLogService;

	public QuizService(
			QuizRepository quizRepository,
			QuizQuestionRepository quizQuestionRepository,
			QuestionRepository questionRepository,
			QuizMapper quizMapper,
			AuditLogService auditLogService) {
		this.quizRepository = quizRepository;
		this.quizQuestionRepository = quizQuestionRepository;
		this.questionRepository = questionRepository;
		this.quizMapper = quizMapper;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public Page<QuizResponse> list(QuizStatus status, String category, String keyword, int page, int limit) {
		return list(status, category, keyword, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<QuizResponse> list(
			QuizStatus status,
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
				"id", "title", "category", "status", "openDate", "closeDate", "createdAt");
		return quizRepository.search(enumName(status), normalize(category), normalize(keyword), pageRequest)
				.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public QuizResponse get(Long id) {
		return toResponse(findQuiz(id));
	}

	@Transactional
	public QuizResponse create(CreateQuizRequest request, Long currentUserId) {
		validateDateRange(request.openDate(), request.closeDate());
		LocalDateTime now = LocalDateTime.now();
		Quiz quiz = new Quiz();
		applyFields(quiz, request);
		quiz.setStatus(QuizStatus.Draft);
		quiz.setCreatedAt(now);
		quiz.setUpdatedAt(now);
		quiz.setCreatedBy(currentUserId);
		quiz.setUpdatedBy(currentUserId);
		Quiz saved = quizRepository.save(quiz);
		auditLogService.record("CREATE_QUIZ", "quiz", saved.getId());
		return toResponse(saved);
	}

	@Transactional
	public QuizResponse update(Long id, UpdateQuizRequest request, Long currentUserId) {
		Quiz quiz = findQuiz(id);
		ensureDraft(quiz);
		validateDateRange(request.openDate(), request.closeDate());
		applyFields(quiz, request);
		quiz.setUpdatedAt(LocalDateTime.now());
		quiz.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_QUIZ", "quiz", quiz.getId());
		return toResponse(quiz);
	}

	@Transactional
	public void delete(Long id, Long currentUserId) {
		Quiz quiz = findQuiz(id);
		ensureDraft(quiz);
		LocalDateTime now = LocalDateTime.now();
		quiz.setDeleted(true);
		quiz.setDeletedAt(now);
		quiz.setUpdatedAt(now);
		quiz.setUpdatedBy(currentUserId);
		auditLogService.record("DELETE_QUIZ", "quiz", quiz.getId());
	}

	@Transactional
	public QuizResponse updateStatus(Long id, UpdateQuizStatusRequest request, Long currentUserId) {
		Quiz quiz = findQuiz(id);
		validateTransition(quiz, request.status());
		quiz.setStatus(request.status());
		quiz.setUpdatedAt(LocalDateTime.now());
		quiz.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_QUIZ_STATUS:" + request.status().name(), "quiz", quiz.getId());
		return toResponse(quiz);
	}

	@Transactional(readOnly = true)
	public List<QuizQuestionResponse> listQuestions(Long id) {
		ensureQuizExists(id);
		return quizQuestionRepository.findByIdQuizIdOrderBySortOrderAsc(id).stream()
				.map(quizMapper::toResponse)
				.toList();
	}

	@Transactional
	public List<QuizQuestionResponse> replaceQuestions(
			Long id,
			UpdateQuizQuestionsRequest request,
			Long currentUserId) {
		Quiz quiz = findQuiz(id);
		ensureDraft(quiz);
		validateQuestionItems(request.questions());
		Map<Long, Question> questions = loadQuestions(request.questions());
		quizQuestionRepository.deleteByIdQuizId(id);
		List<QuizQuestion> quizQuestions = request.questions().stream()
				.map(item -> createQuizQuestion(quiz, questions.get(item.questionId()), item))
				.toList();
		quizQuestionRepository.saveAll(quizQuestions);
		quiz.setUpdatedAt(LocalDateTime.now());
		quiz.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_QUIZ_QUESTIONS", "quiz", quiz.getId());
		return quizQuestions.stream()
				.sorted((left, right) -> left.getSortOrder().compareTo(right.getSortOrder()))
				.map(quizMapper::toResponse)
				.toList();
	}

	private QuizQuestion createQuizQuestion(Quiz quiz, Question question, QuizQuestionItemRequest item) {
		QuizQuestion quizQuestion = new QuizQuestion();
		quizQuestion.setId(new QuizQuestionId(quiz.getId(), question.getId()));
		quizQuestion.setQuiz(quiz);
		quizQuestion.setQuestion(question);
		quizQuestion.setSortOrder(item.sortOrder());
		quizQuestion.setPoints(item.points());
		return quizQuestion;
	}

	private Map<Long, Question> loadQuestions(List<QuizQuestionItemRequest> items) {
		Set<Long> questionIds = items.stream()
				.map(QuizQuestionItemRequest::questionId)
				.collect(Collectors.toSet());
		Map<Long, Question> questions = questionRepository.findAllById(questionIds).stream()
				.collect(Collectors.toMap(Question::getId, Function.identity()));
		if (questions.size() != questionIds.size()) {
			throw new NotFoundException("One or more questions were not found");
		}
		return questions;
	}

	private void validateQuestionItems(List<QuizQuestionItemRequest> items) {
		Set<Long> questionIds = new HashSet<>();
		Set<Integer> sortOrders = new HashSet<>();
		for (QuizQuestionItemRequest item : items) {
			if (!questionIds.add(item.questionId())) {
				throw new BadRequestException("DUPLICATE_QUIZ_QUESTION", "Duplicate question in quiz");
			}
			if (!sortOrders.add(item.sortOrder())) {
				throw new BadRequestException("DUPLICATE_QUIZ_QUESTION_SORT_ORDER", "Duplicate question sort order in quiz");
			}
		}
	}

	private void applyFields(Quiz quiz, CreateQuizRequest request) {
		quiz.setTitle(request.title().trim());
		quiz.setDescription(normalize(request.description()));
		quiz.setDurationMinutes(request.durationMinutes());
		quiz.setPassingScore(request.passingScore());
		quiz.setMaxAttempts(request.maxAttempts());
		quiz.setRandomize(request.randomize());
		quiz.setCategory(request.category().trim());
		quiz.setOpenDate(request.openDate());
		quiz.setCloseDate(request.closeDate());
	}

	private void applyFields(Quiz quiz, UpdateQuizRequest request) {
		quiz.setTitle(request.title().trim());
		quiz.setDescription(normalize(request.description()));
		quiz.setDurationMinutes(request.durationMinutes());
		quiz.setPassingScore(request.passingScore());
		quiz.setMaxAttempts(request.maxAttempts());
		quiz.setRandomize(request.randomize());
		quiz.setCategory(request.category().trim());
		quiz.setOpenDate(request.openDate());
		quiz.setCloseDate(request.closeDate());
	}

	private QuizResponse toResponse(Quiz quiz) {
		return quizMapper.toResponse(quiz, quizQuestionRepository.countByIdQuizId(quiz.getId()));
	}

	private Quiz findQuiz(Long id) {
		return quizRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Quiz not found"));
	}

	private void ensureQuizExists(Long id) {
		if (!quizRepository.existsById(id)) {
			throw new NotFoundException("Quiz not found");
		}
	}

	private void ensureDraft(Quiz quiz) {
		if (quiz.getStatus() != QuizStatus.Draft) {
			throw new ConflictException("QUIZ_NOT_EDITABLE", "Only draft quiz can be edited");
		}
	}

	private void validateTransition(Quiz quiz, QuizStatus target) {
		if (quiz.getStatus() == target) {
			return;
		}
		if (quiz.getStatus() == QuizStatus.Draft && target == QuizStatus.Published) {
			if (quizQuestionRepository.countByIdQuizId(quiz.getId()) == 0) {
				throw new ConflictException("QUIZ_QUESTION_REQUIRED", "Quiz requires at least one question before publishing");
			}
			return;
		}
		if (quiz.getStatus() == QuizStatus.Published && target == QuizStatus.Closed) {
			return;
		}
		throw new ConflictException("INVALID_QUIZ_STATUS_TRANSITION", "Invalid quiz status transition");
	}

	private void validateDateRange(LocalDate openDate, LocalDate closeDate) {
		if (openDate != null && closeDate != null && openDate.isAfter(closeDate)) {
			throw new BadRequestException("INVALID_QUIZ_DATE_RANGE", "Quiz open date must be before or equal to close date");
		}
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String enumName(Enum<?> value) {
		return value == null ? null : value.name();
	}
}
