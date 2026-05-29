package com.fap.quiz.repository;

import com.fap.quiz.entity.QuizQuestion;
import com.fap.quiz.entity.QuizQuestionId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, QuizQuestionId> {

	@EntityGraph(attributePaths = {"question"})
	List<QuizQuestion> findByIdQuizIdOrderBySortOrderAsc(Long quizId);

	long countByIdQuizId(Long quizId);

	void deleteByIdQuizId(Long quizId);
}
