package com.fap.quiz.repository;

import com.fap.quiz.entity.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
