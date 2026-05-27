package com.fap.program.repository;

import com.fap.program.entity.TrainingProgramSyllabus;
import com.fap.program.entity.TrainingProgramSyllabusId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingProgramSyllabusRepository
		extends JpaRepository<TrainingProgramSyllabus, TrainingProgramSyllabusId> {

	List<TrainingProgramSyllabus> findByIdProgramIdOrderBySortOrderAsc(Long programId);

	long countByIdProgramId(Long programId);

	void deleteByIdProgramId(Long programId);

	boolean existsByIdProgramIdAndIdSyllabusId(Long programId, Long syllabusId);
}
