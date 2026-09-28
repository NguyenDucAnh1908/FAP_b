package com.fap.program.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.program.entity.TrainingProgram;
import com.fap.program.enums.TrainingProgramStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrainingProgramRepository extends JpaRepository<TrainingProgram, Long> {

	long countByStatus(TrainingProgramStatus status);

	/** One place for the lookup so every caller fails with the same not-found message. */
	default TrainingProgram getTrainingProgramOrThrow(Long id) {
		return findById(id)
				.orElseThrow(() -> new NotFoundException("Training program not found"));
	}

	interface StatusCount {
		TrainingProgramStatus getStatus();

		Long getTotal();
	}

	/** Row count per status in one query, for dashboards (replaces one count query per status). */
	@Query("select p.status as status, count(p) as total from TrainingProgram p group by p.status")
	List<StatusCount> countGroupedByStatus();

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
