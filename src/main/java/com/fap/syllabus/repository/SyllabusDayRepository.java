package com.fap.syllabus.repository;

import com.fap.syllabus.entity.SyllabusDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SyllabusDayRepository extends JpaRepository<SyllabusDay, Long> {

	List<SyllabusDay> findBySyllabusIdOrderBySortOrderAsc(Long syllabusId);

	Optional<SyllabusDay> findByIdAndSyllabusId(Long id, Long syllabusId);

	@Modifying
	@Query("delete from SyllabusDay d where d.syllabus.id = :syllabusId")
	void deleteBySyllabusId(@Param("syllabusId") Long syllabusId);
}
