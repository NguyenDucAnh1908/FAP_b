package com.fap.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fap.common.audit.AuditLogService;
import com.fap.common.metrics.DomainMetrics;
import com.fap.common.exception.BadRequestException;
import com.fap.common.api.PageRequestFactory;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.quiz.dto.AssignedQuizResponse;
import com.fap.quiz.dto.QuizAnswerItemRequest;
import com.fap.quiz.dto.QuizAttemptReviewQuestionResponse;
import com.fap.quiz.dto.QuizAttemptReviewResponse;
import com.fap.quiz.dto.QuizAttemptResponse;
import com.fap.quiz.dto.SaveQuizAnswersRequest;
import com.fap.quiz.entity.Question;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.entity.QuizQuestion;
import com.fap.quiz.enums.QuizAttemptStatus;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.mapper.QuizAttemptMapper;
import com.fap.quiz.repository.QuizAssignmentRepository;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.quiz.repository.QuizQuestionRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizAttemptService {

	private static final String EMPTY_ANSWERS_JSON = "[]";
	private static final Collection<TrainingRegistrationStatus> ELIGIBLE_REGISTRATION_STATUSES = List.of(
			TrainingRegistrationStatus.Registered,
			TrainingRegistrationStatus.Completed);

	private final QuizRepository quizRepository;
	private final QuizAssignmentRepository quizAssignmentRepository;
	private final QuizQuestionRepository quizQuestionRepository;
	private final QuizAttemptRepository quizAttemptRepository;
	private final UserRepository userRepository;
	private final QuizAttemptMapper quizAttemptMapper;
	private final ObjectMapper objectMapper;
	private final AuditLogService auditLogService;
	private final DomainMetrics domainMetrics;

	public QuizAttemptService(
			QuizRepository quizRepository,
			QuizAssignmentRepository quizAssignmentRepository,
			QuizQuestionRepository quizQuestionRepository,
			QuizAttemptRepository quizAttemptRepository,
			UserRepository userRepository,
			QuizAttemptMapper quizAttemptMapper,
			ObjectMapper objectMapper,
			AuditLogService auditLogService,
			DomainMetrics domainMetrics) {
		this.quizRepository = quizRepository;
		this.quizAssignmentRepository = quizAssignmentRepository;
		this.quizQuestionRepository = quizQuestionRepository;
		this.quizAttemptRepository = quizAttemptRepository;
		this.userRepository = userRepository;
		this.quizAttemptMapper = quizAttemptMapper;
		this.objectMapper = objectMapper;
		this.auditLogService = auditLogService;
		this.domainMetrics = domainMetrics;
	}

	@Transactional(readOnly = true)
	public Page<AssignedQuizResponse> assigned(Long currentUserId, int page, int limit) {
		return assigned(currentUserId, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<AssignedQuizResponse> assigned(
			Long currentUserId,
			int page,
			int limit,
			String sortBy,
			String order) {
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.ASC, "closeDate").and(Sort.by(Sort.Direction.DESC, "id")),
				"id", "closeDate", "openDate", "title", "durationMinutes", "status");
		return quizRepository.searchAssignedToUser(
						currentUserId,
						QuizStatus.Published,
						ELIGIBLE_REGISTRATION_STATUSES,
						LocalDate.now(),
						pageRequest)
				.map(quiz -> {
					long attemptCount = quizAttemptRepository.countByQuizIdAndUserId(quiz.getId(), currentUserId);
					QuizAttempt latestAttempt = quizAttemptRepository
							.findFirstByQuizIdAndUserIdOrderByIdDesc(quiz.getId(), currentUserId)
							.orElse(null);
					return quizAttemptMapper.toAssignedResponse(
							quiz,
							quizQuestionRepository.countByIdQuizId(quiz.getId()),
							attemptCount,
							latestAttempt);
				});
	}

	@Transactional
	public QuizAttemptResponse start(Long quizId, Long currentUserId) {
		Quiz quiz = findQuiz(quizId);
		ensureAvailable(quiz);
		ensureAssignedToUser(quizId, currentUserId);
		ensureNoInProgressAttempt(quizId, currentUserId);
		long attemptCount = quizAttemptRepository.countByQuizIdAndUserId(quizId, currentUserId);
		if (attemptCount >= quiz.getMaxAttempts()) {
			throw new ConflictException("QUIZ_ATTEMPT_LIMIT_REACHED", "Quiz attempt limit reached");
		}

		QuizAttempt attempt = new QuizAttempt();
		attempt.setQuiz(quiz);
		attempt.setUser(userRepository.getReferenceById(currentUserId));
		attempt.setAttemptNumber((int) attemptCount + 1);
		attempt.setStatus(QuizAttemptStatus.InProgress);
		attempt.setAnswersJson(EMPTY_ANSWERS_JSON);
		attempt.setStartedAt(LocalDateTime.now());
		QuizAttempt saved = quizAttemptRepository.save(attempt);
		auditLogService.record("START_QUIZ_ATTEMPT", "quiz_attempt", saved.getId());
		return toResponse(saved);
	}

	@Transactional
	public QuizAttemptResponse saveAnswers(Long attemptId, SaveQuizAnswersRequest request, Long currentUserId) {
		QuizAttempt attempt = findOwnAttempt(attemptId, currentUserId);
		ensureInProgress(attempt);
		List<QuizQuestion> quizQuestions = quizQuestions(attempt);
		if (autoSubmitIfExpired(attempt, quizQuestions)) {
			return quizAttemptMapper.toResponse(attempt, orderedQuestions(attempt, quizQuestions));
		}
		validateAnswers(request.answers(), quizQuestions);
		attempt.setAnswersJson(writeAnswers(request.answers()));
		auditLogService.record("SAVE_QUIZ_ATTEMPT_ANSWERS", "quiz_attempt", attempt.getId());
		return quizAttemptMapper.toResponse(attempt, orderedQuestions(attempt, quizQuestions));
	}

	@Transactional
	public QuizAttemptResponse submit(Long attemptId, Long currentUserId) {
		return domainMetrics.recordQuizSubmit(() -> {
			QuizAttempt attempt = findOwnAttempt(attemptId, currentUserId);
			ensureInProgress(attempt);
			List<QuizQuestion> quizQuestions = quizQuestions(attempt);
			if (autoSubmitIfExpired(attempt, quizQuestions)) {
				return quizAttemptMapper.toResponse(attempt, orderedQuestions(attempt, quizQuestions));
			}
			submitAttempt(attempt, quizQuestions, LocalDateTime.now(), "SUBMIT_QUIZ_ATTEMPT");
			return quizAttemptMapper.toResponse(attempt, orderedQuestions(attempt, quizQuestions));
		});
	}

	@Transactional
	public QuizAttemptResponse get(Long attemptId, Long currentUserId) {
		QuizAttempt attempt = findOwnAttempt(attemptId, currentUserId);
		autoSubmitIfExpired(attempt, quizQuestions(attempt));
		return toResponse(attempt);
	}

	@Transactional
	public QuizAttemptReviewResponse review(Long attemptId, Long currentUserId) {
		QuizAttempt attempt = findOwnAttempt(attemptId, currentUserId);
		List<QuizQuestion> quizQuestions = quizQuestions(attempt);
		autoSubmitIfExpired(attempt, quizQuestions);
		if (attempt.getStatus() != QuizAttemptStatus.Submitted) {
			throw new ConflictException("QUIZ_ATTEMPT_REVIEW_UNAVAILABLE", "Only submitted attempt can be reviewed");
		}
		return toReviewResponse(attempt, orderedQuestions(attempt, quizQuestions));
	}

	private QuizAttemptResponse toResponse(QuizAttempt attempt) {
		List<QuizQuestion> questions = orderedQuestions(attempt, quizQuestions(attempt));
		return quizAttemptMapper.toResponse(attempt, questions);
	}

	private Quiz findQuiz(Long quizId) {
		return quizRepository.findById(quizId)
				.orElseThrow(() -> new NotFoundException("Quiz not found"));
	}

	private QuizAttempt findOwnAttempt(Long attemptId, Long currentUserId) {
		return quizAttemptRepository.findByIdAndUserId(attemptId, currentUserId)
				.orElseThrow(() -> new NotFoundException("Quiz attempt not found"));
	}

	private List<QuizQuestion> quizQuestions(QuizAttempt attempt) {
		List<QuizQuestion> questions = quizQuestionRepository.findByIdQuizIdOrderBySortOrderAsc(attempt.getQuiz().getId());
		if (questions.isEmpty()) {
			throw new ConflictException("QUIZ_QUESTION_REQUIRED", "Quiz requires at least one question");
		}
		return questions;
	}

	private List<QuizQuestion> orderedQuestions(QuizAttempt attempt, List<QuizQuestion> questions) {
		List<QuizQuestion> orderedQuestions = new ArrayList<>(questions);
		if (attempt.getQuiz().isRandomize()) {
			Collections.shuffle(orderedQuestions, new Random(attempt.getId()));
		}
		return orderedQuestions;
	}

	private void ensureAvailable(Quiz quiz) {
		if (quiz.getStatus() != QuizStatus.Published) {
			throw new ConflictException("QUIZ_NOT_AVAILABLE", "Only published quiz can be attempted");
		}
		LocalDate today = LocalDate.now();
		if (quiz.getOpenDate() != null && today.isBefore(quiz.getOpenDate())) {
			throw new ConflictException("QUIZ_NOT_OPEN", "Quiz is not open yet");
		}
		if (quiz.getCloseDate() != null && today.isAfter(quiz.getCloseDate())) {
			throw new ConflictException("QUIZ_CLOSED", "Quiz is already closed");
		}
	}

	private void ensureAssignedToUser(Long quizId, Long currentUserId) {
		long eligibleAssignments = quizAssignmentRepository.countEligibleAssignments(
				quizId,
				currentUserId,
				ELIGIBLE_REGISTRATION_STATUSES);
		if (eligibleAssignments == 0) {
			throw new ConflictException("QUIZ_ASSIGNMENT_REQUIRED", "Quiz is not assigned to the current user");
		}
	}

	private void ensureNoInProgressAttempt(Long quizId, Long currentUserId) {
		if (quizAttemptRepository.countByQuizIdAndUserIdAndStatus(
				quizId,
				currentUserId,
				QuizAttemptStatus.InProgress) > 0) {
			throw new ConflictException("QUIZ_ATTEMPT_IN_PROGRESS", "Finish the in-progress attempt before starting a new one");
		}
	}

	private void ensureInProgress(QuizAttempt attempt) {
		if (attempt.getStatus() != QuizAttemptStatus.InProgress) {
			throw new ConflictException("QUIZ_ATTEMPT_NOT_EDITABLE", "Only in-progress attempt can be changed");
		}
	}

	private boolean autoSubmitIfExpired(QuizAttempt attempt, List<QuizQuestion> quizQuestions) {
		if (attempt.getStatus() != QuizAttemptStatus.InProgress || !isExpired(attempt, LocalDateTime.now())) {
			return false;
		}
		submitAttempt(attempt, quizQuestions, expiresAt(attempt), "AUTO_SUBMIT_QUIZ_ATTEMPT_EXPIRED");
		return true;
	}

	private void submitAttempt(
			QuizAttempt attempt,
			List<QuizQuestion> quizQuestions,
			LocalDateTime submittedAt,
			String auditAction) {
		Map<Long, JsonNode> answers = readAnswers(attempt.getAnswersJson());
		GradeResult gradeResult = grade(quizQuestions, answers);
		attempt.setStatus(QuizAttemptStatus.Submitted);
		attempt.setScore(gradeResult.score());
		attempt.setCorrectCount(gradeResult.correctCount());
		attempt.setTotalQuestions(gradeResult.totalQuestions());
		attempt.setPassed(gradeResult.score() >= attempt.getQuiz().getPassingScore());
		attempt.setTimeTakenSeconds(timeTakenSeconds(attempt.getStartedAt(), submittedAt));
		attempt.setSubmittedAt(submittedAt);
		auditLogService.record(auditAction, "quiz_attempt", attempt.getId());
	}

	private boolean isExpired(QuizAttempt attempt, LocalDateTime now) {
		return now.isAfter(expiresAt(attempt));
	}

	private LocalDateTime expiresAt(QuizAttempt attempt) {
		return attempt.getStartedAt().plusMinutes(attempt.getQuiz().getDurationMinutes());
	}

	private void validateAnswers(List<QuizAnswerItemRequest> answers, List<QuizQuestion> quizQuestions) {
		Set<Long> quizQuestionIds = quizQuestions.stream()
				.map(quizQuestion -> quizQuestion.getQuestion().getId())
				.collect(Collectors.toSet());
		Set<Long> answerQuestionIds = new HashSet<>();
		for (QuizAnswerItemRequest answer : answers) {
			if (!answerQuestionIds.add(answer.questionId())) {
				throw new BadRequestException("DUPLICATE_QUIZ_ANSWER", "Duplicate answer for quiz question");
			}
			if (!quizQuestionIds.contains(answer.questionId())) {
				throw new BadRequestException("QUIZ_ANSWER_QUESTION_NOT_FOUND", "Answer references a question outside this quiz");
			}
			if (!answer.selectedAnswersJson().isArray()) {
				throw new BadRequestException("QUIZ_ANSWER_ARRAY_REQUIRED", "Selected answers must be a JSON array");
			}
		}
	}

	private String writeAnswers(List<QuizAnswerItemRequest> answers) {
		try {
			return objectMapper.writeValueAsString(answers);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Could not serialize quiz answers", exception);
		}
	}

	private Map<Long, JsonNode> readAnswers(String answersJson) {
		try {
			List<QuizAnswerItemRequest> items = objectMapper.readValue(
					answersJson,
					new TypeReference<>() {
					});
			Map<Long, JsonNode> answers = new HashMap<>();
			for (QuizAnswerItemRequest item : items) {
				answers.put(item.questionId(), item.selectedAnswersJson());
			}
			return answers;
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Stored quiz attempt answers are invalid", exception);
		}
	}

	private GradeResult grade(List<QuizQuestion> quizQuestions, Map<Long, JsonNode> answers) {
		BigDecimal earnedPoints = BigDecimal.ZERO;
		BigDecimal totalPoints = BigDecimal.ZERO;
		int correctCount = 0;
		for (QuizQuestion quizQuestion : quizQuestions) {
			Question question = quizQuestion.getQuestion();
			totalPoints = totalPoints.add(quizQuestion.getPoints());
			JsonNode selectedAnswers = answers.get(question.getId());
			if (sameAnswers(selectedAnswers, readQuestionAnswers(question))) {
				earnedPoints = earnedPoints.add(quizQuestion.getPoints());
				correctCount++;
			}
		}
		int score = earnedPoints
				.multiply(BigDecimal.valueOf(100))
				.divide(totalPoints, 0, RoundingMode.HALF_UP)
				.intValue();
		return new GradeResult(score, correctCount, quizQuestions.size());
	}

	private QuizAttemptReviewResponse toReviewResponse(QuizAttempt attempt, List<QuizQuestion> quizQuestions) {
		Map<Long, JsonNode> answers = readAnswers(attempt.getAnswersJson());
		return new QuizAttemptReviewResponse(
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
				quizQuestions.stream()
						.map(quizQuestion -> toReviewQuestionResponse(quizQuestion, answers))
						.toList());
	}

	private QuizAttemptReviewQuestionResponse toReviewQuestionResponse(
			QuizQuestion quizQuestion,
			Map<Long, JsonNode> answers) {
		Question question = quizQuestion.getQuestion();
		JsonNode selectedAnswers = answers.getOrDefault(question.getId(), objectMapper.createArrayNode());
		JsonNode correctAnswers = readQuestionAnswers(question);
		return new QuizAttemptReviewQuestionResponse(
				question.getId(),
				quizQuestion.getSortOrder(),
				quizQuestion.getPoints(),
				question.getContent(),
				question.getQuestionType(),
				question.getCategory(),
				question.getDifficulty(),
				readJson(question.getOptionsJson()),
				selectedAnswers,
				correctAnswers,
				sameAnswers(selectedAnswers, correctAnswers),
				question.getExplanation());
	}

	private JsonNode readJson(String value) {
		try {
			return objectMapper.readTree(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Stored quiz JSON is invalid", exception);
		}
	}

	private JsonNode readQuestionAnswers(Question question) {
		try {
			return objectMapper.readTree(question.getCorrectAnswersJson());
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Stored question answers are invalid", exception);
		}
	}

	private boolean sameAnswers(JsonNode selectedAnswers, JsonNode correctAnswers) {
		return comparableSet(selectedAnswers).equals(comparableSet(correctAnswers));
	}

	private Set<String> comparableSet(JsonNode answers) {
		if (answers == null || answers.isNull()) {
			return Set.of();
		}
		if (!answers.isArray()) {
			return Set.of(answers.isTextual() ? answers.asText() : answers.toString());
		}
		Set<String> values = new HashSet<>();
		for (JsonNode answer : answers) {
			values.add(answer.isTextual() ? answer.asText() : answer.toString());
		}
		return values;
	}

	private int timeTakenSeconds(LocalDateTime startedAt, LocalDateTime submittedAt) {
		long seconds = Duration.between(startedAt, submittedAt).getSeconds();
		return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max((int) seconds, 0);
	}

	private record GradeResult(
			int score,
			int correctCount,
			int totalQuestions
	) {
	}
}
