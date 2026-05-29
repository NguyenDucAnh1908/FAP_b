package com.fap.quiz.repository;

import com.fap.quiz.entity.Quiz;
import com.fap.quiz.enums.QuizStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

	@Query(
			value = """
					select *
					from quizzes q
					where q.is_deleted = 0
					  and (:status is null or q.status = :status)
					  and (:category is null or lower(q.category) = lower(:category))
					  and (:keyword is null
					       or lower(q.title) like '%' || lower(:keyword) || '%'
					       or lower(q.category) like '%' || lower(:keyword) || '%'
					       or dbms_lob.instr(lower(q.description), lower(:keyword)) > 0)
					""",
			countQuery = """
					select count(*)
					from quizzes q
					where q.is_deleted = 0
					  and (:status is null or q.status = :status)
					  and (:category is null or lower(q.category) = lower(:category))
					  and (:keyword is null
					       or lower(q.title) like '%' || lower(:keyword) || '%'
					       or lower(q.category) like '%' || lower(:keyword) || '%'
					       or dbms_lob.instr(lower(q.description), lower(:keyword)) > 0)
					""",
			nativeQuery = true)
	Page<Quiz> search(
			@Param("status") String status,
			@Param("category") String category,
			@Param("keyword") String keyword,
			Pageable pageable);

	@Query(
			value = """
					select distinct q
					from Quiz q
					join QuizAssignment qa on qa.quiz = q
					where q.status = :status
					  and (q.openDate is null or q.openDate <= :today)
					  and (q.closeDate is null or q.closeDate >= :today)
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
					""",
			countQuery = """
					select count(distinct q)
					from Quiz q
					join QuizAssignment qa on qa.quiz = q
					where q.status = :status
					  and (q.openDate is null or q.openDate <= :today)
					  and (q.closeDate is null or q.closeDate >= :today)
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
	Page<Quiz> searchAssignedToUser(
			@Param("userId") Long userId,
			@Param("status") QuizStatus status,
			@Param("eligibleStatuses") Collection<TrainingRegistrationStatus> eligibleStatuses,
			@Param("today") LocalDate today,
			Pageable pageable);
}
