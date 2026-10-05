package com.fap.quiz.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.quiz.entity.Question;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAssignment;
import com.fap.quiz.entity.QuizAttempt;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The lookup defaults replace per-service helpers, so each must keep the exact not-found message
 * the services used to throw.
 */
class QuizLookupDefaultsTest {

	private static final long QUIZ_ID = 31L;
	private static final long ID = 900L;
	private static final long USER_ID = 7L;

	@Test
	void quizLookupReturnsTheQuizOrIsNotFound() {
		QuizRepository repository = mock(QuizRepository.class);
		doCallRealMethod().when(repository).getQuizOrThrow(any());
		Quiz quiz = new Quiz();
		when(repository.findById(QUIZ_ID)).thenReturn(Optional.of(quiz));

		assertThat(repository.getQuizOrThrow(QUIZ_ID)).isSameAs(quiz);
		assertThatThrownBy(() -> repository.getQuizOrThrow(ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz not found");
	}

	@Test
	void questionLookupReturnsTheQuestionOrIsNotFound() {
		QuestionRepository repository = mock(QuestionRepository.class);
		doCallRealMethod().when(repository).getQuestionOrThrow(any());
		Question question = new Question();
		when(repository.findById(ID)).thenReturn(Optional.of(question));

		assertThat(repository.getQuestionOrThrow(ID)).isSameAs(question);
		assertThatThrownBy(() -> repository.getQuestionOrThrow(QUIZ_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Question not found");
	}

	@Test
	void assignmentLookupIsScopedToTheQuiz() {
		QuizAssignmentRepository repository = mock(QuizAssignmentRepository.class);
		doCallRealMethod().when(repository).getByQuizIdAndIdOrThrow(any(), any());
		QuizAssignment assignment = new QuizAssignment();
		when(repository.findByQuizIdAndId(QUIZ_ID, ID)).thenReturn(Optional.of(assignment));

		assertThat(repository.getByQuizIdAndIdOrThrow(QUIZ_ID, ID)).isSameAs(assignment);
		assertThatThrownBy(() -> repository.getByQuizIdAndIdOrThrow(QUIZ_ID + 1, ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz assignment not found");
	}

	@Test
	void ownAttemptLookupIsScopedToTheUser() {
		QuizAttemptRepository repository = mock(QuizAttemptRepository.class);
		doCallRealMethod().when(repository).getByIdAndUserIdOrThrow(any(), any());
		QuizAttempt attempt = new QuizAttempt();
		when(repository.findByIdAndUserId(ID, USER_ID)).thenReturn(Optional.of(attempt));

		assertThat(repository.getByIdAndUserIdOrThrow(ID, USER_ID)).isSameAs(attempt);
		assertThatThrownBy(() -> repository.getByIdAndUserIdOrThrow(ID, USER_ID + 1))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz attempt not found");
	}

	@Test
	void quizAttemptLookupIsScopedToTheQuiz() {
		QuizAttemptRepository repository = mock(QuizAttemptRepository.class);
		doCallRealMethod().when(repository).getByQuizIdAndIdOrThrow(any(), any());
		QuizAttempt attempt = new QuizAttempt();
		when(repository.findByQuizIdAndId(QUIZ_ID, ID)).thenReturn(Optional.of(attempt));

		assertThat(repository.getByQuizIdAndIdOrThrow(QUIZ_ID, ID)).isSameAs(attempt);
		assertThatThrownBy(() -> repository.getByQuizIdAndIdOrThrow(QUIZ_ID + 1, ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Quiz attempt not found");
	}
}
