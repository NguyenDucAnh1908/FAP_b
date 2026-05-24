package com.fap.syllabus.repository;

import com.fap.syllabus.entity.SyllabusDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SyllabusDayRepository extends JpaRepository<SyllabusDay, Long> {

	List<SyllabusDay> findBySyllabusIdOrderBySortOrderAsc(Long syllabusId);

	Optional<SyllabusDay> findByIdAndSyllabusId(Long id, Long syllabusId);
}
