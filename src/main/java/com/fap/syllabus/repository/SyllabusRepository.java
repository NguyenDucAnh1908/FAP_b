package com.fap.syllabus.repository;

import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.enums.SyllabusStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {

	boolean existsByCodeIgnoreCase(String code);

	@Query("""
			select s
			from Syllabus s
			where (:status is null or s.status = :status)
			  and (:levelName is null or lower(s.levelName) = lower(:levelName))
			  and (:keyword is null
			       or lower(s.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(s.code) like concat(concat('%', lower(:keyword)), '%')
			       or lower(s.version) like concat(concat('%', lower(:keyword)), '%'))
			""")
	Page<Syllabus> search(
			@Param("status") SyllabusStatus status,
			@Param("levelName") String levelName,
			@Param("keyword") String keyword,
			Pageable pageable);

	@Query(
			value = """
					select count(*)
					from syllabus_topics topic
					join syllabus_units unit on unit.id = topic.unit_id
					join syllabus_days day on day.id = unit.day_id
					where day.syllabus_id = :syllabusId
					""",
			nativeQuery = true)
	long countTopicsBySyllabusId(@Param("syllabusId") Long syllabusId);
}
