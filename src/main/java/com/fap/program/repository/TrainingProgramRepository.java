package com.fap.program.repository;

import com.fap.program.entity.TrainingProgram;
import com.fap.program.enums.TrainingProgramStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingProgramRepository extends JpaRepository<TrainingProgram, Long> {

	@Query("""
			select p
			from TrainingProgram p
			where (:status is null or p.status = :status)
			  and (:keyword is null
			       or lower(p.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(p.version) like concat(concat('%', lower(:keyword)), '%'))
			""")
	Page<TrainingProgram> search(
			@Param("status") TrainingProgramStatus status,
			@Param("keyword") String keyword,
			Pageable pageable);
}
