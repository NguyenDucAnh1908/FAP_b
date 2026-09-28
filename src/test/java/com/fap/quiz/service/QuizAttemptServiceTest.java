package com.fap.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.metrics.DomainMetrics;
import com.fap.quiz.dto.QuizAttemptResponse;
import com.fap.quiz.entity.Question;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.entity.QuizQuestion;
import com.fap.quiz.entity.QuizQuestionId;
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

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Time-dependent attempt rules (open/close window, start and submit stamps, expiry auto-submit)
 * against a fixed clock, so the boundaries are exact instead of relative to the real date.
 */
class QuizAttemptServiceTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 9, 0);
	private static final LocalDate TODAY = NOW.toLocalDate();
	private static final long QUIZ_ID = 31L;
	private static final long QUESTION_ID = 501L;
	private static final long ATTEMPT_ID = 900L;
	private static final long CURRENT_USER_ID = 7L;
	private static final int DURATION_MINUTES = 30;
	private static final String CORRECT_ANSWERS_JSON = "[{\"questionId\":501,\"selectedAnswersJson\":[\"A\"]}]";

	private final QuizRepository quizRepository = mock(QuizRepository.class);
	private final QuizAssignmentRepository quizAssignmentRepository = mock(QuizAssignmentRepository.class);
	private final QuizQuestionRepository quizQuestionRepository = mock(QuizQuestionRepository.class);
	private final QuizAttemptRepository quizAttemptRepository = mock(QuizAttemptRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

	private final QuizAttemptService service = new QuizAttemptService(
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

	private Quiz quiz;

	@BeforeEach
	void setUp() {
		doCallRealMethod().when(quizRepository).getQuizOrThrow(any());
		doCallRealMethod().when(quizAttemptRepository).getByIdAndUserIdOrThrow(any(), any());
		quiz = new Quiz();
		quiz.setId(QUIZ_ID);
		quiz.setTitle("Java basics");
		quiz.setStatus(QuizStatus.Published);
		quiz.setDurationMinutes(DURATION_MINUTES);
		quiz.setPassingScore(50);
		quiz.setMaxAttempts(2);
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
		when(quizQuestionRepository.findByIdQuizIdOrderBySortOrderAsc(QUIZ_ID)).thenReturn(List.of(quizQuestion()));
	}

	@Test
	void startIsAllowedOnTheOpenAndCloseDatesAndStampsTheClockTime() {
		quiz.setOpenDate(TODAY);
		quiz.setCloseDate(TODAY);
		when(quizAssignmentRepository.countEligibleAssignments(any(), any(), any())).thenReturn(1L);
		when(quizAttemptRepository.save(any(QuizAttempt.class))).thenAnswer(invocation -> {
			QuizAttempt saved = invocation.getArgument(0);
			saved.setId(ATTEMPT_ID);
			return saved;
		});

		QuizAttemptResponse response = service.start(QUIZ_ID, CURRENT_USER_ID);

		assertThat(response.status()).isEqualTo(QuizAttemptStatus.InProgress);
		assertThat(response.attemptNumber()).isEqualTo(1);
		assertThat(response.startedAt()).isEqualTo(NOW);
		verify(auditLogService).record("START_QUIZ_ATTEMPT", "quiz_attempt", ATTEMPT_ID);
	}

	@Test
	void startIsRejectedBeforeTheOpenDate() {
		quiz.setOpenDate(TODAY.plusDays(1));

		assertThatThrownBy(() -> service.start(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("QUIZ_NOT_OPEN");
		verify(quizAttemptRepository, never()).save(any());
	}

	@Test
	void startIsRejectedAfterTheCloseDate() {
		quiz.setCloseDate(TODAY.minusDays(1));

		assertThatThrownBy(() -> service.start(QUIZ_ID, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("QUIZ_CLOSED");
		verify(quizAttemptRepository, never()).save(any());
	}

	@Test
	void submitStampsTheClockTimeAndMeasuresTimeTaken() {
		QuizAttempt attempt = givenAttempt(NOW.minusMinutes(5), CORRECT_ANSWERS_JSON);

		QuizAttemptResponse response = service.submit(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(response.status()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(response.score()).isEqualTo(100);
		assertThat(response.passed()).isTrue();
		assertThat(attempt.getSubmittedAt()).isEqualTo(NOW);
		assertThat(attempt.getTimeTakenSeconds()).isEqualTo(300);
		verify(auditLogService).record("SUBMIT_QUIZ_ATTEMPT", "quiz_attempt", ATTEMPT_ID);
	}

	@Test
	void expiredAttemptIsAutoSubmittedAtItsDeadlineWithTheStoredAnswers() {
		LocalDateTime startedAt = NOW.minusMinutes(DURATION_MINUTES + 1);
		QuizAttempt attempt = givenAttempt(startedAt, CORRECT_ANSWERS_JSON);

		service.get(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(attempt.getScore()).isEqualTo(100);
		assertThat(attempt.getSubmittedAt()).isEqualTo(startedAt.plusMinutes(DURATION_MINUTES));
		assertThat(attempt.getTimeTakenSeconds()).isEqualTo(DURATION_MINUTES * 60);
		verify(auditLogService).record("AUTO_SUBMIT_QUIZ_ATTEMPT_EXPIRED", "quiz_attempt", ATTEMPT_ID);
	}

	@Test
	void attemptIsStillOpenAtExactlyItsDeadline() {
		QuizAttempt attempt = givenAttempt(NOW.minusMinutes(DURATION_MINUTES), "[]");

		service.get(ATTEMPT_ID, CURRENT_USER_ID);

		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.InProgress);
		assertThat(attempt.getSubmittedAt()).isNull();
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@Test
	void attemptOfAnotherUserIsNotFound() {
		when(quizAttemptRepository.findByIdAndUserId(ATTEMPT_ID, CURRENT_USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(ATTEMPT_ID, CURRENT_USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz attempt not found");
	}

	private QuizAttempt givenAttempt(LocalDateTime startedAt, String answersJson) {
		QuizAttempt attempt = new QuizAttempt();
		attempt.setId(ATTEMPT_ID);
		attempt.setQuiz(quiz);
		attempt.setAttemptNumber(1);
		attempt.setStatus(QuizAttemptStatus.InProgress);
		attempt.setAnswersJson(answersJson);
		attempt.setStartedAt(startedAt);
		when(quizAttemptRepository.findByIdAndUserId(ATTEMPT_ID, CURRENT_USER_ID)).thenReturn(Optional.of(attempt));
		return attempt;
	}

	private QuizQuestion quizQuestion() {
		Question question = new Question();
		question.setId(QUESTION_ID);
		question.setContent("Pick A");
		question.setQuestionType(QuestionType.single);
		question.setOptionsJson("[\"A\",\"B\"]");
		question.setCorrectAnswersJson("[\"A\"]");
		QuizQuestion quizQuestion = new QuizQuestion();
		quizQuestion.setId(new QuizQuestionId(QUIZ_ID, QUESTION_ID));
		quizQuestion.setQuiz(quiz);
		quizQuestion.setQuestion(question);
		quizQuestion.setSortOrder(1);
		return quizQuestion;
	}
}
