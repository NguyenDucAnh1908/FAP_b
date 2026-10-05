package com.fap.quiz.service;

import com.fap.common.exception.ConflictException;
import com.fap.quiz.dto.QuizAttemptResponse;
import com.fap.quiz.entity.Quiz;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.enums.QuizAttemptStatus;
import com.fap.quiz.enums.QuizStatus;
import com.fap.support.AbstractOracleIT;
import com.fap.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Pins the transactional contract of the closed-quiz rule, which a Mockito test cannot: the 409
 * raised by {@code submit}/{@code saveAnswers} must not roll back the auto-submit that precedes it,
 * otherwise the attempt would stay InProgress and every retry would fail the same way.
 */
class QuizAttemptClosedQuizIT extends AbstractOracleIT {

	@Autowired
	private QuizAttemptService quizAttemptService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private Long attemptId;
	private Long userId;

	@BeforeEach
	void startAttemptOnAQuizThatIsThenClosed() {
		List<Long> quizIds = entityManager.createQuery("""
						select qq.id.quizId from QuizQuestion qq
						group by qq.id.quizId
						order by count(qq) desc, qq.id.quizId
						""", Long.class)
				.setMaxResults(1)
				.getResultList();
		assumeThat(quizIds).as("seed data has a quiz with questions").isNotEmpty();
		Quiz quiz = entityManager.find(Quiz.class, quizIds.get(0));
		userId = entityManager.createQuery("select u.id from User u order by u.id", Long.class)
				.setMaxResults(1)
				.getSingleResult();
		Integer lastAttempt = entityManager.createQuery("""
						select coalesce(max(a.attemptNumber), 0) from QuizAttempt a
						where a.quiz.id = :quizId and a.user.id = :userId
						""", Integer.class)
				.setParameter("quizId", quiz.getId())
				.setParameter("userId", userId)
				.getSingleResult();

		QuizAttempt attempt = new QuizAttempt();
		attempt.setQuiz(quiz);
		attempt.setUser(entityManager.getReference(User.class, userId));
		attempt.setAttemptNumber(lastAttempt + 1);
		attempt.setStatus(QuizAttemptStatus.InProgress);
		attempt.setAnswersJson("[]");
		attempt.setStartedAt(LocalDateTime.now());
		entityManager.persist(attempt);
		attemptId = attempt.getId();
		// Closed while the attempt is running; the test transaction rolls this back afterwards.
		quiz.setStatus(QuizStatus.Closed);
		flushAndClear();
	}

	@Test
	void submitOnClosedQuizRejectsButKeepsTheAutoSubmit() {
		// Call through a participating transaction so its status shows whether the service's 409
		// marked the shared transaction rollback-only, which would discard the auto-submit and its
		// audit row together with the error.
		TransactionTemplate participating = new TransactionTemplate(transactionManager);
		Boolean rollbackOnly = participating.execute(status -> {
			assertThatThrownBy(() -> quizAttemptService.submit(attemptId, userId))
					.isInstanceOf(ConflictException.class)
					.extracting("code")
					.isEqualTo("QUIZ_CLOSED");
			return status.isRollbackOnly();
		});

		assertThat(rollbackOnly).isFalse();
		flushAndClear();
		QuizAttempt attempt = entityManager.find(QuizAttempt.class, attemptId);
		assertThat(attempt.getStatus()).isEqualTo(QuizAttemptStatus.Submitted);
		assertThat(attempt.getSubmittedAt()).isNotNull();
		assertThat(attempt.getScore()).isNotNull();
		Long audits = entityManager.createQuery("""
						select count(a) from AuditLog a
						where a.action = 'AUTO_SUBMIT_QUIZ_CLOSED' and a.entityId = :attemptId
						""", Long.class)
				.setParameter("attemptId", attemptId)
				.getSingleResult();
		assertThat(audits).isEqualTo(1);
	}

	@Test
	void getOnClosedQuizReturnsTheAutoSubmittedAttempt() {
		QuizAttemptResponse response = quizAttemptService.get(attemptId, userId);

		assertThat(response.status()).isEqualTo(QuizAttemptStatus.Submitted);
		flushAndClear();
		assertThat(entityManager.find(QuizAttempt.class, attemptId).getStatus()).isEqualTo(QuizAttemptStatus.Submitted);
	}
}
