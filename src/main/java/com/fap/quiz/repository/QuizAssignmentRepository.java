package com.fap.quiz.repository;

import com.fap.quiz.entity.QuizAssignment;
import com.fap.training.enums.TrainingRegistrationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuizAssignmentRepository extends JpaRepository<QuizAssignment, Long> {

	@EntityGraph(attributePaths = {"quiz", "fapClass", "trainingSession", "assignedBy"})
	List<QuizAssignment> findByQuizIdOrderByIdAsc(Long quizId);

	@EntityGraph(attributePaths = {"quiz", "fapClass", "trainingSession", "assignedBy"})
	Optional<QuizAssignment> findByQuizIdAndId(Long quizId, Long id);

	boolean existsByQuizIdAndFapClassId(Long quizId, Long classId);

	boolean existsByQuizIdAndTrainingSessionId(Long quizId, Long trainingSessionId);

	@Query("""
			select count(qa)
			from QuizAssignment qa
			where qa.quiz.id = :quizId
			  and (
			       qa.trainingSession.id in (
			           select r.trainingSession.id
			           from TrainingRegistration r
			           where r.user.id = :userId
			             and r.status in :eligibleStatuses
			       )
			       or qa.fapClass.id in (
			           select r.trainingSession.fapClass.id
			           from TrainingRegistration r
			           where r.user.id = :userId
			             and r.status in :eligibleStatuses
			       )
			  )
			""")
	long countEligibleAssignments(
			@Param("quizId") Long quizId,
			@Param("userId") Long userId,
			@Param("eligibleStatuses") Collection<TrainingRegistrationStatus> eligibleStatuses);
}
