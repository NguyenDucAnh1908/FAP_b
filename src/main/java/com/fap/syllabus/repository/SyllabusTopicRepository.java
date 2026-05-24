package com.fap.syllabus.repository;

import com.fap.syllabus.entity.SyllabusTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyllabusTopicRepository extends JpaRepository<SyllabusTopic, Long> {

	Optional<SyllabusTopic> findByIdAndUnitId(Long id, Long unitId);

	Optional<SyllabusTopic> findByIdAndUnitDaySyllabusId(Long id, Long syllabusId);
}
