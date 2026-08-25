package com.fap.quiz.repository;

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

	long countByQuizIdAndUserId(Long quizId, Long userId);

	long countByQuizIdAndUserIdAndStatus(Long quizId, Long userId, QuizAttemptStatus status);

	long countByQuizIdAndUserIdAndPassed(Long quizId, Long userId, Boolean passed);

	@EntityGraph(attributePaths = {"quiz", "user"})
	Optional<QuizAttempt> findByIdAndUserId(Long id, Long userId);

	@EntityGraph(attributePaths = {"quiz", "user"})
	Optional<QuizAttempt> findByQuizIdAndId(Long quizId, Long id);

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
