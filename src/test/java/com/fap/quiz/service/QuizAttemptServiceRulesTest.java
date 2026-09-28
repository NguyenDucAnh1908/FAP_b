package com.fap.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.metrics.DomainMetrics;
import com.fap.quiz.dto.AssignedQuizResponse;
import com.fap.quiz.dto.QuizAnswerItemRequest;
import com.fap.quiz.dto.QuizAttemptQuestionResponse;
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
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.user.entity.User;
import com.fap.user.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Business rules of a trainee's quiz attempt beyond the clock boundaries in
 * {@link QuizAttemptServiceTest}: who may start an attempt and how many, which answers may be saved,
 * and how a submission is graded. A wrong grade or an extra attempt changes a trainee's course
 * result, so each rule is checked on both its allowed and rejected side.
 */
class QuizAttemptServiceRulesTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 9, 0);
	private static final LocalDate TODAY = NOW.toLocalDate();
	private static final long QUIZ_ID = 31L;
	private static final long FIRST_QUESTION_ID = 501L;
	private static final long SECOND_QUESTION_ID = 502L;
	private static final long ATTEMPT_ID = 900L;
	private static final long CURRENT_USER_ID = 7L;
	private static final int DURATION_MINUTES = 30;
	private static final int MAX_ATTEMPTS = 2;
	private static final List<TrainingRegistrationStatus> ELIGIBLE_STATUSES = List.of(
			TrainingRegistrationStatus.Registered,
			TrainingRegistrationStatus.Completed);

	private final QuizRepository quizRepository = mock(QuizRepository.class);
	private final QuizAssignmentRepository quizAssignmentRepository = mock(QuizAssignmentRepository.class);
	private final QuizQuestionRepository quizQuestionRepository = mock(QuizQuestionRepository.class);
	private final QuizAttemptRepository quizAttemptRepository = mock(QuizAttemptRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

	private final QuizAttemptService service =
			serviceWith(Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

	private Quiz quiz;

	@BeforeEach
	void setUp() {
		lenient().doCallRealMethod().when(quizRepository).getQuizOrThrow(any());
		lenient().doCallRealMethod().when(quizAttemptRepository).getByIdAndUserIdOrThrow(any(), any());
		quiz = new Quiz();
		quiz.setId(QUIZ_ID);
		quiz.setTitle("Java basics");
		quiz.setStatus(QuizStatus.Published);
		quiz.setDurationMinutes(DURATION_MINUTES);
		quiz.setPassingScore(50);
		quiz.setMaxAttempts(MAX_ATTEMPTS);
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
		givenQuestions(List.of(quizQuestion(FIRST_QUESTION_ID, 1, "[\"A\"]", BigDecimal.ONE)));
	}

	@Test
	void startCreatesTheFirstAttemptInProgressForTheCurrentUser() {
		User user = new User();
		user.setId(CURRENT_USER_ID);
		when(userRepository.getReferenceById(CURRENT_USER_ID)).thenReturn(user);
		givenStartable(0);

		QuizAttemptResponse response = service.start(QUIZ_ID, CURRENT_USER_ID);

		QuizAttempt saved = captureSavedAttempt();
		assertThat(saved.getQuiz()).isSameAs(quiz);
		assertThat(saved.getUser()).isSameAs(user);
		assertThat(saved.getAttemptNumber()).isEqualTo(1);
		assertThat(saved.getStatus()).isEqualTo(QuizAttemptStatus.InProgress);
		assertThat(saved.getAnswersJson()).isEqualTo("[]");
		assertThat(saved.getStartedAt()).isEqualTo(NOW);
		assertThat(saved.getSubmittedAt()).isNull();
		assertThat(saved.getScore()).isNull();
		verify(auditLogService).record("START_QUIZ_ATTEMPT", "quiz_attempt", ATTEMPT_ID);
		assertThat(response.id()).isEqualTo(ATTEMPT_ID);
		assertThat(response.questions()).extracting(QuizAttemptQuestionResponse::questionId)
				.containsExactly(FIRST_QUESTION_ID);
	}

	@Test
	void startAllowsTheLastRemainingAttemptAndNumbersItAfterThePreviousOnes() {
		givenStartable(MAX_ATTEMPTS - 1);

		QuizAttemptResponse response = service.start(QUIZ_ID, CURRENT_USER_ID);

		assertThat(captureSavedAttempt().getAttemptNumber()).isEqualTo(MAX_ATTEMPTS);
		assertThat(response.attemptNumber()).isEqualTo(MAX_ATTEMPTS);
	}

	@ParameterizedTest(name = "{0} previous attempts of {1} allowed is rejected")
	@CsvSource({
			"2, 2",
			"3, 2",
			"1, 1"
	})
	void startRejectsWhenTheAttemptLimitIsReached(long previousAttempts, int maxAttempts) {
		quiz.setMaxAttempts(maxAttempts);
		givenStartable(previousAttempts);

		assertThatThrownBy(() -> service.start(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Quiz attempt limit reached")
				.extracting("code")
				.isEqualTo("QUIZ_ATTEMPT_LIMIT_REACHED");

		verify(quizAttemptRepository, never()).save(any());
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	/** A trainee with a running attempt is told to finish it, even when it was their last one. */
	@Test
	void startReportsTheRunningAttemptBeforeTheLimit() {
		givenStartable(MAX_ATTEMPTS);
		when(quizAttemptRepository.countByQuizIdAndUserIdAndStatus(
				QUIZ_ID, CURRENT_USER_ID, QuizAttemptStatus.InProgress)).thenReturn(1L);

		assertThatThrownBy(() -> service.start(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Finish the in-progress attempt before starting a new one")
				.extracting("code")
				.isEqualTo("QUIZ_ATTEMPT_IN_PROGRESS");

		verify(quizAttemptRepository, never()).save(any());
	}

	@Test
	void startRequiresAnAssignmentThroughAnEligibleRegistration() {
		when(quizAssignmentRepository.countEligibleAssignments(any(), any(), any())).thenReturn(0L);

		assertThatThrownBy(() -> service.start(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Quiz is not assigned to the current user")
				.extracting("code")
				.isEqualTo("QUIZ_ASSIGNMENT_REQUIRED");

		verify(quizAssignmentRepository).countEligibleAssignments(QUIZ_ID, CURRENT_USER_ID, ELIGIBLE_STATUSES);
		verify(quizAttemptRepository, never()).save(any());
	}

	@ParameterizedTest(name = "a {0} quiz cannot be attempted")
	@EnumSource(value = QuizStatus.class, names = {"Draft", "Closed"})
	void startRejectsAQuizThatIsNotPublished(QuizStatus status) {
		quiz.setStatus(status);

		assertThatThrownBy(() -> service.start(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Only published quiz can be attempted")
				.extracting("code")
				.isEqualTo("QUIZ_NOT_AVAILABLE");

		verifyNoInteractions(quizAssignmentRepository);
		verify(quizAttemptRepository, never()).save(any());
	}

	@Test
	void startReportsTheStatusBeforeThePassedCloseDate() {
		quiz.setStatus(QuizStatus.Closed);
		quiz.setCloseDate(TODAY.minusDays(1));

		assertThatThrownBy(() -> service.start(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("QUIZ_NOT_AVAILABLE");
	}

	@Test
	void startChecksTheWindowBeforeTheAssignment() {
		quiz.setOpenDate(TODAY.plusDays(1));

		assertThatThrownBy(() -> service.start(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Quiz is not open yet")
				.extracting("code")
				.isEqualTo("QUIZ_NOT_OPEN");

		verifyNoInteractions(quizAssignmentRepository);
	}

	@Test
	void startRejectsAfterTheCloseDateWithItsClientVisibleMessage() {
		quiz.setCloseDate(TODAY.minusDays(1));

		assertThatThrownBy(() -> service.start(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Quiz is already closed");

		verifyNoInteractions(quizAssignmentRepository);
	}

	@Test
	void startIsAllowedWhenTheQuizHasNoWindow() {
		givenStartable(0);

		QuizAttemptResponse response = service.start(QUIZ_ID, CURRENT_USER_ID);

		assertThat(response.status()).isEqualTo(QuizAttemptStatus.InProgress);
	}

	@Test
	void startUsesTheDateAndTimeOfTheClockZone() {
		// 20:00 UTC on 10 March is already 03:00 on 11 March for a UTC+7 clock.
		Clock clockAhead = Clock.fixed(NOW.withHour(20).toInstant(ZoneOffset.UTC), ZoneOffset.ofHours(7));
		quiz.setOpenDate(TODAY.plusDays(1));
		givenStartable(0);

		QuizAttemptResponse response = serviceWith(clockAhead).start(QUIZ_ID, CURRENT_USER_ID);

		assertThat(response.startedAt()).isEqualTo(LocalDateTime.of(2026, 3, 11, 3, 0));
	}

	@Test
	void startRejectsUnknownQuiz() {
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.start(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz not found");

		verifyNoInteractions(quizAssignmentRepository);
		verify(quizAttemptRepository, never()).save(any());
	}

	/**
	 * Current order, pinned: the attempt is saved before the empty question set is noticed, so only
	 * the transaction rollback keeps the row out of the database.
	 */
	@Test
	void startOnAQuizWithoutQuestionsFailsAfterSavingTheAttempt() {
		givenQuestions(List.of());
		givenStartable(0);

		assertThatThrownBy(() -> service.start(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Quiz requires at least one question")
				.extracting("code")
				.isEqualTo("QUIZ_QUESTION_REQUIRED");

		verify(quizAttemptRepository).save(any(QuizAttempt.class));
	}

	@Test
	void saveAnswersStoresTheSelectionAndAudits() {
		givenQuestions(List.of(
				quizQuestion(FIRST_QUESTION_ID, 1, "[\"A\"]", BigDecimal.ONE),
				quizQuestion(SECOND_QUESTION_ID, 2, "[\"B\",\"C\"]", BigDecimal.ONE)));
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), "[]");

		QuizAttemptResponse response = service.saveAnswers(ATTEMPT_ID, new SaveQuizAnswersRequest(List.of(
				new QuizAnswerItemRequest(FIRST_QUESTION_ID, json("[\"A\"]")),
				new QuizAnswerItemRequest(SECOND_QUESTION_ID, json("[\"C\",\"B\"]")))), CURRENT_USER_ID);

		JsonNode expected = json(storedAnswers(
				answer(FIRST_QUESTION_ID, "[\"A\"]"), answer(SECOND_QUESTION_ID, "[\"C\",\"B\"]")));
		assertThat(json(attempt.getAnswersJson())).isEqualTo(expected);
		assertThat(response.answersJson()).isEqualTo(expected);
		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.InProgress);
		assertThat(attempt.getScore()).isNull();
		verify(auditLogService).record("SAVE_QUIZ_ATTEMPT_ANSWERS", "quiz_attempt", ATTEMPT_ID);
	}

	@Test
	void saveAnswersReplacesTheStoredSelectionEvenWithAnEmptyOne() {
		QuizAttempt attempt = givenAttempt(
				QuizAttemptStatus.InProgress, NOW.minusMinutes(5), storedAnswers(answer(FIRST_QUESTION_ID, "[\"A\"]")));

		service.saveAnswers(ATTEMPT_ID, new SaveQuizAnswersRequest(List.of()), CURRENT_USER_ID);

		assertThat(attempt.getAnswersJson()).isEqualTo("[]");
	}

	@Test
	void saveAnswersIsStillAcceptedAtExactlyTheDeadline() {
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(DURATION_MINUTES), "[]");

		service.saveAnswers(ATTEMPT_ID, answers(FIRST_QUESTION_ID, "[\"A\"]"), CURRENT_USER_ID);

		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.InProgress);
		assertThat(json(attempt.getAnswersJson())).isEqualTo(json(storedAnswers(answer(FIRST_QUESTION_ID, "[\"A\"]"))));
		verify(auditLogService).record("SAVE_QUIZ_ATTEMPT_ANSWERS", "quiz_attempt", ATTEMPT_ID);
	}

	/**
	 * Current behaviour, pinned: answers sent after the deadline are dropped without an error, and
	 * the attempt is graded with what was saved before the deadline.
	 */
	@Test
	void saveAnswersAfterTheDeadlineGradesTheStoredAnswersAndDropsTheNewOnes() {
		LocalDateTime startedAt = NOW.minusMinutes(DURATION_MINUTES + 1);
		String stored = storedAnswers(answer(FIRST_QUESTION_ID, "[\"A\"]"));
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, startedAt, stored);

		QuizAttemptResponse response =
				service.saveAnswers(ATTEMPT_ID, answers(FIRST_QUESTION_ID, "[\"B\"]"), CURRENT_USER_ID);

		assertThat(attempt.getAnswersJson()).isEqualTo(stored);
		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(attempt.getScore()).isEqualTo(100);
		assertThat(attempt.getSubmittedAt()).isEqualTo(startedAt.plusMinutes(DURATION_MINUTES));
		assertThat(attempt.getTimeTakenSeconds()).isEqualTo(DURATION_MINUTES * 60);
		assertThat(response.status()).isEqualTo(QuizAttemptStatus.Submitted);
		verify(auditLogService).record("AUTO_SUBMIT_QUIZ_ATTEMPT_EXPIRED", "quiz_attempt", ATTEMPT_ID);
		verify(auditLogService, never()).record(eq("SAVE_QUIZ_ATTEMPT_ANSWERS"), anyString(), anyLong());
	}

	@Test
	void saveAnswersRejectsASubmittedAttempt() {
		String stored = storedAnswers(answer(FIRST_QUESTION_ID, "[\"A\"]"));
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.Submitted, NOW.minusMinutes(5), stored);

		assertThatThrownBy(() -> service.saveAnswers(ATTEMPT_ID, answers(FIRST_QUESTION_ID, "[\"B\"]"), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Only in-progress attempt can be changed")
				.extracting("code")
				.isEqualTo("QUIZ_ATTEMPT_NOT_EDITABLE");

		assertThat(attempt.getAnswersJson()).isEqualTo(stored);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void saveAnswersRejectsTwoAnswersForTheSameQuestion() {
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), "[]");
		SaveQuizAnswersRequest request = new SaveQuizAnswersRequest(List.of(
				new QuizAnswerItemRequest(FIRST_QUESTION_ID, json("[\"A\"]")),
				new QuizAnswerItemRequest(FIRST_QUESTION_ID, json("[\"B\"]"))));

		assertRejectedAnswers(request, "DUPLICATE_QUIZ_ANSWER", "Duplicate answer for quiz question");

		assertThat(attempt.getAnswersJson()).isEqualTo("[]");
	}

	@Test
	void saveAnswersRejectsAQuestionOutsideTheQuiz() {
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), "[]");

		assertRejectedAnswers(answers(99L, "[\"A\"]"),
				"QUIZ_ANSWER_QUESTION_NOT_FOUND", "Answer references a question outside this quiz");

		assertThat(attempt.getAnswersJson()).isEqualTo("[]");
	}

	@Test
	void saveAnswersRejectsASelectionThatIsNotAnArray() {
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), "[]");
		SaveQuizAnswersRequest request = new SaveQuizAnswersRequest(List.of(
				new QuizAnswerItemRequest(FIRST_QUESTION_ID, JsonNodeFactory.instance.textNode("A"))));

		assertRejectedAnswers(request, "QUIZ_ANSWER_ARRAY_REQUIRED", "Selected answers must be a JSON array");

		assertThat(attempt.getAnswersJson()).isEqualTo("[]");
	}

	@Test
	void saveAnswersRejectsAnotherUsersAttempt() {
		when(quizAttemptRepository.findByIdAndUserId(ATTEMPT_ID, CURRENT_USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.saveAnswers(ATTEMPT_ID, answers(FIRST_QUESTION_ID, "[\"A\"]"), CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz attempt not found");

		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void submitAllCorrectScoresHundredAndPasses() {
		givenQuestions(List.of(
				quizQuestion(FIRST_QUESTION_ID, 1, "[\"A\"]", BigDecimal.ONE),
				quizQuestion(SECOND_QUESTION_ID, 2, "[\"B\",\"C\"]", BigDecimal.ONE)));
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(10), storedAnswers(
				answer(FIRST_QUESTION_ID, "[\"A\"]"), answer(SECOND_QUESTION_ID, "[\"B\",\"C\"]")));

		QuizAttemptResponse response = service.submit(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(attempt.getScore()).isEqualTo(100);
		assertThat(attempt.getCorrectCount()).isEqualTo(2);
		assertThat(attempt.getTotalQuestions()).isEqualTo(2);
		assertThat(attempt.getPassed()).isTrue();
		assertThat(attempt.getSubmittedAt()).isEqualTo(NOW);
		assertThat(attempt.getTimeTakenSeconds()).isEqualTo(600);
		assertThat(response.score()).isEqualTo(100);
		verify(auditLogService).record("SUBMIT_QUIZ_ATTEMPT", "quiz_attempt", ATTEMPT_ID);
	}

	@ParameterizedTest(name = "{1} of {0} correct scores {2}")
	@CsvSource({
			"8, 1, 13",
			"3, 1, 33",
			"3, 2, 67",
			"2, 1, 50",
			"4, 0, 0",
			"4, 4, 100"
	})
	void submitScoresTheCorrectShareRoundedHalfUp(int questionCount, int correctCount, int expectedScore) {
		List<QuizQuestion> questions = new ArrayList<>();
		List<String> stored = new ArrayList<>();
		for (int index = 0; index < questionCount; index++) {
			long questionId = FIRST_QUESTION_ID + index;
			questions.add(quizQuestion(questionId, index + 1, "[\"A\"]", BigDecimal.ONE));
			stored.add(answer(questionId, index < correctCount ? "[\"A\"]" : "[\"B\"]"));
		}
		givenQuestions(questions);
		QuizAttempt attempt = givenAttempt(
				QuizAttemptStatus.InProgress, NOW.minusMinutes(5), storedAnswers(stored.toArray(String[]::new)));

		service.submit(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getScore()).isEqualTo(expectedScore);
		assertThat(attempt.getCorrectCount()).isEqualTo(correctCount);
		assertThat(attempt.getTotalQuestions()).isEqualTo(questionCount);
	}

	@ParameterizedTest(name = "2-point question correct {0}, 1-point question correct {1} scores {2}")
	@CsvSource({
			"true, false, 67",
			"false, true, 33"
	})
	void submitWeightsTheScoreByQuestionPoints(boolean heavyCorrect, boolean lightCorrect, int expectedScore) {
		givenQuestions(List.of(
				quizQuestion(FIRST_QUESTION_ID, 1, "[\"A\"]", new BigDecimal("2.00")),
				quizQuestion(SECOND_QUESTION_ID, 2, "[\"A\"]", BigDecimal.ONE)));
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), storedAnswers(
				answer(FIRST_QUESTION_ID, heavyCorrect ? "[\"A\"]" : "[\"B\"]"),
				answer(SECOND_QUESTION_ID, lightCorrect ? "[\"A\"]" : "[\"B\"]")));

		service.submit(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getScore()).isEqualTo(expectedScore);
		assertThat(attempt.getCorrectCount()).isEqualTo(1);
	}

	@ParameterizedTest(name = "passing score {0}: passed {1}")
	@CsvSource({
			"50, true",
			"51, false"
	})
	void submitPassesFromExactlyThePassingScore(int passingScore, boolean expectedPassed) {
		quiz.setPassingScore(passingScore);
		givenQuestions(List.of(
				quizQuestion(FIRST_QUESTION_ID, 1, "[\"A\"]", BigDecimal.ONE),
				quizQuestion(SECOND_QUESTION_ID, 2, "[\"A\"]", BigDecimal.ONE)));
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), storedAnswers(
				answer(FIRST_QUESTION_ID, "[\"A\"]"), answer(SECOND_QUESTION_ID, "[\"B\"]")));

		service.submit(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getScore()).isEqualTo(50);
		assertThat(attempt.getPassed()).isEqualTo(expectedPassed);
	}

	@Test
	void submitCountsAnUnansweredQuestionAsWrong() {
		givenQuestions(List.of(
				quizQuestion(FIRST_QUESTION_ID, 1, "[\"A\"]", BigDecimal.ONE),
				quizQuestion(SECOND_QUESTION_ID, 2, "[\"A\"]", BigDecimal.ONE)));
		QuizAttempt attempt = givenAttempt(
				QuizAttemptStatus.InProgress, NOW.minusMinutes(5), storedAnswers(answer(FIRST_QUESTION_ID, "[\"A\"]")));

		service.submit(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getCorrectCount()).isEqualTo(1);
		assertThat(attempt.getTotalQuestions()).isEqualTo(2);
		assertThat(attempt.getScore()).isEqualTo(50);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("selectionComparisons")
	void submitComparesTheSelectionWithTheKeyAsASet(String rule, String key, String selected, boolean expectedCorrect) {
		givenQuestions(List.of(quizQuestion(FIRST_QUESTION_ID, 1, key, BigDecimal.ONE)));
		QuizAttempt attempt = givenAttempt(
				QuizAttemptStatus.InProgress, NOW.minusMinutes(5), storedAnswers(answer(FIRST_QUESTION_ID, selected)));

		service.submit(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getCorrectCount()).isEqualTo(expectedCorrect ? 1 : 0);
		assertThat(attempt.getScore()).isEqualTo(expectedCorrect ? 100 : 0);
	}

	/**
	 * Clock skew between the node that started the attempt and the one grading it must not yield a
	 * negative duration.
	 */
	@Test
	void submitClampsANegativeTimeTakenToZero() {
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.plusMinutes(1), "[]");

		service.submit(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getTimeTakenSeconds()).isZero();
		assertThat(attempt.getSubmittedAt()).isEqualTo(NOW);
	}

	@Test
	void submitAtExactlyTheDeadlineIsARegularSubmission() {
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(DURATION_MINUTES), "[]");

		service.submit(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getSubmittedAt()).isEqualTo(NOW);
		assertThat(attempt.getTimeTakenSeconds()).isEqualTo(DURATION_MINUTES * 60);
		verify(auditLogService).record("SUBMIT_QUIZ_ATTEMPT", "quiz_attempt", ATTEMPT_ID);
		verify(auditLogService, never()).record(eq("AUTO_SUBMIT_QUIZ_ATTEMPT_EXPIRED"), anyString(), anyLong());
	}

	@Test
	void submitAfterTheDeadlineIsStampedAtTheDeadline() {
		LocalDateTime startedAt = NOW.minusMinutes(DURATION_MINUTES + 10);
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.InProgress, startedAt, "[]");

		QuizAttemptResponse response = service.submit(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(attempt.getSubmittedAt()).isEqualTo(startedAt.plusMinutes(DURATION_MINUTES));
		assertThat(attempt.getTimeTakenSeconds()).isEqualTo(DURATION_MINUTES * 60);
		assertThat(attempt.getPassed()).isFalse();
		assertThat(response.status()).isEqualTo(QuizAttemptStatus.Submitted);
		verify(auditLogService).record("AUTO_SUBMIT_QUIZ_ATTEMPT_EXPIRED", "quiz_attempt", ATTEMPT_ID);
		verify(auditLogService, never()).record(eq("SUBMIT_QUIZ_ATTEMPT"), anyString(), anyLong());
	}

	@Test
	void submitRejectsAnAlreadySubmittedAttemptAndKeepsItsGrade() {
		QuizAttempt attempt = givenSubmittedAttempt(40, "[]");

		assertThatThrownBy(() -> service.submit(ATTEMPT_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("QUIZ_ATTEMPT_NOT_EDITABLE");

		assertThat(attempt.getScore()).isEqualTo(40);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void submitRejectsAnotherUsersAttempt() {
		when(quizAttemptRepository.findByIdAndUserId(ATTEMPT_ID, CURRENT_USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.submit(ATTEMPT_ID, CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz attempt not found");
	}

	@Test
	void submitIsTimed() {
		givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), "[]");

		service.submit(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(meterRegistry.get("fap.quiz.attempt.submit").timer().count()).isEqualTo(1);
	}

	@Test
	void getNeverRegradesASubmittedAttempt() {
		QuizAttempt attempt = givenSubmittedAttempt(40, storedAnswers(answer(FIRST_QUESTION_ID, "[\"A\"]")));

		QuizAttemptResponse response = service.get(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getScore()).isEqualTo(40);
		assertThat(response.score()).isEqualTo(40);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void reviewRejectsARunningAttempt() {
		givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), "[]");

		assertThatThrownBy(() -> service.review(ATTEMPT_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Only submitted attempt can be reviewed")
				.extracting("code")
				.isEqualTo("QUIZ_ATTEMPT_REVIEW_UNAVAILABLE");
	}

	@Test
	void reviewShowsTheSelectionTheKeyAndTheOutcomePerQuestion() {
		givenQuestions(List.of(
				quizQuestion(FIRST_QUESTION_ID, 1, "[\"A\"]", BigDecimal.ONE),
				quizQuestion(SECOND_QUESTION_ID, 2, "[\"B\"]", BigDecimal.ONE)));
		givenSubmittedAttempt(50, storedAnswers(answer(FIRST_QUESTION_ID, "[\"A\"]")));

		QuizAttemptReviewResponse review = service.review(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(review.score()).isEqualTo(50);
		assertThat(review.questions()).extracting(QuizAttemptReviewQuestionResponse::questionId)
				.containsExactly(FIRST_QUESTION_ID, SECOND_QUESTION_ID);
		QuizAttemptReviewQuestionResponse answered = review.questions().get(0);
		assertThat(answered.selectedAnswersJson()).isEqualTo(json("[\"A\"]"));
		assertThat(answered.correctAnswersJson()).isEqualTo(json("[\"A\"]"));
		assertThat(answered.correct()).isTrue();
		assertThat(answered.optionsJson()).isEqualTo(json("[\"A\",\"B\",\"C\"]"));
		assertThat(answered.explanation()).isEqualTo("Because of " + FIRST_QUESTION_ID);
		QuizAttemptReviewQuestionResponse unanswered = review.questions().get(1);
		assertThat(unanswered.selectedAnswersJson()).isEqualTo(json("[]"));
		assertThat(unanswered.correctAnswersJson()).isEqualTo(json("[\"B\"]"));
		assertThat(unanswered.correct()).isFalse();
	}

	@Test
	void reviewSubmitsAnExpiredAttemptFirst() {
		QuizAttempt attempt = givenAttempt(
				QuizAttemptStatus.InProgress,
				NOW.minusMinutes(DURATION_MINUTES + 1),
				storedAnswers(answer(FIRST_QUESTION_ID, "[\"A\"]")));

		QuizAttemptReviewResponse review = service.review(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(review.status()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(review.score()).isEqualTo(100);
		verify(auditLogService).record("AUTO_SUBMIT_QUIZ_ATTEMPT_EXPIRED", "quiz_attempt", ATTEMPT_ID);
	}

	@Test
	void reviewRejectsAnotherUsersAttempt() {
		when(quizAttemptRepository.findByIdAndUserId(ATTEMPT_ID, CURRENT_USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.review(ATTEMPT_ID, CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz attempt not found");
	}

	/**
	 * The shuffled order is not stored, so it must be derived from the attempt alone: every read of
	 * the same attempt, including its review, shows the questions in the same order.
	 */
	@Test
	void randomizedOrderIsDerivedFromTheAttemptAndStableAcrossReads() {
		quiz.setRandomize(true);
		List<Long> sortOrderIds = List.of(
				FIRST_QUESTION_ID, FIRST_QUESTION_ID + 1, FIRST_QUESTION_ID + 2,
				FIRST_QUESTION_ID + 3, FIRST_QUESTION_ID + 4);
		givenOnePointQuestionsInSortOrder(sortOrderIds);
		givenSubmittedAttempt(0, "[]");
		List<Long> expected = shuffledForAttempt(sortOrderIds);

		List<Long> firstRead = questionIds(service.get(ATTEMPT_ID, CURRENT_USER_ID));
		List<Long> secondRead = questionIds(service.get(ATTEMPT_ID, CURRENT_USER_ID));
		List<Long> reviewOrder = service.review(ATTEMPT_ID, CURRENT_USER_ID).questions().stream()
				.map(QuizAttemptReviewQuestionResponse::questionId)
				.toList();

		assertThat(expected).isNotEqualTo(sortOrderIds);
		assertThat(firstRead).containsExactlyElementsOf(expected);
		assertThat(secondRead).containsExactlyElementsOf(expected);
		assertThat(reviewOrder).containsExactlyElementsOf(expected);
	}

	/**
	 * The seeded shuffle of a short list can happen to be the identity (for two questions and this
	 * attempt id it is), which would hide a missing randomize guard. So the fixture is one that the
	 * shuffle would visibly reorder, and its sort order runs against the id order so that sorting by
	 * id would not pass either.
	 */
	@Test
	void nonRandomizedQuizKeepsTheSortOrder() {
		List<Long> sortOrderIds = List.of(
				FIRST_QUESTION_ID + 4, FIRST_QUESTION_ID + 3, FIRST_QUESTION_ID + 2,
				FIRST_QUESTION_ID + 1, FIRST_QUESTION_ID);
		givenOnePointQuestionsInSortOrder(sortOrderIds);
		givenAttempt(QuizAttemptStatus.InProgress, NOW.minusMinutes(5), "[]");

		List<Long> readOrder = questionIds(service.get(ATTEMPT_ID, CURRENT_USER_ID));

		assertThat(shuffledForAttempt(sortOrderIds)).isNotEqualTo(sortOrderIds);
		assertThat(readOrder).containsExactlyElementsOf(sortOrderIds);
	}

	@Test
	void assignedListsPublishedQuizzesOpenOnTheClockDateForEligibleRegistrations() {
		when(quizRepository.searchAssignedToUser(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

		service.assigned(CURRENT_USER_ID, 0, 20);

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(quizRepository).searchAssignedToUser(
				eq(CURRENT_USER_ID), eq(QuizStatus.Published), eq(ELIGIBLE_STATUSES), eq(TODAY), pageable.capture());
		assertThat(pageable.getValue().getSort())
				.isEqualTo(Sort.by(Sort.Direction.ASC, "closeDate").and(Sort.by(Sort.Direction.DESC, "id")));
	}

	@ParameterizedTest(name = "{0} attempts used of 2 leaves {1}")
	@CsvSource({
			"0, 2",
			"1, 1",
			"2, 0",
			"3, 0"
	})
	void assignedReportsTheRemainingAttemptsNeverBelowZero(long attemptsUsed, long expectedRemaining) {
		when(quizRepository.searchAssignedToUser(any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(quiz)));
		when(quizAttemptRepository.countByQuizIdAndUserId(QUIZ_ID, CURRENT_USER_ID)).thenReturn(attemptsUsed);
		when(quizQuestionRepository.countByIdQuizId(QUIZ_ID)).thenReturn(3L);

		AssignedQuizResponse response = service.assigned(CURRENT_USER_ID, 0, 20).getContent().get(0);

		assertThat(response.attemptCount()).isEqualTo(attemptsUsed);
		assertThat(response.remainingAttempts()).isEqualTo(expectedRemaining);
		assertThat(response.questionCount()).isEqualTo(3);
	}

	@Test
	void assignedShowsTheLatestAttemptOutcome() {
		QuizAttempt latest = new QuizAttempt();
		latest.setId(55L);
		latest.setStatus(QuizAttemptStatus.Submitted);
		latest.setScore(80);
		latest.setPassed(true);
		when(quizRepository.searchAssignedToUser(any(), any(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(quiz)));
		when(quizAttemptRepository.findFirstByQuizIdAndUserIdOrderByIdDesc(QUIZ_ID, CURRENT_USER_ID))
				.thenReturn(Optional.of(latest));

		AssignedQuizResponse response = service.assigned(CURRENT_USER_ID, 0, 20).getContent().get(0);

		assertThat(response.latestAttemptId()).isEqualTo(55L);
		assertThat(response.latestAttemptStatus()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(response.latestScore()).isEqualTo(80);
		assertThat(response.latestPassed()).isTrue();
	}

	@Test
	void assignedRejectsAnUnknownSortField() {
		assertThatThrownBy(() -> service.assigned(CURRENT_USER_ID, 0, 20, "passingScore", "asc"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_FIELD");

		verify(quizRepository, never()).searchAssignedToUser(any(), any(), any(), any(), any());
	}

	static Stream<Arguments> selectionComparisons() {
		return Stream.of(
				Arguments.of("the order of the options is ignored", "[\"A\",\"C\"]", "[\"C\",\"A\"]", true),
				Arguments.of("a missing option earns no partial credit", "[\"A\",\"C\"]", "[\"A\"]", false),
				Arguments.of("an extra option makes it wrong", "[\"A\",\"C\"]", "[\"A\",\"B\",\"C\"]", false),
				Arguments.of("an empty selection is wrong", "[\"A\"]", "[]", false),
				Arguments.of("numbers and text are compared by their text", "[1]", "[\"1\"]", true),
				Arguments.of("text is compared case-sensitively", "[\"A\"]", "[\"a\"]", false),
				Arguments.of("a repeated option counts once", "[\"A\"]", "[\"A\",\"A\"]", true));
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
				new DomainMetrics(meterRegistry),
				clock);
	}

	private void assertRejectedAnswers(SaveQuizAnswersRequest request, String code, String message) {
		assertThatThrownBy(() -> service.saveAnswers(ATTEMPT_ID, request, CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.hasMessage(message)
				.extracting("code")
				.isEqualTo(code);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	private void givenStartable(long previousAttempts) {
		when(quizAssignmentRepository.countEligibleAssignments(any(), any(), any())).thenReturn(1L);
		when(quizAttemptRepository.countByQuizIdAndUserId(QUIZ_ID, CURRENT_USER_ID)).thenReturn(previousAttempts);
		when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> {
			QuizAttempt saved = invocation.getArgument(0);
			saved.setId(ATTEMPT_ID);
			return saved;
		});
	}

	private void givenQuestions(List<QuizQuestion> questions) {
		when(quizQuestionRepository.findByIdQuizIdOrderBySortOrderAsc(QUIZ_ID)).thenReturn(questions);
	}

	/** One-point questions that the repository returns in the given order, numbered 1..n. */
	private void givenOnePointQuestionsInSortOrder(List<Long> questionIds) {
		List<QuizQuestion> questions = new ArrayList<>();
		for (int index = 0; index < questionIds.size(); index++) {
			questions.add(quizQuestion(questionIds.get(index), index + 1, "[\"A\"]", BigDecimal.ONE));
		}
		givenQuestions(questions);
	}

	/** The order a randomized quiz shows for this attempt: the service seeds the shuffle with the attempt id. */
	private static List<Long> shuffledForAttempt(List<Long> sortOrderIds) {
		List<Long> shuffled = new ArrayList<>(sortOrderIds);
		Collections.shuffle(shuffled, new Random(ATTEMPT_ID));
		return shuffled;
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

	/** Submitted long before the deadline check could matter, with a grade that differs from a regrade. */
	private QuizAttempt givenSubmittedAttempt(int score, String answersJson) {
		LocalDateTime startedAt = NOW.minusDays(1);
		QuizAttempt attempt = givenAttempt(QuizAttemptStatus.Submitted, startedAt, answersJson);
		attempt.setScore(score);
		attempt.setPassed(score >= quiz.getPassingScore());
		attempt.setSubmittedAt(startedAt.plusMinutes(10));
		return attempt;
	}

	private QuizQuestion quizQuestion(long questionId, int sortOrder, String correctAnswersJson, BigDecimal points) {
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
		quizQuestion.setPoints(points);
		return quizQuestion;
	}

	private SaveQuizAnswersRequest answers(long questionId, String selectedJson) {
		return new SaveQuizAnswersRequest(List.of(new QuizAnswerItemRequest(questionId, json(selectedJson))));
	}

	/** One stored answer item, in the shape {@code saveAnswers} writes. */
	private static String answer(long questionId, String selectedJson) {
		return "{\"questionId\":" + questionId + ",\"selectedAnswersJson\":" + selectedJson + "}";
	}

	private static String storedAnswers(String... items) {
		return "[" + String.join(",", items) + "]";
	}

	private QuizAttempt captureSavedAttempt() {
		ArgumentCaptor<QuizAttempt> saved = ArgumentCaptor.forClass(QuizAttempt.class);
		verify(quizAttemptRepository).save(saved.capture());
		return saved.getValue();
	}

	private static List<Long> questionIds(QuizAttemptResponse response) {
		return response.questions().stream()
				.map(QuizAttemptQuestionResponse::questionId)
				.toList();
	}

	private JsonNode json(String value) {
		try {
			return objectMapper.readTree(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Invalid test JSON: " + value, exception);
		}
	}
}
