package com.fap.syllabus.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.syllabus.entity.SyllabusUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyllabusUnitRepository extends JpaRepository<SyllabusUnit, Long> {

	Optional<SyllabusUnit> findByIdAndDayId(Long id, Long dayId);

	Optional<SyllabusUnit> findByIdAndDaySyllabusId(Long id, Long syllabusId);

	default SyllabusUnit getByIdAndDaySyllabusIdOrThrow(Long id, Long syllabusId) {
		return findByIdAndDaySyllabusId(id, syllabusId)
				.orElseThrow(() -> new NotFoundException("Syllabus unit not found"));
	}
}
