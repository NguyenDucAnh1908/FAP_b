package com.fap.training.repository;

import com.fap.training.entity.AttendanceRecord;
import com.fap.training.enums.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	Optional<AttendanceRecord> findByTrainingSessionIdAndUserId(Long trainingSessionId, Long userId);

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	List<AttendanceRecord> findByTrainingSessionIdOrderByUserFullNameAsc(Long trainingSessionId);

	@EntityGraph(attributePaths = {"trainingSession", "trainingSession.fapClass", "trainingSession.trainer", "user"})
	@Query("""
			select a
			from AttendanceRecord a
			where a.user.id = :userId
			  and (:status is null or a.status = :status)
			  and (:fromDate is null or a.trainingSession.sessionDate >= :fromDate)
			  and (:toDate is null or a.trainingSession.sessionDate <= :toDate)
			  and (:keyword is null
			       or lower(a.trainingSession.title) like concat(concat('%', lower(:keyword)), '%')
			       or lower(coalesce(a.trainingSession.room, '')) like concat(concat('%', lower(:keyword)), '%')
			       or lower(a.trainingSession.fapClass.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(a.trainingSession.fapClass.classCode) like concat(concat('%', lower(:keyword)), '%')
			       or lower(a.trainingSession.trainer.fullName) like concat(concat('%', lower(:keyword)), '%')
			       or lower(a.trainingSession.trainer.email) like concat(concat('%', lower(:keyword)), '%'))
			order by a.trainingSession.sessionDate desc, a.trainingSession.startTime desc, a.id desc
			""")
	Page<AttendanceRecord> searchMine(
			@Param("userId") Long userId,
			@Param("status") AttendanceStatus status,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			@Param("keyword") String keyword,
			Pageable pageable);

	long countByTrainingSessionId(Long trainingSessionId);
}
