package com.fap.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.metrics.DomainMetrics;
import com.fap.quiz.dto.QuizAnswerItemRequest;
import com.fap.quiz.dto.QuizAttemptResponse;
import com.fap.quiz.dto.QuizAttemptReviewQuestionResponse;
import com.fap.quiz.dto.QuizAttemptReviewResponse;
import com.fap.quiz.dto.SaveQuizAnswersRequest;
import com.fap.quiz.entity.Question;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.entity.QuizQuestion;
import com.fap.quiz.entity.QuizQuestionId;
import com.fap.quiz.enums.QuestionDifficulty;
import com.fap.quiz.enums.QuestionType;
import com.fap.quiz.enums.QuizAttemptStatus;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.mapper.QuizAttemptMapper;
import com.fap.quiz.repository.QuizAssignmentRepository;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.quiz.repository.QuizQuestionRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.user.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * What happens to a trainee's attempt when an admin sets the quiz to {@code Closed} while it is
 * running: the attempt is graded with the answers saved so far, and a save or submit arriving
 * afterwards is refused with {@code QUIZ_CLOSED} so the client learns its answers were not kept.
 * A passed {@code closeDate} alone, by contrast, only blocks new attempts: whoever started in time
 * may finish within the attempt's own duration. Expiry rules themselves live in
 * {@link QuizAttemptServiceTest} and {@link QuizAttemptServiceRulesTest}.
 */
class QuizAttemptClosedQuizTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 9, 0);
	private static final LocalDate TODAY = NOW.toLocalDate();
	private static final long QUIZ_ID = 31L;
	private static final long FIRST_QUESTION_ID = 501L;
	private static final long SECOND_QUESTION_ID = 502L;
	private static final long ATTEMPT_ID = 900L;
	private static final long CURRENT_USER_ID = 7L;
	private static final int DURATION_MINUTES = 30;
	/** One of the two one-point questions answered correctly, so a regrade with new answers would show. */
	private static final String STORED_ANSWERS = storedAnswers(answer(FIRST_QUESTION_ID, "[\"A\"]"));
	private static final int STORED_SCORE = 50;

	private final QuizRepository quizRepository = mock(QuizRepository.class);
	private final QuizAssignmentRepository quizAssignmentRepository = mock(QuizAssignmentRepository.class);
	private final QuizQuestionRepository quizQuestionRepository = mock(QuizQuestionRepository.class);
	private final QuizAttemptRepository quizAttemptRepository = mock(QuizAttemptRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final QuizAttemptService service =
			serviceWith(Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

	private Quiz quiz;

	@BeforeEach
	void setUp() {
		doCallRealMethod().when(quizAttemptRepository).getByIdAndUserIdOrThrow(any(), any());
		quiz = new Quiz();
		quiz.setId(QUIZ_ID);
		quiz.setTitle("Java basics");
		quiz.setStatus(QuizStatus.Published);
		quiz.setDurationMinutes(DURATION_MINUTES);
		quiz.setPassingScore(50);
		quiz.setMaxAttempts(2);
		when(quizQuestionRepository.findByIdQuizIdOrderBySortOrderAsc(QUIZ_ID)).thenReturn(List.of(
				quizQuestion(FIRST_QUESTION_ID, 1, "[\"A\"]"),
				quizQuestion(SECOND_QUESTION_ID, 2, "[\"B\"]")));
	}

	@Test
	void saveAnswersOnAClosedQuizGradesTheStoredAnswersAndRefusesTheNewOnes() {
		givenClosedQuiz();
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), STORED_ANSWERS);

		assertThatThrownBy(() -> service.saveAnswers(ATTEMPT_ID, answersWorthFullMarks(), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Quiz is already closed")
				.extracting("code")
				.isEqualTo("QUIZ_CLOSED");

		assertThat(attempt.getAnswersJson()).isEqualTo(STORED_ANSWERS);
		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(attempt.getScore()).isEqualTo(STORED_SCORE);
		assertThat(attempt.getCorrectCount()).isEqualTo(1);
		assertThat(attempt.getTotalQuestions()).isEqualTo(2);
		assertThat(attempt.getPassed()).isTrue();
		assertThat(attempt.getSubmittedAt()).isEqualTo(NOW);
		assertThat(attempt.getTimeTakenSeconds()).isEqualTo(300);
		verify(auditLogService).record("AUTO_SUBMIT_QUIZ_CLOSED", "quiz_attempt", ATTEMPT_ID);
		verify(auditLogService, never()).record(eq("SAVE_QUIZ_ATTEMPT_ANSWERS"), anyString(), anyLong());
	}

	@Test
	void submitOnAClosedQuizIsRefusedAfterTheAutoSubmit() {
		givenClosedQuiz();
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), STORED_ANSWERS);

		assertThatThrownBy(() -> service.submit(ATTEMPT_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Quiz is already closed")
				.extracting("code")
				.isEqualTo("QUIZ_CLOSED");

		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(attempt.getScore()).isEqualTo(STORED_SCORE);
		assertThat(attempt.getSubmittedAt()).isEqualTo(NOW);
		verify(auditLogService).record("AUTO_SUBMIT_QUIZ_CLOSED", "quiz_attempt", ATTEMPT_ID);
		verify(auditLogService, never()).record(eq("SUBMIT_QUIZ_ATTEMPT"), anyString(), anyLong());
	}

	@Test
	void getOnAClosedQuizReturnsTheAutoSubmittedAttempt() {
		givenClosedQuiz();
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), STORED_ANSWERS);

		QuizAttemptResponse response = service.get(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(response.status()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(response.score()).isEqualTo(STORED_SCORE);
		assertThat(response.submittedAt()).isEqualTo(NOW);
		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.Submitted);
		verify(auditLogService).record("AUTO_SUBMIT_QUIZ_CLOSED", "quiz_attempt", ATTEMPT_ID);
	}

	@Test
	void reviewOnAClosedQuizReturnsTheAutoSubmittedAttempt() {
		givenClosedQuiz();
		givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), STORED_ANSWERS);

		QuizAttemptReviewResponse review = service.review(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(review.status()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(review.score()).isEqualTo(STORED_SCORE);
		assertThat(review.questions()).extracting(QuizAttemptReviewQuestionResponse::correct)
				.containsExactly(true, false);
		verify(auditLogService).record("AUTO_SUBMIT_QUIZ_CLOSED", "quiz_attempt", ATTEMPT_ID);
	}

	/** Every submission audits once, so one audit row means the attempt was graded once. */
	@Test
	void aClosedQuizEndsTheAttemptOnlyOnceAcrossReads() {
		givenClosedQuiz();
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), STORED_ANSWERS);

		service.get(ATTEMPT_ID, CURRENT_USER_ID);
		service.review(ATTEMPT_ID, CURRENT_USER_ID);
		service.get(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getScore()).isEqualTo(STORED_SCORE);
		verify(auditLogService, times(1)).record(anyString(), anyString(), anyLong());
		verify(auditLogService).record("AUTO_SUBMIT_QUIZ_CLOSED", "quiz_attempt", ATTEMPT_ID);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("allCalls")
	void anExpiredAttemptOfAClosedQuizTakesTheExpiryPathOnce(String call, Consumer<QuizAttemptService> invoke) {
		givenClosedQuiz();
		LocalDateTime startedAt = NOW.minusMinutes(DURATION_MINUTES + 1);
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, startedAt, STORED_ANSWERS);

		invoke.accept(service);

		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(attempt.getScore()).isEqualTo(STORED_SCORE);
		assertThat(attempt.getSubmittedAt()).isEqualTo(startedAt.plusMinutes(DURATION_MINUTES));
		assertThat(attempt.getTimeTakenSeconds()).isEqualTo(DURATION_MINUTES * 60);
		verify(auditLogService, times(1)).record(anyString(), anyString(), anyLong());
		verify(auditLogService).record("AUTO_SUBMIT_QUIZ_ATTEMPT_EXPIRED", "quiz_attempt", ATTEMPT_ID);
	}

	/**
	 * Started at 23:50 on the quiz's last day: ten minutes past midnight the date is after
	 * {@code closeDate}, yet the attempt is only twenty minutes into its thirty and may be finished.
	 */
	@Test
	void anAttemptStartedBeforeTheCloseDateMayStillBeSavedAndSubmittedAfterIt() {
		LocalDateTime justAfterMidnight = TODAY.atTime(0, 10);
		QuizAttemptService lateService =
				serviceWith(Clock.fixed(justAfterMidnight.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
		quiz.setCloseDate(TODAY.minusDays(1));
		QuizAttempt attempt = givenAttempt(
				QuizAttemptStatus.InProgress, justAfterMidnight.minusMinutes(20), STORED_ANSWERS);

		QuizAttemptResponse saved = lateService.saveAnswers(ATTEMPT_ID, answersWorthFullMarks(), CURRENT_USER_ID);
		QuizAttemptResponse submitted = lateService.submit(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(saved.status()).isEqualTo(QuizAttemptStatus.InProgress);
		assertThat(json(attempt.getAnswersJson())).isEqualTo(json(storedAnswers(
				answer(FIRST_QUESTION_ID, "[\"A\"]"), answer(SECOND_QUESTION_ID, "[\"B\"]"))));
		assertThat(submitted.status()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(attempt.getScore()).isEqualTo(100);
		assertThat(attempt.getSubmittedAt()).isEqualTo(justAfterMidnight);
		assertThat(attempt.getTimeTakenSeconds()).isEqualTo(20 * 60);
		verify(auditLogService).record("SAVE_QUIZ_ATTEMPT_ANSWERS", "quiz_attempt", ATTEMPT_ID);
		verify(auditLogService).record("SUBMIT_QUIZ_ATTEMPT", "quiz_attempt", ATTEMPT_ID);
		verify(auditLogService, never()).record(eq("AUTO_SUBMIT_QUIZ_CLOSED"), anyString(), anyLong());
	}

	@Test
	void getKeepsTheGradeOfAnAttemptSubmittedBeforeTheQuizWasClosed() {
		givenClosedQuiz();
		QuizAttempt attempt = givenSubmittedAttempt(40);

		QuizAttemptResponse response = service.get(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(response.status()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(response.score()).isEqualTo(40);
		assertThat(attempt.getScore()).isEqualTo(40);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("writeCalls")
	void aSubmittedAttemptOfAClosedQuizIsStillReportedAsNotEditable(
			String call,
			Consumer<QuizAttemptService> invoke) {
		givenClosedQuiz();
		QuizAttempt attempt = givenSubmittedAttempt(40);

		assertThatThrownBy(() -> invoke.accept(service))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Only in-progress attempt can be changed")
				.extracting("code")
				.isEqualTo("QUIZ_ATTEMPT_NOT_EDITABLE");

		assertThat(attempt.getScore()).isEqualTo(40);
		assertThat(attempt.getAnswersJson()).isEqualTo(STORED_ANSWERS);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	static Stream<Arguments> writeCalls() {
		return Stream.of(
				call("saveAnswers", target ->
						target.saveAnswers(ATTEMPT_ID, new SaveQuizAnswersRequest(List.of()), CURRENT_USER_ID)),
				call("submit", target -> target.submit(ATTEMPT_ID, CURRENT_USER_ID)));
	}

	static Stream<Arguments> allCalls() {
		return Stream.concat(writeCalls(), Stream.of(
				call("get", target -> target.get(ATTEMPT_ID, CURRENT_USER_ID)),
				call("review", target -> target.review(ATTEMPT_ID, CURRENT_USER_ID))));
	}

	private static Arguments call(String name, Consumer<QuizAttemptService> invoke) {
		return Arguments.of(name, invoke);
	}

	private QuizAttemptService serviceWith(Clock clock) {
		return new QuizAttemptService(
				quizRepository,
				quizAssignmentRepository,
				quizQuestionRepository,
				quizAttemptRepository,
				userRepository,
				new QuizAttemptMapper(objectMapper),
				objectMapper,
				auditLogService,
				new DomainMetrics(new SimpleMeterRegistry()),
				clock);
	}

	/** Closed by an admin ahead of its close date, so only the status can be what ends the attempt. */
	private void givenClosedQuiz() {
		quiz.setStatus(QuizStatus.Closed);
		quiz.setCloseDate(TODAY.plusDays(3));
	}

	private QuizAttempt givenAttempt(QuizAttemptStatus status, LocalDateTime startedAt, String answersJson) {
		QuizAttempt attempt = new QuizAttempt();
		attempt.setId(ATTEMPT_ID);
		attempt.setQuiz(quiz);
		attempt.setAttemptNumber(1);
		attempt.setStatus(status);
		attempt.setAnswersJson(answersJson);
		attempt.setStartedAt(startedAt);
		when(quizAttemptRepository.findByIdAndUserId(ATTEMPT_ID, CURRENT_USER_ID)).thenReturn(Optional.of(attempt));
		return attempt;
	}

	/** Submitted the day before, with a grade that differs from what a regrade of the stored answers gives. */
	private QuizAttempt givenSubmittedAttempt(int score) {
		LocalDateTime startedAt = NOW.minusDays(1);
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.Submitted, startedAt, STORED_ANSWERS);
		attempt.setScore(score);
		attempt.setPassed(score >= quiz.getPassingScore());
		attempt.setSubmittedAt(startedAt.plusMinutes(10));
		return attempt;
	}

	private QuizQuestion quizQuestion(long questionId, int sortOrder, String correctAnswersJson) {
		Question question = new Question();
		question.setId(questionId);
		question.setContent("Question " + questionId);
		question.setQuestionType(QuestionType.multiple);
		question.setCategory("java");
		question.setDifficulty(QuestionDifficulty.Medium);
		question.setOptionsJson("[\"A\",\"B\",\"C\"]");
		question.setCorrectAnswersJson(correctAnswersJson);
		question.setExplanation("Because of " + questionId);
		QuizQuestion quizQuestion = new QuizQuestion();
		quizQuestion.setId(new QuizQuestionId(QUIZ_ID, questionId));
		quizQuestion.setQuiz(quiz);
		quizQuestion.setQuestion(question);
		quizQuestion.setSortOrder(sortOrder);
		quizQuestion.setPoints(BigDecimal.ONE);
		return quizQuestion;
	}

	/** Both questions answered correctly: kept, these would raise the grade from 50 to 100. */
	private SaveQuizAnswersRequest answersWorthFullMarks() {
		return new SaveQuizAnswersRequest(List.of(
				new QuizAnswerItemRequest(FIRST_QUESTION_ID, json("[\"A\"]")),
				new QuizAnswerItemRequest(SECOND_QUESTION_ID, json("[\"B\"]"))));
	}

	/** One stored answer item, in the shape {@code saveAnswers} writes. */
	private static String answer(long questionId, String selectedJson) {
		return "{\"questionId\":" + questionId + ",\"selectedAnswersJson\":" + selectedJson + "}";
	}

	private static String storedAnswers(String... items) {
		return "[" + String.join(",", items) + "]";
	}

	private JsonNode json(String value) {
		try {
			return objectMapper.readTree(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Invalid test JSON: " + value, exception);
		}
	}
}
