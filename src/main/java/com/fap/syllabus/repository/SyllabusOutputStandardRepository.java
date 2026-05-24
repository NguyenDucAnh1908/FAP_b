package com.fap.syllabus.repository;

import com.fap.syllabus.entity.SyllabusOutputStandard;
import com.fap.syllabus.entity.SyllabusOutputStandardId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyllabusOutputStandardRepository
		extends JpaRepository<SyllabusOutputStandard, SyllabusOutputStandardId> {

	List<SyllabusOutputStandard> findByIdSyllabusIdOrderByIdStandardCodeAsc(Long syllabusId);

	void deleteByIdSyllabusId(Long syllabusId);
}
