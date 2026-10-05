package com.fap.quiz.repository;

import com.fap.common.security.FapUserPrincipal;
import com.fap.common.security.RoleNames;
import com.fap.quiz.dto.QuizAttemptResultResponse;
import com.fap.quiz.dto.QuizAttemptSummaryResponse;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.enums.QuizAttemptStatus;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.service.QuizResultService;
import com.fap.support.AbstractOracleIT;
import com.fap.support.ItPrincipals;
import com.fap.training.enums.TrainingRegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Runs the quiz result/assignment queries on Oracle. Several of them select whole entities that
 * carry CLOB columns, which Oracle rejects in some SQL shapes (e.g. DISTINCT), so a mocked
 * repository cannot prove they work.
 */
class QuizQueriesIT extends AbstractOracleIT {

	private static final List<TrainingRegistrationStatus> ELIGIBLE = List.of(
			TrainingRegistrationStatus.Registered,
			TrainingRegistrationStatus.Completed);

	@Autowired
	private QuizResultService quizResultService;

	@Autowired
	private QuizRepository quizRepository;

	private Long quizId;
	private FapUserPrincipal superAdmin;

	@BeforeEach
	void pickQuizWithMostAttempts() {
		List<Long> quizIds = entityManager.createQuery("""
						select a.quiz.id from QuizAttempt a
						group by a.quiz.id
						order by count(a) desc, a.quiz.id
						""", Long.class)
				.setMaxResults(1)
				.getResultList();
		assumeThat(quizIds).as("seed data contains quiz attempts").isNotEmpty();
		quizId = quizIds.get(0);
		Long adminId = entityManager.createQuery(
						"select u.id from User u join u.roles r where r.name = :roleName order by u.id", Long.class)
				.setParameter("roleName", RoleNames.SUPER_ADMIN)
				.setMaxResults(1)
				.getSingleResult();
		superAdmin = ItPrincipals.superAdmin(adminId);
	}

	@Test
	void listAttemptsRunsForSuperAdminAndScopedViewer() {
		Page<QuizAttemptResultResponse> all = quizResultService.listAttempts(
				quizId, null, null, null, null, null, superAdmin, 0, 20);
		FapUserPrincipal trainer = new FapUserPrincipal(
				superAdmin.id(), "it-trainer@fap.local", "", Set.of(RoleNames.TRAINER), true, List.of());
		Page<QuizAttemptResultResponse> scoped = quizResultService.listAttempts(
				quizId, QuizAttemptStatus.Submitted, null, null, null, null, trainer, 0, 20);

		assertThat(all.getTotalElements()).isEqualTo(attemptsOfQuiz().size());
		assertThat(scoped.getTotalElements()).isLessThanOrEqualTo(all.getTotalElements());
	}

	@Test
	void summaryMatchesTheAttemptsOfTheQuiz() {
		List<QuizAttempt> attempts = attemptsOfQuiz();
		List<Integer> submittedScores = attempts.stream()
				.filter(attempt -> attempt.getStatus() == QuizAttemptStatus.Submitted)
				.map(QuizAttempt::getScore)
				.filter(Objects::nonNull)
				.toList();
		long submitted = attempts.stream().filter(a -> a.getStatus() == QuizAttemptStatus.Submitted).count();
		long passed = attempts.stream().filter(a -> Boolean.TRUE.equals(a.getPassed())).count();

		QuizAttemptSummaryResponse summary = quizResultService.summary(quizId, null, null, superAdmin);

		assertThat(summary.totalAttempts()).isEqualTo(attempts.size());
		assertThat(summary.submittedAttempts()).isEqualTo(submitted);
		assertThat(summary.inProgressAttempts()).isEqualTo(attempts.size() - submitted);
		assertThat(summary.passedAttempts()).isEqualTo(passed);
		assertThat(summary.failedAttempts()).isEqualTo(submitted - passed);
		if (submittedScores.isEmpty()) {
			assertThat(summary.averageScore()).isNull();
			assertThat(summary.highestScore()).isNull();
		} else {
			assertThat(summary.averageScore())
					.isCloseTo(submittedScores.stream().mapToInt(Integer::intValue).average().orElseThrow(), within(0.001));
			assertThat(summary.highestScore()).isEqualTo(submittedScores.stream().max(Integer::compare).orElseThrow());
			assertThat(summary.lowestScore()).isEqualTo(submittedScores.stream().min(Integer::compare).orElseThrow());
		}
	}

	/** Regression guard: the summary is an aggregate and must not materialize attempts (and their CLOBs). */
	@Test
	void summaryDoesNotLoadAttempts() {
		Measured<QuizAttemptSummaryResponse> measured = measure(
				() -> quizResultService.summary(quizId, null, null, superAdmin));

		assertThat(measured.result().totalAttempts()).isPositive();
		// The quiz itself only.
		assertThat(measured.entitiesLoaded()).isEqualTo(1);
		assertThat(measured.statements()).isLessThanOrEqualTo(2);
	}

	@Test
	void assignedQuizQueriesRun() {
		Object[] enrollment = entityManager.createQuery(
						"select e.user.id, e.fapClass.id from ClassEnrollment e order by e.id", Object[].class)
				.setMaxResults(1)
				.getResultList()
				.stream()
				.findFirst()
				.orElse(new Object[] {superAdmin.id(), -1L});
		Long userId = (Long) enrollment[0];
		Long classId = (Long) enrollment[1];
		LocalDate today = LocalDate.now();

		var page = quizRepository.searchAssignedToUser(userId, QuizStatus.Published, ELIGIBLE, today, PageRequest.of(0, 10));
		var byClass = quizRepository.findAssignedToUserByClass(userId, classId, QuizStatus.Published, ELIGIBLE, today);

		assertThat(page.getContent()).doesNotHaveDuplicates();
		assertThat(byClass).doesNotHaveDuplicates();
	}

	private List<QuizAttempt> attemptsOfQuiz() {
		return entityManager.createQuery("select a from QuizAttempt a where a.quiz.id = :quizId", QuizAttempt.class)
				.setParameter("quizId", quizId)
				.getResultList();
	}
}
