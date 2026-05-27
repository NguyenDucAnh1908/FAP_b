package com.fap.training.repository;

import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {

	@EntityGraph(attributePaths = {"fapClass", "trainer"})
	Optional<TrainingSession> findWithClassAndTrainerById(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"fapClass", "trainer"})
	@Query("select s from TrainingSession s where s.id = :id")
	Optional<TrainingSession> findWithClassAndTrainerByIdForUpdate(@Param("id") Long id);

	@EntityGraph(attributePaths = {"fapClass", "trainer"})
	@Query("""
			select s
			from TrainingSession s
			where (:status is null or s.status = :status)
			  and (:classId is null or s.fapClass.id = :classId)
			  and (:trainerId is null or s.trainer.id = :trainerId)
			  and (:fromDate is null or s.sessionDate >= :fromDate)
			  and (:toDate is null or s.sessionDate <= :toDate)
			  and (:keyword is null
			       or lower(s.title) like concat(concat('%', lower(:keyword)), '%')
			       or lower(coalesce(s.room, '')) like concat(concat('%', lower(:keyword)), '%')
			       or lower(s.fapClass.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(s.fapClass.classCode) like concat(concat('%', lower(:keyword)), '%')
			       or lower(s.trainer.fullName) like concat(concat('%', lower(:keyword)), '%')
			       or lower(s.trainer.email) like concat(concat('%', lower(:keyword)), '%'))
			""")
	Page<TrainingSession> search(
			@Param("status") TrainingSessionStatus status,
			@Param("classId") Long classId,
			@Param("trainerId") Long trainerId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			@Param("keyword") String keyword,
			Pageable pageable);
}
