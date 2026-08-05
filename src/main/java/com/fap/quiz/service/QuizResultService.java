package com.fap.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fap.clazz.service.ClassAccessService;
import com.fap.common.exception.ConflictException;
import com.fap.common.api.PageRequestFactory;
import com.fap.common.exception.ForbiddenException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.security.FapUserPrincipal;
import com.fap.quiz.dto.QuizAnswerItemRequest;
import com.fap.quiz.dto.QuizAttemptResultResponse;
import com.fap.quiz.dto.QuizAttemptReviewQuestionResponse;
import com.fap.quiz.dto.QuizAttemptReviewResponse;
import com.fap.quiz.dto.QuizAttemptSummaryResponse;
import com.fap.quiz.entity.Question;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.entity.QuizQuestion;
import com.fap.quiz.enums.QuizAttemptStatus;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.quiz.repository.QuizQuestionRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.training.enums.TrainingRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class QuizResultService {

	private static final String SUPER_ADMIN_ROLE = "Super Admin";
	private static final String CLASS_ADMIN_ROLE = "Class Admin";
	private static final String TRAINER_ROLE = "Trainer";
	private static final Collection<TrainingRegistrationStatus> ELIGIBLE_REGISTRATION_STATUSES = List.of(
			TrainingRegistrationStatus.Registered,
			TrainingRegistrationStatus.Completed);

	private final QuizRepository quizRepository;
	private final QuizAttemptRepository quizAttemptRepository;
	private final QuizQuestionRepository quizQuestionRepository;
	private final ClassAccessService classAccessService;
	private final ObjectMapper objectMapper;

	public QuizResultService(
			QuizRepository quizRepository,
			QuizAttemptRepository quizAttemptRepository,
			QuizQuestionRepository quizQuestionRepository,
			ClassAccessService classAccessService,
			ObjectMapper objectMapper) {
		this.quizRepository = quizRepository;
		this.quizAttemptRepository = quizAttemptRepository;
		this.quizQuestionRepository = quizQuestionRepository;
		this.classAccessService = classAccessService;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public Page<QuizAttemptResultResponse> listAttempts(
			Long quizId,
			QuizAttemptStatus status,
			Boolean passed,
			Long userId,
			Long classId,
			Long trainingSessionId,
			FapUserPrincipal principal,
			int page,
			int limit) {
		return listAttempts(quizId, status, passed, userId, classId, trainingSessionId, principal, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<QuizAttemptResultResponse> listAttempts(
			Long quizId,
			QuizAttemptStatus status,
			Boolean passed,
			Long userId,
			Long classId,
			Long trainingSessionId,
			FapUserPrincipal principal,
			int page,
			int limit,
			String sortBy,
			String order) {
		ensureQuizExists(quizId);
		assertCanViewResults(principal, classId, trainingSessionId);
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.DESC, "id"),
				"id", "startedAt", "submittedAt", "score", "passed", "status");
		return quizAttemptRepository.searchQuizResults(
						quizId,
						status,
						passed,
						userId,
						classId,
						trainingSessionId,
						isSuperAdmin(principal),
						principal.id(),
						ELIGIBLE_REGISTRATION_STATUSES,
						pageRequest)
				.map(this::toResultResponse);
	}

	@Transactional(readOnly = true)
	public QuizAttemptReviewResponse getAttemptDetail(Long quizId, Long attemptId, FapUserPrincipal principal) {
		assertCanViewResults(principal, null, null);
		QuizAttempt attempt = quizAttemptRepository.findByQuizIdAndId(quizId, attemptId)
				.orElseThrow(() -> new NotFoundException("Quiz attempt not found"));
		assertCanViewAttempt(principal, quizId, attemptId);
		if (attempt.getStatus() != QuizAttemptStatus.Submitted) {
			throw new ConflictException("QUIZ_ATTEMPT_REVIEW_UNAVAILABLE", "Only submitted attempt can be reviewed");
		}
		List<QuizQuestion> questions = quizQuestionRepository.findByIdQuizIdOrderBySortOrderAsc(quizId);
		return toReviewResponse(attempt, questions);
	}

	@Transactional(readOnly = true)
	public QuizAttemptSummaryResponse summary(
			Long quizId,
			Long classId,
			Long trainingSessionId,
			FapUserPrincipal principal) {
		Quiz quiz = findQuiz(quizId);
		assertCanViewResults(principal, classId, trainingSessionId);
		List<QuizAttempt> attempts = quizAttemptRepository.searchQuizResults(
						quizId,
						null,
						null,
						null,
						classId,
						trainingSessionId,
						isSuperAdmin(principal),
						principal.id(),
						ELIGIBLE_REGISTRATION_STATUSES,
						Pageable.unpaged())
				.getContent();
		return summarize(quiz, attempts);
	}

	private QuizAttemptResultResponse toResultResponse(QuizAttempt attempt) {
		return new QuizAttemptResultResponse(
				attempt.getId(),
				attempt.getQuiz().getId(),
				attempt.getQuiz().getTitle(),
				attempt.getUser().getId(),
				attempt.getUser().getFullName(),
				attempt.getUser().getEmail(),
				attempt.getAttemptNumber(),
				attempt.getStatus(),
				attempt.getScore(),
				attempt.getCorrectCount(),
				attempt.getTotalQuestions(),
				attempt.getPassed(),
				attempt.getTimeTakenSeconds(),
				attempt.getStartedAt(),
				attempt.getSubmittedAt());
	}

	private QuizAttemptSummaryResponse summarize(Quiz quiz, List<QuizAttempt> attempts) {
		long submittedAttempts = attempts.stream()
				.filter(attempt -> attempt.getStatus() == QuizAttemptStatus.Submitted)
				.count();
		long inProgressAttempts = attempts.stream()
				.filter(attempt -> attempt.getStatus() == QuizAttemptStatus.InProgress)
				.count();
		long passedAttempts = attempts.stream()
				.filter(attempt -> Boolean.TRUE.equals(attempt.getPassed()))
				.count();
		long failedAttempts = submittedAttempts - passedAttempts;
		List<Integer> scores = attempts.stream()
				.filter(attempt -> attempt.getStatus() == QuizAttemptStatus.Submitted)
				.map(QuizAttempt::getScore)
				.filter(score -> score != null)
				.toList();
		Double averageScore = scores.isEmpty()
				? null
				: scores.stream().mapToInt(Integer::intValue).average().orElse(0);
		Integer highestScore = scores.stream().max(Integer::compareTo).orElse(null);
		Integer lowestScore = scores.stream().min(Integer::compareTo).orElse(null);
		double passRate = submittedAttempts == 0
				? 0
				: BigDecimal.valueOf(passedAttempts)
						.multiply(BigDecimal.valueOf(100))
						.divide(BigDecimal.valueOf(submittedAttempts), 2, RoundingMode.HALF_UP)
						.doubleValue();
		return new QuizAttemptSummaryResponse(
				quiz.getId(),
				quiz.getTitle(),
				attempts.size(),
				inProgressAttempts,
				submittedAttempts,
				passedAttempts,
				failedAttempts,
				passRate,
				averageScore,
				highestScore,
				lowestScore);
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
		JsonNode correctAnswers = readJson(question.getCorrectAnswersJson());
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

	private void assertCanViewResults(FapUserPrincipal principal, Long classId, Long trainingSessionId) {
		if (!isResultViewer(principal)) {
			throw new ForbiddenException("You cannot view quiz results");
		}
		if (classId != null) {
			classAccessService.assertCanViewClass(principal, classId);
		}
		if (trainingSessionId != null) {
			classAccessService.assertCanViewSession(principal, trainingSessionId);
		}
	}

	private void assertCanViewAttempt(FapUserPrincipal principal, Long quizId, Long attemptId) {
		if (quizAttemptRepository.countVisibleQuizResult(
				quizId,
				attemptId,
				isSuperAdmin(principal),
				principal.id(),
				ELIGIBLE_REGISTRATION_STATUSES) == 0) {
			throw new ForbiddenException("You cannot view this quiz attempt");
		}
	}

	private Quiz findQuiz(Long quizId) {
		return quizRepository.findById(quizId)
				.orElseThrow(() -> new NotFoundException("Quiz not found"));
	}

	private void ensureQuizExists(Long quizId) {
		if (!quizRepository.existsById(quizId)) {
			throw new NotFoundException("Quiz not found");
		}
	}

	private boolean isResultViewer(FapUserPrincipal principal) {
		return principal.roles().contains(SUPER_ADMIN_ROLE)
				|| principal.roles().contains(CLASS_ADMIN_ROLE)
				|| principal.roles().contains(TRAINER_ROLE);
	}

	private boolean isSuperAdmin(FapUserPrincipal principal) {
		return principal.roles().contains(SUPER_ADMIN_ROLE);
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

	private JsonNode readJson(String value) {
		try {
			return objectMapper.readTree(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Stored quiz JSON is invalid", exception);
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
}
