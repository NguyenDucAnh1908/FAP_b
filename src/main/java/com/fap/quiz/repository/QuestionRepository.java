package com.fap.quiz.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.quiz.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

	/** One place for the lookup so every caller fails with the same not-found message. */
	default Question getQuestionOrThrow(Long id) {
		return findById(id)
				.orElseThrow(() -> new NotFoundException("Question not found"));
	}

	@Query(
			value = """
					select *
					from questions q
					where q.is_deleted = 0
					  and (:questionType is null or q.question_type = :questionType)
					  and (:difficulty is null or q.difficulty = :difficulty)
					  and (:category is null or lower(q.category) = lower(:category))
					  and (:keyword is null
					       or lower(q.category) like '%' || lower(:keyword) || '%'
					       or dbms_lob.instr(lower(q.content), lower(:keyword)) > 0)
					""",
			countQuery = """
					select count(*)
					from questions q
					where q.is_deleted = 0
					  and (:questionType is null or q.question_type = :questionType)
					  and (:difficulty is null or q.difficulty = :difficulty)
					  and (:category is null or lower(q.category) = lower(:category))
					  and (:keyword is null
					       or lower(q.category) like '%' || lower(:keyword) || '%'
					       or dbms_lob.instr(lower(q.content), lower(:keyword)) > 0)
					""",
			nativeQuery = true)
	Page<Question> search(
			@Param("questionType") String questionType,
			@Param("difficulty") String difficulty,
			@Param("category") String category,
			@Param("keyword") String keyword,
			Pageable pageable);
}
