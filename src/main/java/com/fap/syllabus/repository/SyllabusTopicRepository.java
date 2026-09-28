package com.fap.syllabus.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.syllabus.entity.SyllabusTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyllabusTopicRepository extends JpaRepository<SyllabusTopic, Long> {

	Optional<SyllabusTopic> findByIdAndUnitId(Long id, Long unitId);

	Optional<SyllabusTopic> findByIdAndUnitDaySyllabusId(Long id, Long syllabusId);

	default SyllabusTopic getByIdAndUnitDaySyllabusIdOrThrow(Long id, Long syllabusId) {
		return findByIdAndUnitDaySyllabusId(id, syllabusId)
				.orElseThrow(() -> new NotFoundException("syllabus_topic", "Syllabus topic not found"));
	}
}
