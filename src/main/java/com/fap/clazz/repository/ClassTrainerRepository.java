package com.fap.clazz.repository;

import com.fap.clazz.entity.ClassTrainer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassTrainerRepository extends JpaRepository<ClassTrainer, Long> {

	@EntityGraph(attributePaths = {"user", "syllabus"})
	List<ClassTrainer> findByFapClassIdOrderByIdAsc(Long classId);

	void deleteByFapClassId(Long classId);

	boolean existsByFapClassId(Long classId);

	boolean existsByFapClassIdAndUserId(Long classId, Long userId);
}
