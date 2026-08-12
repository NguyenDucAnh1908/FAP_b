package com.fap.training.repository;

import com.fap.training.entity.TrainingSession;
import com.fap.training.enums.TrainingRegistrationStatus;
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

	@EntityGraph(attributePaths = {"fapClass", "trainer"})
	@Query("""
			select s
			from TrainingSession s
			where (:scopeUserId is null
			       or exists (select ca.id from ClassAdmin ca
			                  where ca.fapClass = s.fapClass and ca.user.id = :scopeUserId)
			       or exists (select ct.id from ClassTrainer ct
			                  where ct.fapClass = s.fapClass and ct.user.id = :scopeUserId)
			       or s.trainer.id = :scopeUserId)
			  and (:status is null or s.status = :status)
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
	Page<TrainingSession> searchScoped(
			@Param("scopeUserId") Long scopeUserId,
			@Param("status") TrainingSessionStatus status,
			@Param("classId") Long classId,
			@Param("trainerId") Long trainerId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			@Param("keyword") String keyword,
			Pageable pageable);

	long countByTrainerIdAndStatus(Long trainerId, TrainingSessionStatus status);

	long countByTrainerIdAndStatusAndSessionDateGreaterThanEqual(
			Long trainerId,
			TrainingSessionStatus status,
			LocalDate sessionDate);

	@Query("""
			select count(s)
			from TrainingSession s
			where s.trainer.id = :trainerId
			  and s.status = :sessionStatus
			  and s.sessionDate <= :asOfDate
			  and (select count(r)
			       from TrainingRegistration r
			       where r.trainingSession = s
			         and r.status = :registrationStatus) >
			      (select count(a)
			       from AttendanceRecord a
			       where a.trainingSession = s)
			""")
	long countPendingAttendanceSessions(
			@Param("trainerId") Long trainerId,
			@Param("sessionStatus") TrainingSessionStatus sessionStatus,
			@Param("registrationStatus") TrainingRegistrationStatus registrationStatus,
			@Param("asOfDate") LocalDate asOfDate);

	@Query("""
			select count(distinct s)
			from TrainingSession s
			join ClassAdmin ca on ca.fapClass = s.fapClass
			where ca.user.id = :adminId
			  and (:status is null or s.status = :status)
			  and (:fromDate is null or s.sessionDate >= :fromDate)
			  and (:toDate is null or s.sessionDate <= :toDate)
			""")
	long countByClassAdminId(
			@Param("adminId") Long adminId,
			@Param("status") TrainingSessionStatus status,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);

	@Query("""
			select count(distinct s)
			from TrainingSession s
			join ClassAdmin ca on ca.fapClass = s.fapClass
			where ca.user.id = :adminId
			  and s.status = :sessionStatus
			  and s.sessionDate <= :asOfDate
			  and (select count(r)
			       from TrainingRegistration r
			       where r.trainingSession = s
			         and r.status = :registrationStatus) >
			      (select count(a)
			       from AttendanceRecord a
			       where a.trainingSession = s)
			""")
	long countPendingAttendanceSessionsByClassAdminId(
			@Param("adminId") Long adminId,
			@Param("sessionStatus") TrainingSessionStatus sessionStatus,
			@Param("registrationStatus") TrainingRegistrationStatus registrationStatus,
			@Param("asOfDate") LocalDate asOfDate);

	@EntityGraph(attributePaths = {"fapClass", "trainer"})
	@Query("""
			select s
			from TrainingSession s
			where exists (select ca.id from ClassAdmin ca
			              where ca.fapClass = s.fapClass and ca.user.id = :adminId)
			  and (:status is null or s.status = :status)
			  and (:fromDate is null or s.sessionDate >= :fromDate)
			  and (:toDate is null or s.sessionDate <= :toDate)
			""")
	Page<TrainingSession> searchByClassAdminId(
			@Param("adminId") Long adminId,
			@Param("status") TrainingSessionStatus status,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			Pageable pageable);
}
