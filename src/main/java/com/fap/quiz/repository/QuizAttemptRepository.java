package com.fap.quiz.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.quiz.entity.QuizAttempt;
import com.fap.quiz.enums.QuizAttemptStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import java.time.LocalDateTime;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

	@Query("""
			select count(a)
			from QuizAttempt a
			where (:status is null or a.status = :status)
			  and (:passed is null or a.passed = :passed)
			  and (:fromDateTime is null or a.submittedAt >= :fromDateTime)
			  and (:toDateTime is null or a.submittedAt < :toDateTime)
			""")
	long countForDashboard(
			@Param("status") QuizAttemptStatus status,
			@Param("passed") Boolean passed,
			@Param("fromDateTime") LocalDateTime fromDateTime,
			@Param("toDateTime") LocalDateTime toDateTime);

	interface UserQuizAttemptStats {
		Long getQuizId();

		Long getAttempts();

		Long getPassedAttempts();

		Long getLatestAttemptId();
	}

	/** One user's attempt count, passed count and latest attempt id for each quiz, in one query. */
	@Query("""
			select a.quiz.id as quizId,
			       count(a) as attempts,
			       coalesce(sum(case when a.passed = true then 1L else 0L end), 0L) as passedAttempts,
			       max(a.id) as latestAttemptId
			from QuizAttempt a
			where a.user.id = :userId
			  and a.quiz.id in :quizIds
			group by a.quiz.id
			""")
	List<UserQuizAttemptStats> summarizeForUserByQuiz(
			@Param("userId") Long userId,
			@Param("quizIds") Collection<Long> quizIds);

	interface ScoredAttempt {
		Long getId();

		Long getQuizId();

		Long getUserId();

		Integer getScore();
	}

	/**
	 * Submitted attempts of the given quizzes by users enrolled in the class, best first per
	 * {@code findFirstByQuizIdAndUserIdAndStatusOrderByScoreDescIdDesc} ordering (on Oracle a null
	 * score sorts first in DESC order). A projection, so the CLOB answers are never read.
	 */
	@Query("""
			select a.id as id, a.quiz.id as quizId, a.user.id as userId, a.score as score
			from QuizAttempt a
			where a.status = com.fap.quiz.enums.QuizAttemptStatus.Submitted
			  and a.quiz.id in :quizIds
			  and a.user.id in (select e.user.id from ClassEnrollment e where e.fapClass.id = :classId)
			order by a.score desc, a.id desc
			""")
	List<ScoredAttempt> findSubmittedForClassOrderByScoreDesc(
			@Param("classId") Long classId,
			@Param("quizIds") Collection<Long> quizIds);

	interface SubmittedOutcomeCount {
		Long getSubmitted();

		Long getPassed();
	}

	/** Submitted and passed attempt totals in one scan, for the admin dashboard. */
	@Query("""
			select count(a) as submitted,
			       coalesce(sum(case when a.passed = true then 1L else 0L end), 0L) as passed
			from QuizAttempt a
			where a.status = com.fap.quiz.enums.QuizAttemptStatus.Submitted
			""")
	SubmittedOutcomeCount countSubmittedOutcomes();

	long countByQuizIdAndUserId(Long quizId, Long userId);

	long countByQuizIdAndUserIdAndStatus(Long quizId, Long userId, QuizAttemptStatus status);

	long countByQuizIdAndUserIdAndPassed(Long quizId, Long userId, Boolean passed);

	@EntityGraph(attributePaths = {"quiz", "user"})
	Optional<QuizAttempt> findByIdAndUserId(Long id, Long userId);

	/** Scoped to the owner so another user's attempt reads as not found rather than forbidden. */
	default QuizAttempt getByIdAndUserIdOrThrow(Long id, Long userId) {
		return findByIdAndUserId(id, userId)
				.orElseThrow(() -> new NotFoundException("Quiz attempt not found"));
	}

	@EntityGraph(attributePaths = {"quiz", "user"})
	Optional<QuizAttempt> findByQuizIdAndId(Long quizId, Long id);

	/** Scoped to the quiz so an attempt id of another quiz reads as not found. */
	default QuizAttempt getByQuizIdAndIdOrThrow(Long quizId, Long id) {
		return findByQuizIdAndId(quizId, id)
				.orElseThrow(() -> new NotFoundException("Quiz attempt not found"));
	}

	@EntityGraph(attributePaths = {"quiz", "user"})
	Optional<QuizAttempt> findFirstByQuizIdAndUserIdOrderByIdDesc(Long quizId, Long userId);

	@EntityGraph(attributePaths = {"quiz", "user"})
	Optional<QuizAttempt> findFirstByQuizIdAndUserIdAndStatusOrderByIdDesc(
			Long quizId,
			Long userId,
			QuizAttemptStatus status);

	@EntityGraph(attributePaths = {"quiz", "user"})
	Optional<QuizAttempt> findFirstByQuizIdAndUserIdAndStatusOrderByScoreDescIdDesc(
			Long quizId,
			Long userId,
			QuizAttemptStatus status);

	@EntityGraph(attributePaths = {"quiz", "user"})
	@Query(
			value = """
					select distinct a
					from QuizAttempt a
					where a.quiz.id = :quizId
					  and (:status is null or a.status = :status)
					  and (:passed is null or a.passed = :passed)
					  and (:userId is null or a.user.id = :userId)
					  and (:classId is null or exists (
					      select r.id
					      from TrainingRegistration r
					      where r.user = a.user
					        and r.status in :eligibleStatuses
					        and r.trainingSession.fapClass.id = :classId
					  ))
					  and (:trainingSessionId is null or exists (
					      select r.id
					      from TrainingRegistration r
					      where r.user = a.user
					        and r.status in :eligibleStatuses
					        and r.trainingSession.id = :trainingSessionId
					  ))
					  and (
					      :scopeAll = true
					      or exists (
					          select r.id
					          from TrainingRegistration r
					          join ClassAdmin ca on ca.fapClass = r.trainingSession.fapClass
					          where r.user = a.user
					            and r.status in :eligibleStatuses
					            and ca.user.id = :scopeUserId
					      )
					      or exists (
					          select r.id
					          from TrainingRegistration r
					          join ClassTrainer ct on ct.fapClass = r.trainingSession.fapClass
					          where r.user = a.user
					            and r.status in :eligibleStatuses
					            and ct.user.id = :scopeUserId
					      )
					      or exists (
					          select r.id
					          from TrainingRegistration r
					          where r.user = a.user
					            and r.status in :eligibleStatuses
					            and r.trainingSession.trainer.id = :scopeUserId
					      )
					  )
					""",
			countQuery = """
					select count(distinct a)
					from QuizAttempt a
					where a.quiz.id = :quizId
					  and (:status is null or a.status = :status)
					  and (:passed is null or a.passed = :passed)
					  and (:userId is null or a.user.id = :userId)
					  and (:classId is null or exists (
					      select r.id
					      from TrainingRegistration r
					      where r.user = a.user
					        and r.status in :eligibleStatuses
					        and r.trainingSession.fapClass.id = :classId
					  ))
					  and (:trainingSessionId is null or exists (
					      select r.id
					      from TrainingRegistration r
					      where r.user = a.user
					        and r.status in :eligibleStatuses
					        and r.trainingSession.id = :trainingSessionId
					  ))
					  and (
					      :scopeAll = true
					      or exists (
					          select r.id
					          from TrainingRegistration r
					          join ClassAdmin ca on ca.fapClass = r.trainingSession.fapClass
					          where r.user = a.user
					            and r.status in :eligibleStatuses
					            and ca.user.id = :scopeUserId
					      )
					      or exists (
					          select r.id
					          from TrainingRegistration r
					          join ClassTrainer ct on ct.fapClass = r.trainingSession.fapClass
					          where r.user = a.user
					            and r.status in :eligibleStatuses
					            and ct.user.id = :scopeUserId
					      )
					      or exists (
					          select r.id
					          from TrainingRegistration r
					          where r.user = a.user
					            and r.status in :eligibleStatuses
					            and r.trainingSession.trainer.id = :scopeUserId
					      )
					  )
					""")
	Page<QuizAttempt> searchQuizResults(
			@Param("quizId") Long quizId,
			@Param("status") QuizAttemptStatus status,
			@Param("passed") Boolean passed,
			@Param("userId") Long userId,
			@Param("classId") Long classId,
			@Param("trainingSessionId") Long trainingSessionId,
			@Param("scopeAll") boolean scopeAll,
			@Param("scopeUserId") Long scopeUserId,
			@Param("eligibleStatuses") Collection<TrainingRegistrationStatus> eligibleStatuses,
			Pageable pageable);

	/**
	 * Aggregates the attempts {@link #searchQuizResults} would return (same filters, keep the WHERE
	 * clauses in sync) without loading them, so the summary never materializes attempts and their
	 * CLOB answers. Averages and extremes only count submitted attempts.
	 */
	@Query("""
			select new com.fap.quiz.repository.QuizAttemptStats(
			    count(a),
			    coalesce(sum(case when a.status = com.fap.quiz.enums.QuizAttemptStatus.InProgress then 1L else 0L end), 0L),
			    coalesce(sum(case when a.status = com.fap.quiz.enums.QuizAttemptStatus.Submitted then 1L else 0L end), 0L),
			    coalesce(sum(case when a.passed = true then 1L else 0L end), 0L),
			    avg(case when a.status = com.fap.quiz.enums.QuizAttemptStatus.Submitted then a.score end),
			    max(case when a.status = com.fap.quiz.enums.QuizAttemptStatus.Submitted then a.score end),
			    min(case when a.status = com.fap.quiz.enums.QuizAttemptStatus.Submitted then a.score end))
			from QuizAttempt a
			where a.quiz.id = :quizId
			  and (:classId is null or exists (
			      select r.id
			      from TrainingRegistration r
			      where r.user = a.user
			        and r.status in :eligibleStatuses
			        and r.trainingSession.fapClass.id = :classId
			  ))
			  and (:trainingSessionId is null or exists (
			      select r.id
			      from TrainingRegistration r
			      where r.user = a.user
			        and r.status in :eligibleStatuses
			        and r.trainingSession.id = :trainingSessionId
			  ))
			  and (
			      :scopeAll = true
			      or exists (
			          select r.id
			          from TrainingRegistration r
			          join ClassAdmin ca on ca.fapClass = r.trainingSession.fapClass
			          where r.user = a.user
			            and r.status in :eligibleStatuses
			            and ca.user.id = :scopeUserId
			      )
			      or exists (
			          select r.id
			          from TrainingRegistration r
			          join ClassTrainer ct on ct.fapClass = r.trainingSession.fapClass
			          where r.user = a.user
			            and r.status in :eligibleStatuses
			            and ct.user.id = :scopeUserId
			      )
			      or exists (
			          select r.id
			          from TrainingRegistration r
			          where r.user = a.user
			            and r.status in :eligibleStatuses
			            and r.trainingSession.trainer.id = :scopeUserId
			      )
			  )
			""")
	QuizAttemptStats summarizeQuizResults(
			@Param("quizId") Long quizId,
			@Param("classId") Long classId,
			@Param("trainingSessionId") Long trainingSessionId,
			@Param("scopeAll") boolean scopeAll,
			@Param("scopeUserId") Long scopeUserId,
			@Param("eligibleStatuses") Collection<TrainingRegistrationStatus> eligibleStatuses);

	@Query("""
			select count(distinct a)
			from QuizAttempt a
			where a.quiz.id = :quizId
			  and a.id = :attemptId
			  and (
			      :scopeAll = true
			      or exists (
			          select r.id
			          from TrainingRegistration r
			          join ClassAdmin ca on ca.fapClass = r.trainingSession.fapClass
			          where r.user = a.user
			            and r.status in :eligibleStatuses
			            and ca.user.id = :scopeUserId
			      )
			      or exists (
			          select r.id
			          from TrainingRegistration r
			          join ClassTrainer ct on ct.fapClass = r.trainingSession.fapClass
			          where r.user = a.user
			            and r.status in :eligibleStatuses
			            and ct.user.id = :scopeUserId
			      )
			      or exists (
			          select r.id
			          from TrainingRegistration r
			          where r.user = a.user
			            and r.status in :eligibleStatuses
			            and r.trainingSession.trainer.id = :scopeUserId
			      )
			  )
			""")
	long countVisibleQuizResult(
			@Param("quizId") Long quizId,
			@Param("attemptId") Long attemptId,
			@Param("scopeAll") boolean scopeAll,
			@Param("scopeUserId") Long scopeUserId,
			@Param("eligibleStatuses") Collection<TrainingRegistrationStatus> eligibleStatuses);
}
