package com.fap.quiz.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.quiz.dto.UpdateQuizStatusRequest;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.mapper.QuizMapper;
import com.fap.quiz.repository.QuestionRepository;
import com.fap.quiz.repository.QuizQuestionRepository;
import com.fap.quiz.repository.QuizRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the quiz lifecycle: Draft -> Published -> Closed, one way only. A quiz with no questions
 * must never become visible to trainees.
 */
class QuizStateMachineTest {

	private static final long QUIZ_ID = 31L;
	private static final long CURRENT_USER_ID = 7L;

	private final QuizRepository quizRepository = mock(QuizRepository.class);
	private final QuizQuestionRepository quizQuestionRepository = mock(QuizQuestionRepository.class);
	private final QuestionRepository questionRepository = mock(QuestionRepository.class);
	private final QuizMapper quizMapper = mock(QuizMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);

	private final QuizService service = new QuizService(
			quizRepository,
			quizQuestionRepository,
			questionRepository,
			quizMapper,
			auditLogService);

	@ParameterizedTest(name = "{0} -> {1} is allowed")
	@CsvSource({
			"Draft, Published",
			"Published, Closed"
	})
	void allowsValidTransition(QuizStatus current, QuizStatus target) {
		Quiz quiz = givenQuiz(current);
		when(quizQuestionRepository.countByIdQuizId(QUIZ_ID)).thenReturn(5L);

		service.updateStatus(QUIZ_ID, new UpdateQuizStatusRequest(target), CURRENT_USER_ID);

		assertThat(quiz.getStatus()).isEqualTo(target);
		assertThat(quiz.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		verify(auditLogService).record("UPDATE_QUIZ_STATUS:" + target.name(), "quiz", QUIZ_ID);
	}

	@ParameterizedTest(name = "{0} -> {1} is rejected")
	@CsvSource({
			"Draft, Closed",
			"Published, Draft",
			"Closed, Draft",
			"Closed, Published"
	})
	void rejectsInvalidTransition(QuizStatus current, QuizStatus target) {
		Quiz quiz = givenQuiz(current);
		when(quizQuestionRepository.countByIdQuizId(QUIZ_ID)).thenReturn(5L);

		assertThatThrownBy(() ->
				service.updateStatus(QUIZ_ID, new UpdateQuizStatusRequest(target), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("INVALID_QUIZ_STATUS_TRANSITION");

		assertThat(quiz.getStatus()).isEqualTo(current);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@ParameterizedTest(name = "{0} -> {0} is a no-op")
	@CsvSource({"Draft", "Published", "Closed"})
	void allowsTransitionToSameStatus(QuizStatus current) {
		Quiz quiz = givenQuiz(current);
		when(quizQuestionRepository.countByIdQuizId(QUIZ_ID)).thenReturn(5L);

		service.updateStatus(QUIZ_ID, new UpdateQuizStatusRequest(current), CURRENT_USER_ID);

		assertThat(quiz.getStatus()).isEqualTo(current);
	}

	@Test
	void rejectsPublishingQuizWithoutQuestions() {
		givenQuiz(QuizStatus.Draft);
		when(quizQuestionRepository.countByIdQuizId(QUIZ_ID)).thenReturn(0L);

		assertThatThrownBy(() -> service.updateStatus(
				QUIZ_ID, new UpdateQuizStatusRequest(QuizStatus.Published), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("QUIZ_QUESTION_REQUIRED");
	}

	@Test
	void closingSkipsTheQuestionCountGuard() {
		Quiz quiz = givenQuiz(QuizStatus.Published);
		when(quizQuestionRepository.countByIdQuizId(QUIZ_ID)).thenReturn(0L);

		service.updateStatus(QUIZ_ID, new UpdateQuizStatusRequest(QuizStatus.Closed), CURRENT_USER_ID);

		assertThat(quiz.getStatus()).isEqualTo(QuizStatus.Closed);
	}

	/** No messages.properties key exists for these codes, so the Java message is the HTTP message. */
	@Test
	void rejectedTransitionKeepsItsClientVisibleMessageAndSkipsTheQuestionCount() {
		givenQuiz(QuizStatus.Closed);

		assertThatThrownBy(() -> service.updateStatus(
				QUIZ_ID, new UpdateQuizStatusRequest(QuizStatus.Published), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Invalid quiz status transition");

		verify(quizQuestionRepository, never()).countByIdQuizId(anyLong());
	}

	@Test
	void publishingWithoutQuestionsKeepsItsClientVisibleMessage() {
		givenQuiz(QuizStatus.Draft);
		when(quizQuestionRepository.countByIdQuizId(QUIZ_ID)).thenReturn(0L);

		assertThatThrownBy(() -> service.updateStatus(
				QUIZ_ID, new UpdateQuizStatusRequest(QuizStatus.Published), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Quiz requires at least one question before publishing");
	}

	@Test
	void rejectsMissingTargetStatusAsInvalidTransition() {
		Quiz quiz = givenQuiz(QuizStatus.Draft);

		assertThatThrownBy(() -> service.updateStatus(QUIZ_ID, new UpdateQuizStatusRequest(null), CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("INVALID_QUIZ_STATUS_TRANSITION");

		assertThat(quiz.getStatus()).isEqualTo(QuizStatus.Draft);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	private Quiz givenQuiz(QuizStatus status) {
		Quiz quiz = new Quiz();
		quiz.setId(QUIZ_ID);
		quiz.setStatus(status);
		lenient().doCallRealMethod().when(quizRepository).getQuizOrThrow(any());
		when(quizRepository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));
		return quiz;
	}
}
