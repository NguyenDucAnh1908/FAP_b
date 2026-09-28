package com.fap.quiz.repository;

import com.fap.quiz.entity.QuizQuestion;
import com.fap.quiz.entity.QuizQuestionId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, QuizQuestionId> {

	@EntityGraph(attributePaths = {"question"})
	List<QuizQuestion> findByIdQuizIdOrderBySortOrderAsc(Long quizId);

	long countByIdQuizId(Long quizId);

	interface QuizQuestionCount {
		Long getQuizId();

		Long getTotal();
	}

	@Query("""
			select qq.id.quizId as quizId, count(qq) as total
			from QuizQuestion qq
			where qq.id.quizId in :quizIds
			group by qq.id.quizId
			""")
	List<QuizQuestionCount> countGroupedByQuizId(@Param("quizIds") Collection<Long> quizIds);

	void deleteByIdQuizId(Long quizId);
}
