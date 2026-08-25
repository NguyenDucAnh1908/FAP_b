package com.fap.result.repository;

import com.fap.result.entity.ClassCompletionQuiz;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassCompletionQuizRepository extends JpaRepository<ClassCompletionQuiz, Long> {
	@EntityGraph(attributePaths = {"fapClass", "quiz"})
	List<ClassCompletionQuiz> findByFapClassIdOrderByIdAsc(Long classId);

	boolean existsByFapClassIdAndQuizId(Long classId, Long quizId);

	void deleteByFapClassId(Long classId);
}
