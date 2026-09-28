package com.fap.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fap.clazz.service.ClassAccessService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.ForbiddenException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.security.FapUserPrincipal;
import com.fap.common.security.RoleNames;
import com.fap.quiz.dto.QuizAttemptResultResponse;
import com.fap.quiz.dto.QuizAttemptReviewQuestionResponse;
import com.fap.quiz.dto.QuizAttemptReviewResponse;
import com.fap.quiz.dto.QuizAttemptSummaryResponse;
import com.fap.quiz.entity.Question;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.entity.QuizQuestion;
import com.fap.quiz.entity.QuizQuestionId;
import com.fap.quiz.enums.QuestionDifficulty;
import com.fap.quiz.enums.QuestionType;
import com.fap.quiz.enums.QuizAttemptStatus;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.repository.QuizAttemptRepository;
import com.fap.quiz.repository.QuizAttemptStats;
import com.fap.quiz.repository.QuizQuestionRepository;
import com.fap.quiz.repository.QuizRepository;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Quiz results expose other people's scores and answers, so every read is gated three ways: the
 * caller's role, the class/session filter they ask for, and the per-attempt visibility scope
 * (Super Admin sees everything, Class Admin and Trainer only trainees of their own classes). The
 * summary's pass rate is shown to staff as a percentage, so its rounding is pinned too.
 */
class QuizResultServiceTest {

	private static final long QUIZ_ID = 31L;
	private static final long ATTEMPT_ID = 900L;
	private static final long VIEWER_ID = 7L;
	private static final long TRAINEE_ID = 42L;
	private static final long CLASS_ID = 10L;
	private static final long SESSION_ID = 61L;
	private static final long FIRST_QUESTION_ID = 501L;
	private static final long SECOND_QUESTION_ID = 502L;
	private static final long THIRD_QUESTION_ID = 503L;
	private static final List<TrainingRegistrationStatus> ELIGIBLE_STATUSES = List.of(
			TrainingRegistrationStatus.Registered,
			TrainingRegistrationStatus.Completed);
	private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 3, 10, 9, 0);
	private static final LocalDateTime SUBMITTED_AT = STARTED_AT.plusMinutes(12);

	private final QuizRepository quizRepository = mock(QuizRepository.class);
	private final QuizAttemptRepository quizAttemptRepository = mock(QuizAttemptRepository.class);
	private final QuizQuestionRepository quizQuestionRepository = mock(QuizQuestionRepository.class);
	private final ClassAccessService classAccessService = mock(ClassAccessService.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final QuizResultService service = new QuizResultService(
			quizRepository,
			quizAttemptRepository,
			quizQuestionRepository,
			classAccessService,
			objectMapper);

	private Quiz quiz;

	@BeforeEach
	void setUp() {
		// The lookup helpers are interface default methods; a plain mock would return null from them.
		lenient().doCallRealMethod().when(quizRepository).getQuizOrThrow(any());
		lenient().doCallRealMethod().when(quizAttemptRepository).getByQuizIdAndIdOrThrow(any(), any());
		quiz = new Quiz();
		quiz.setId(QUIZ_ID);
		quiz.setTitle("Java basics");
		quiz.setStatus(QuizStatus.Published);
		when(quizRepository.existsById(QUIZ_ID)).thenReturn(true);
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
		when(quizAttemptRepository.searchQuizResults(
				any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any()))
				.thenReturn(Page.empty());
	}

	@Test
	void listAttemptsReportsUnknownQuizBeforeTheRoleCheck() {
		when(quizRepository.existsById(QUIZ_ID)).thenReturn(false);

		assertThatThrownBy(() -> listAttempts(principal(RoleNames.TRAINEE), null, null))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);
		verifySearchNeverCalled();
	}

	@Test
	void listAttemptsForbidsTraineeBeforeCheckingTheClassFilter() {
		assertThatThrownBy(() -> listAttempts(principal(RoleNames.TRAINEE), CLASS_ID, SESSION_ID))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("You cannot view quiz results")
				.extracting("code")
				.isEqualTo("ACCESS_DENIED");
		verifyNoInteractions(classAccessService);
		verifySearchNeverCalled();
	}

	@Test
	void listAttemptsForbidsPrincipalWithoutAnyRole() {
		FapUserPrincipal principal = new FapUserPrincipal(VIEWER_ID, "nobody@fap.local", "", Set.of(), true, List.of());

		assertThatThrownBy(() -> listAttempts(principal, null, null))
				.isInstanceOf(ForbiddenException.class)
				.extracting("code")
				.isEqualTo("ACCESS_DENIED");
		verifySearchNeverCalled();
	}

	@ParameterizedTest(name = "{0} may list quiz results")
	@ValueSource(strings = {RoleNames.SUPER_ADMIN, RoleNames.CLASS_ADMIN, RoleNames.TRAINER})
	void listAttemptsAllowsResultViewerRoles(String role) {
		Page<QuizAttemptResultResponse> page = listAttempts(principal(role), null, null);

		assertThat(page.getContent()).isEmpty();
		verify(quizAttemptRepository).searchQuizResults(
				any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any());
	}

	@Test
	void listAttemptsLetsSuperAdminSeeEveryAttempt() {
		listAttempts(principal(RoleNames.SUPER_ADMIN), null, null);

		verify(quizAttemptRepository).searchQuizResults(
				eq(QUIZ_ID), any(), any(), any(), any(), any(),
				eq(true), eq(VIEWER_ID), eq(ELIGIBLE_STATUSES), any());
	}

	@ParameterizedTest(name = "{0} only sees trainees of their own classes")
	@ValueSource(strings = {RoleNames.CLASS_ADMIN, RoleNames.TRAINER})
	void listAttemptsScopesClassAdminAndTrainerToTheirOwnTrainees(String role) {
		listAttempts(principal(role), null, null);

		verify(quizAttemptRepository).searchQuizResults(
				eq(QUIZ_ID), any(), any(), any(), any(), any(),
				eq(false), eq(VIEWER_ID), eq(ELIGIBLE_STATUSES), any());
	}

	@Test
	void listAttemptsPassesEveryFilterToTheQuery() {
		FapUserPrincipal principal = principal(RoleNames.CLASS_ADMIN);

		service.listAttempts(
				QUIZ_ID, QuizAttemptStatus.Submitted, true, TRAINEE_ID, CLASS_ID, SESSION_ID, principal, 0, 20);

		verify(quizAttemptRepository).searchQuizResults(
				eq(QUIZ_ID), eq(QuizAttemptStatus.Submitted), eq(true), eq(TRAINEE_ID), eq(CLASS_ID), eq(SESSION_ID),
				eq(false), eq(VIEWER_ID), eq(ELIGIBLE_STATUSES), any());
	}

	@Test
	void listAttemptsChecksClassAccessForAClassFilter() {
		FapUserPrincipal principal = principal(RoleNames.TRAINER);

		listAttempts(principal, CLASS_ID, null);

		verify(classAccessService).assertCanViewClass(principal, CLASS_ID);
		verify(classAccessService, never()).assertCanViewSession(any(), anyLong());
	}

	@Test
	void listAttemptsChecksSessionAccessForASessionFilter() {
		FapUserPrincipal principal = principal(RoleNames.TRAINER);

		listAttempts(principal, null, SESSION_ID);

		verify(classAccessService).assertCanViewSession(principal, SESSION_ID);
		verify(classAccessService, never()).assertCanViewClass(any(), anyLong());
	}

	@Test
	void listAttemptsSkipsClassAccessChecksWithoutFilters() {
		listAttempts(principal(RoleNames.CLASS_ADMIN), null, null);

		verifyNoInteractions(classAccessService);
	}

	@Test
	void listAttemptsStopsWhenTheClassFilterIsNotAccessible() {
		FapUserPrincipal principal = principal(RoleNames.CLASS_ADMIN);
		doThrow(new ForbiddenException("You cannot view this class"))
				.when(classAccessService).assertCanViewClass(principal, CLASS_ID);

		assertThatThrownBy(() -> listAttempts(principal, CLASS_ID, null))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("You cannot view this class");
		verifySearchNeverCalled();
	}

	@Test
	void listAttemptsStopsWhenTheSessionFilterIsNotAccessible() {
		FapUserPrincipal principal = principal(RoleNames.TRAINER);
		doThrow(new ForbiddenException("You cannot view this training session"))
				.when(classAccessService).assertCanViewSession(principal, SESSION_ID);

		assertThatThrownBy(() -> listAttempts(principal, null, SESSION_ID))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("You cannot view this training session");
		verifySearchNeverCalled();
	}

	@Test
	void listAttemptsDefaultsToNewestAttemptFirst() {
		service.listAttempts(QUIZ_ID, null, null, null, null, null, principal(RoleNames.SUPER_ADMIN), 2, 25);

		Pageable pageable = capturePageable();
		assertThat(pageable.getPageNumber()).isEqualTo(2);
		assertThat(pageable.getPageSize()).isEqualTo(25);
		assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
	}

	@Test
	void listAttemptsSortsByARequestedWhitelistedField() {
		service.listAttempts(
				QUIZ_ID, null, null, null, null, null, principal(RoleNames.SUPER_ADMIN), 0, 20, "score", "asc");

		assertThat(capturePageable().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "score"));
	}

	@Test
	void listAttemptsRejectsASortFieldOutsideTheWhitelist() {
		assertThatThrownBy(() -> service.listAttempts(
				QUIZ_ID, null, null, null, null, null, principal(RoleNames.SUPER_ADMIN), 0, 20, "user.email", "asc"))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_SORT_FIELD");
		verifySearchNeverCalled();
	}

	@Test
	void listAttemptsMapsTheAttemptTraineeAndScoreIntoAResultRow() {
		QuizAttempt attempt = submittedAttempt("[]");
		attempt.setScore(80);
		attempt.setCorrectCount(4);
		attempt.setTotalQuestions(5);
		attempt.setPassed(true);
		attempt.setTimeTakenSeconds(720);
		when(quizAttemptRepository.searchQuizResults(
				any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any()))
				.thenReturn(new PageImpl<>(List.of(attempt)));

		Page<QuizAttemptResultResponse> page = listAttempts(principal(RoleNames.SUPER_ADMIN), null, null);

		assertThat(page.getContent()).singleElement().satisfies(row -> {
			assertThat(row.id()).isEqualTo(ATTEMPT_ID);
			assertThat(row.quizId()).isEqualTo(QUIZ_ID);
			assertThat(row.quizTitle()).isEqualTo("Java basics");
			assertThat(row.userId()).isEqualTo(TRAINEE_ID);
			assertThat(row.userFullName()).isEqualTo("Trainee One");
			assertThat(row.userEmail()).isEqualTo("trainee@fap.local");
			assertThat(row.attemptNumber()).isEqualTo(1);
			assertThat(row.status()).isEqualTo(QuizAttemptStatus.Submitted);
			assertThat(row.score()).isEqualTo(80);
			assertThat(row.correctCount()).isEqualTo(4);
			assertThat(row.totalQuestions()).isEqualTo(5);
			assertThat(row.passed()).isTrue();
			assertThat(row.timeTakenSeconds()).isEqualTo(720);
			assertThat(row.startedAt()).isEqualTo(STARTED_AT);
			assertThat(row.submittedAt()).isEqualTo(SUBMITTED_AT);
		});
	}

	@Test
	void getAttemptDetailForbidsTraineeBeforeLookingUpTheAttempt() {
		assertThatThrownBy(() -> service.getAttemptDetail(QUIZ_ID, ATTEMPT_ID, principal(RoleNames.TRAINEE)))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("You cannot view quiz results")
				.extracting("code")
				.isEqualTo("ACCESS_DENIED");
		verify(quizAttemptRepository, never()).findByQuizIdAndId(any(), any());
		verify(quizAttemptRepository, never()).countVisibleQuizResult(any(), any(), anyBoolean(), any(), any());
	}

	/** Scoped to the quiz, so an attempt id of another quiz is not found rather than reviewable. */
	@Test
	void getAttemptDetailRejectsAnUnknownOrForeignAttempt() {
		when(quizAttemptRepository.findByQuizIdAndId(QUIZ_ID, ATTEMPT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getAttemptDetail(QUIZ_ID, ATTEMPT_ID, principal(RoleNames.SUPER_ADMIN)))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz attempt not found")
				.extracting("code")
				.isEqualTo(NotFoundException.CODE);
		verify(quizAttemptRepository, never()).countVisibleQuizResult(any(), any(), anyBoolean(), any(), any());
	}

	@Test
	void getAttemptDetailForbidsAnAttemptOutsideTheViewersClasses() {
		givenAttempt(submittedAttempt("[]"));
		when(quizAttemptRepository.countVisibleQuizResult(
				eq(QUIZ_ID), eq(ATTEMPT_ID), anyBoolean(), any(), any())).thenReturn(0L);

		assertThatThrownBy(() -> service.getAttemptDetail(QUIZ_ID, ATTEMPT_ID, principal(RoleNames.TRAINER)))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("You cannot view this quiz attempt")
				.extracting("code")
				.isEqualTo("ACCESS_DENIED");
		verifyNoInteractions(quizQuestionRepository);
	}

	@ParameterizedTest(name = "{0} is checked with scopeAll={1}")
	@CsvSource({
			RoleNames.SUPER_ADMIN + ", true",
			RoleNames.CLASS_ADMIN + ", false",
			RoleNames.TRAINER + ", false"
	})
	void getAttemptDetailChecksVisibilityWithTheViewersScope(String role, boolean scopeAll) {
		givenAttempt(submittedAttempt("[]"));
		givenVisibleAttempt();

		service.getAttemptDetail(QUIZ_ID, ATTEMPT_ID, principal(role));

		verify(quizAttemptRepository).countVisibleQuizResult(
				QUIZ_ID, ATTEMPT_ID, scopeAll, VIEWER_ID, ELIGIBLE_STATUSES);
	}

	@Test
	void getAttemptDetailRejectsAnAttemptStillInProgress() {
		QuizAttempt attempt = submittedAttempt("[]");
		attempt.setStatus(QuizAttemptStatus.InProgress);
		givenAttempt(attempt);
		givenVisibleAttempt();

		assertThatThrownBy(() -> service.getAttemptDetail(QUIZ_ID, ATTEMPT_ID, principal(RoleNames.SUPER_ADMIN)))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Only submitted attempt can be reviewed")
				.extracting("code")
				.isEqualTo("QUIZ_ATTEMPT_REVIEW_UNAVAILABLE");
		verifyNoInteractions(quizQuestionRepository);
	}

	/**
	 * Staff review always follows the quiz's sort order, even for a randomized quiz, unlike the
	 * trainee's own review (QuizAttemptService.review), which replays the attempt's shuffled order.
	 */
	@Test
	void getAttemptDetailListsQuestionsInQuizSortOrderEvenWhenTheQuizIsRandomized() {
		quiz.setRandomize(true);
		givenAttempt(submittedAttempt("[]"));
		givenVisibleAttempt();
		List<Long> sortOrderIds = List.of(SECOND_QUESTION_ID, FIRST_QUESTION_ID, THIRD_QUESTION_ID);
		// Some seeds leave a short list unchanged (seed 900 keeps two items in place), which would let a
		// shuffling implementation pass unnoticed; guard that the attempt's seed really reorders this fixture.
		List<Long> attemptSeededOrder = new ArrayList<>(sortOrderIds);
		Collections.shuffle(attemptSeededOrder, new Random(ATTEMPT_ID));
		assertThat(attemptSeededOrder).isNotEqualTo(sortOrderIds);
		givenQuestions(
				quizQuestion(SECOND_QUESTION_ID, 1, "[\"B\"]"),
				quizQuestion(FIRST_QUESTION_ID, 2, "[\"A\"]"),
				quizQuestion(THIRD_QUESTION_ID, 3, "[\"C\"]"));

		QuizAttemptReviewResponse review = service.getAttemptDetail(QUIZ_ID, ATTEMPT_ID, principal(RoleNames.TRAINER));

		assertThat(review.questions())
				.extracting(QuizAttemptReviewQuestionResponse::questionId)
				.containsExactlyElementsOf(sortOrderIds);
		assertThat(review.questions())
				.extracting(QuizAttemptReviewQuestionResponse::sortOrder)
				.containsExactly(1, 2, 3);
	}

	@Test
	void getAttemptDetailTreatsAnUnansweredQuestionAsEmptyAndWrong() {
		givenAttempt(submittedAttempt("[{\"questionId\":501,\"selectedAnswersJson\":[\"A\"]}]"));
		givenVisibleAttempt();
		givenQuestions(
				quizQuestion(FIRST_QUESTION_ID, 1, "[\"A\"]"),
				quizQuestion(SECOND_QUESTION_ID, 2, "[\"B\"]"));

		QuizAttemptReviewResponse review = service.getAttemptDetail(QUIZ_ID, ATTEMPT_ID, principal(RoleNames.TRAINER));

		QuizAttemptReviewQuestionResponse answered = review.questions().get(0);
		assertThat(answered.selectedAnswersJson().toString()).isEqualTo("[\"A\"]");
		assertThat(answered.correct()).isTrue();
		QuizAttemptReviewQuestionResponse unanswered = review.questions().get(1);
		assertThat(unanswered.selectedAnswersJson().toString()).isEqualTo("[]");
		assertThat(unanswered.correct()).isFalse();
	}

	@ParameterizedTest(name = "selected {0} against correct [\"A\",\"C\"] is correct={1}")
	@CsvSource(delimiter = '|', value = {
			"[\"C\",\"A\"]     | true",
			"[\"A\",\"C\"]     | true",
			"[\"A\"]           | false",
			"[\"A\",\"B\",\"C\"] | false",
			"[]                | false"
	})
	void getAttemptDetailComparesMultipleChoiceAnswersAsASet(String selected, boolean expectedCorrect) {
		givenAttempt(submittedAttempt("[{\"questionId\":501,\"selectedAnswersJson\":" + selected + "}]"));
		givenVisibleAttempt();
		QuizQuestion multiple = quizQuestion(FIRST_QUESTION_ID, 1, "[\"A\",\"C\"]");
		multiple.getQuestion().setQuestionType(QuestionType.multiple);
		givenQuestions(multiple);

		QuizAttemptReviewResponse review = service.getAttemptDetail(QUIZ_ID, ATTEMPT_ID, principal(RoleNames.TRAINER));

		assertThat(review.questions()).singleElement()
				.extracting(QuizAttemptReviewQuestionResponse::correct)
				.isEqualTo(expectedCorrect);
	}

	@Test
	void getAttemptDetailReturnsTheAttemptOutcomeAndQuestionContent() {
		String answersJson = "[{\"questionId\":501,\"selectedAnswersJson\":[\"A\"]}]";
		QuizAttempt attempt = submittedAttempt(answersJson);
		attempt.setScore(100);
		attempt.setCorrectCount(1);
		attempt.setTotalQuestions(1);
		attempt.setPassed(true);
		attempt.setTimeTakenSeconds(720);
		givenAttempt(attempt);
		givenVisibleAttempt();
		QuizQuestion quizQuestion = quizQuestion(FIRST_QUESTION_ID, 1, "[\"A\"]");
		quizQuestion.setPoints(BigDecimal.valueOf(2));
		givenQuestions(quizQuestion);

		QuizAttemptReviewResponse review = service.getAttemptDetail(QUIZ_ID, ATTEMPT_ID, principal(RoleNames.CLASS_ADMIN));

		assertThat(review.id()).isEqualTo(ATTEMPT_ID);
		assertThat(review.quizId()).isEqualTo(QUIZ_ID);
		assertThat(review.quizTitle()).isEqualTo("Java basics");
		assertThat(review.attemptNumber()).isEqualTo(1);
		assertThat(review.status()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(review.answersJson().toString()).isEqualTo(answersJson);
		assertThat(review.score()).isEqualTo(100);
		assertThat(review.correctCount()).isEqualTo(1);
		assertThat(review.totalQuestions()).isEqualTo(1);
		assertThat(review.passed()).isTrue();
		assertThat(review.timeTakenSeconds()).isEqualTo(720);
		assertThat(review.startedAt()).isEqualTo(STARTED_AT);
		assertThat(review.submittedAt()).isEqualTo(SUBMITTED_AT);
		assertThat(review.questions()).singleElement().satisfies(question -> {
			assertThat(question.questionId()).isEqualTo(FIRST_QUESTION_ID);
			assertThat(question.points()).isEqualByComparingTo("2");
			assertThat(question.content()).isEqualTo("Question 501");
			assertThat(question.questionType()).isEqualTo(QuestionType.single);
			assertThat(question.category()).isEqualTo("Java");
			assertThat(question.difficulty()).isEqualTo(QuestionDifficulty.Easy);
			assertThat(question.optionsJson().toString()).isEqualTo("[\"A\",\"B\",\"C\"]");
			assertThat(question.correctAnswersJson().toString()).isEqualTo("[\"A\"]");
			assertThat(question.explanation()).isEqualTo("Because 501");
		});
	}

	/** Corrupt stored JSON is a server fault, not a business error, so it surfaces as a 500. */
	@Test
	void getAttemptDetailFailsLoudlyOnCorruptStoredAnswers() {
		givenAttempt(submittedAttempt("not json"));
		givenVisibleAttempt();
		givenQuestions(quizQuestion(FIRST_QUESTION_ID, 1, "[\"A\"]"));

		assertThatThrownBy(() -> service.getAttemptDetail(QUIZ_ID, ATTEMPT_ID, principal(RoleNames.SUPER_ADMIN)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Stored quiz attempt answers are invalid");
	}

	@Test
	void summaryReportsUnknownQuizBeforeTheRoleCheck() {
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.summary(QUIZ_ID, null, null, principal(RoleNames.TRAINEE)))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz not found");
		verify(quizAttemptRepository, never()).summarizeQuizResults(any(), any(), any(), anyBoolean(), any(), any());
	}

	@Test
	void summaryForbidsTrainee() {
		assertThatThrownBy(() -> service.summary(QUIZ_ID, CLASS_ID, null, principal(RoleNames.TRAINEE)))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("You cannot view quiz results")
				.extracting("code")
				.isEqualTo("ACCESS_DENIED");
		verifyNoInteractions(classAccessService);
		verify(quizAttemptRepository, never()).summarizeQuizResults(any(), any(), any(), anyBoolean(), any(), any());
	}

	@Test
	void summaryChecksBothFiltersAndScopesTheAggregate() {
		FapUserPrincipal principal = principal(RoleNames.CLASS_ADMIN);
		givenStats(new QuizAttemptStats(0L, 0L, 0L, 0L, null, null, null));

		service.summary(QUIZ_ID, CLASS_ID, SESSION_ID, principal);

		verify(classAccessService).assertCanViewClass(principal, CLASS_ID);
		verify(classAccessService).assertCanViewSession(principal, SESSION_ID);
		verify(quizAttemptRepository).summarizeQuizResults(
				QUIZ_ID, CLASS_ID, SESSION_ID, false, VIEWER_ID, ELIGIBLE_STATUSES);
	}

	@Test
	void summaryLetsSuperAdminAggregateEveryAttempt() {
		givenStats(new QuizAttemptStats(0L, 0L, 0L, 0L, null, null, null));

		service.summary(QUIZ_ID, null, null, principal(RoleNames.SUPER_ADMIN));

		verifyNoInteractions(classAccessService);
		verify(quizAttemptRepository).summarizeQuizResults(QUIZ_ID, null, null, true, VIEWER_ID, ELIGIBLE_STATUSES);
	}

	@Test
	void summaryStopsWhenTheSessionFilterIsNotAccessible() {
		FapUserPrincipal principal = principal(RoleNames.TRAINER);
		doThrow(new ForbiddenException("You cannot view this training session"))
				.when(classAccessService).assertCanViewSession(principal, SESSION_ID);

		assertThatThrownBy(() -> service.summary(QUIZ_ID, null, SESSION_ID, principal))
				.isInstanceOf(ForbiddenException.class)
				.hasMessage("You cannot view this training session");
		verify(quizAttemptRepository, never()).summarizeQuizResults(any(), any(), any(), anyBoolean(), any(), any());
	}

	@Test
	void summaryDerivesFailedAttemptsAndPassRateFromTheAggregate() {
		givenStats(new QuizAttemptStats(4L, 1L, 3L, 2L, 71.5, 90, 40));

		QuizAttemptSummaryResponse summary = service.summary(QUIZ_ID, null, null, principal(RoleNames.SUPER_ADMIN));

		assertThat(summary.quizId()).isEqualTo(QUIZ_ID);
		assertThat(summary.quizTitle()).isEqualTo("Java basics");
		assertThat(summary.totalAttempts()).isEqualTo(4);
		assertThat(summary.inProgressAttempts()).isEqualTo(1);
		assertThat(summary.submittedAttempts()).isEqualTo(3);
		assertThat(summary.passedAttempts()).isEqualTo(2);
		assertThat(summary.failedAttempts()).isEqualTo(1);
		assertThat(summary.passRate()).isEqualTo(66.67);
		assertThat(summary.averageScore()).isEqualTo(71.5);
		assertThat(summary.highestScore()).isEqualTo(90);
		assertThat(summary.lowestScore()).isEqualTo(40);
	}

	/** Two decimals, HALF_UP: 1/32 = 3.125% must show as 3.13, not the banker's 3.12. */
	@ParameterizedTest(name = "{0} passed of {1} submitted -> {2}%")
	@CsvSource({
			"2, 3, 66.67",
			"1, 6, 16.67",
			"1, 32, 3.13",
			"3, 32, 9.38",
			"3, 3, 100.0",
			"0, 5, 0.0"
	})
	void summaryRoundsThePassRateHalfUpToTwoDecimals(long passed, long submitted, double expectedPassRate) {
		givenStats(new QuizAttemptStats(submitted, 0L, submitted, passed, 50.0, 100, 0));

		QuizAttemptSummaryResponse summary = service.summary(QUIZ_ID, null, null, principal(RoleNames.SUPER_ADMIN));

		assertThat(summary.passRate()).isEqualTo(expectedPassRate);
		assertThat(summary.failedAttempts()).isEqualTo(submitted - passed);
	}

	@Test
	void summaryReportsZeroPassRateAndNoScoresWhenNothingWasSubmitted() {
		givenStats(new QuizAttemptStats(2L, 2L, 0L, 0L, null, null, null));

		QuizAttemptSummaryResponse summary = service.summary(QUIZ_ID, null, null, principal(RoleNames.TRAINER));

		assertThat(summary.totalAttempts()).isEqualTo(2);
		assertThat(summary.inProgressAttempts()).isEqualTo(2);
		assertThat(summary.submittedAttempts()).isZero();
		assertThat(summary.failedAttempts()).isZero();
		assertThat(summary.passRate()).isZero();
		assertThat(summary.averageScore()).isNull();
		assertThat(summary.highestScore()).isNull();
		assertThat(summary.lowestScore()).isNull();
	}

	private Page<QuizAttemptResultResponse> listAttempts(FapUserPrincipal principal, Long classId, Long sessionId) {
		return service.listAttempts(QUIZ_ID, null, null, null, classId, sessionId, principal, 0, 20);
	}

	private Pageable capturePageable() {
		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(quizAttemptRepository).searchQuizResults(
				any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), captor.capture());
		return captor.getValue();
	}

	private void verifySearchNeverCalled() {
		verify(quizAttemptRepository, never()).searchQuizResults(
				any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any());
	}

	private void givenAttempt(QuizAttempt attempt) {
		when(quizAttemptRepository.findByQuizIdAndId(QUIZ_ID, ATTEMPT_ID)).thenReturn(Optional.of(attempt));
	}

	private void givenVisibleAttempt() {
		when(quizAttemptRepository.countVisibleQuizResult(
				eq(QUIZ_ID), eq(ATTEMPT_ID), anyBoolean(), any(), any())).thenReturn(1L);
	}

	private void givenQuestions(QuizQuestion... quizQuestions) {
		when(quizQuestionRepository.findByIdQuizIdOrderBySortOrderAsc(QUIZ_ID)).thenReturn(Arrays.asList(quizQuestions));
	}

	private void givenStats(QuizAttemptStats stats) {
		when(quizAttemptRepository.summarizeQuizResults(any(), any(), any(), anyBoolean(), any(), any()))
				.thenReturn(stats);
	}

	private QuizAttempt submittedAttempt(String answersJson) {
		User trainee = new User();
		trainee.setId(TRAINEE_ID);
		trainee.setFullName("Trainee One");
		trainee.setEmail("trainee@fap.local");
		QuizAttempt attempt = new QuizAttempt();
		attempt.setId(ATTEMPT_ID);
		attempt.setQuiz(quiz);
		attempt.setUser(trainee);
		attempt.setAttemptNumber(1);
		attempt.setStatus(QuizAttemptStatus.Submitted);
		attempt.setAnswersJson(answersJson);
		attempt.setStartedAt(STARTED_AT);
		attempt.setSubmittedAt(SUBMITTED_AT);
		return attempt;
	}

	private QuizQuestion quizQuestion(long questionId, int sortOrder, String correctAnswersJson) {
		Question question = new Question();
		question.setId(questionId);
		question.setContent("Question " + questionId);
		question.setQuestionType(QuestionType.single);
		question.setCategory("Java");
		question.setDifficulty(QuestionDifficulty.Easy);
		question.setOptionsJson("[\"A\",\"B\",\"C\"]");
		question.setCorrectAnswersJson(correctAnswersJson);
		question.setExplanation("Because " + questionId);
		QuizQuestion quizQuestion = new QuizQuestion();
		quizQuestion.setId(new QuizQuestionId(QUIZ_ID, questionId));
		quizQuestion.setQuiz(quiz);
		quizQuestion.setQuestion(question);
		quizQuestion.setSortOrder(sortOrder);
		return quizQuestion;
	}

	private static FapUserPrincipal principal(String role) {
		return new FapUserPrincipal(VIEWER_ID, "user" + VIEWER_ID + "@fap.local", "", Set.of(role), true, List.of());
	}
}
