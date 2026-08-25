package com.fap.result.repository;

import com.fap.result.entity.CourseResultQuiz;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseResultQuizRepository extends JpaRepository<CourseResultQuiz, Long> {
	@EntityGraph(attributePaths = "quiz")
	List<CourseResultQuiz> findByCourseResultIdOrderByIdAsc(Long courseResultId);

	void deleteByCourseResultId(Long courseResultId);
}
