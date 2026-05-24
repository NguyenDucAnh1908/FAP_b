package com.fap.syllabus.repository;

import com.fap.syllabus.entity.SyllabusUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyllabusUnitRepository extends JpaRepository<SyllabusUnit, Long> {

	Optional<SyllabusUnit> findByIdAndDayId(Long id, Long dayId);

	Optional<SyllabusUnit> findByIdAndDaySyllabusId(Long id, Long syllabusId);
}
