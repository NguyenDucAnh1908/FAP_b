package com.fap.clazz.repository;

import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;

public interface ClassRepository extends JpaRepository<FapClass, Long> {

	boolean existsByClassCodeIgnoreCase(String classCode);

	long countByStatus(ClassStatus status);

	@EntityGraph(attributePaths = "trainingProgram")
	Optional<FapClass> findWithTrainingProgramById(Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "trainingProgram")
	@Query("select c from FapClass c where c.id = :id")
	Optional<FapClass> findWithTrainingProgramByIdForUpdate(@Param("id") Long id);

	@EntityGraph(attributePaths = "trainingProgram")
	@Query("""
			select c
			from FapClass c
			where c.status = com.fap.clazz.enums.ClassStatus.Active
			  and c.selfEnrollmentEnabled = true
			  and (:today is null or c.enrollmentStartDate is null or c.enrollmentStartDate <= :today)
			  and (:today is null or c.enrollmentEndDate is null or c.enrollmentEndDate >= :today)
			  and (:keyword is null
			       or lower(c.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(c.classCode) like concat(concat('%', lower(:keyword)), '%'))
			  and not exists (
			      select e.id from ClassEnrollment e
			      where e.fapClass = c
			        and e.user.id = :userId
			        and e.status in (com.fap.clazz.enums.ClassEnrollmentStatus.Enrolled,
			                         com.fap.clazz.enums.ClassEnrollmentStatus.PendingApproval,
			                         com.fap.clazz.enums.ClassEnrollmentStatus.Waitlisted,
			                         com.fap.clazz.enums.ClassEnrollmentStatus.Completed)
			  )
			""")
	Page<FapClass> searchAvailableForUser(
			@Param("userId") Long userId,
			@Param("today") LocalDate today,
			@Param("keyword") String keyword,
			Pageable pageable);

	@EntityGraph(attributePaths = "trainingProgram")
	@Query("""
			select c
			from FapClass c
			where (:status is null or c.status = :status)
			  and (:trainingProgramId is null or c.trainingProgram.id = :trainingProgramId)
			  and (:keyword is null
			       or lower(c.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(c.classCode) like concat(concat('%', lower(:keyword)), '%')
			       or lower(coalesce(c.location, '')) like concat(concat('%', lower(:keyword)), '%'))
			""")
	Page<FapClass> search(
			@Param("status") ClassStatus status,
			@Param("trainingProgramId") Long trainingProgramId,
			@Param("keyword") String keyword,
			Pageable pageable);

	@EntityGraph(attributePaths = "trainingProgram")
	@Query("""
			select distinct c
			from FapClass c
			left join ClassAdmin ca on ca.fapClass = c and ca.user.id = :scopeUserId
			left join ClassTrainer ct on ct.fapClass = c and ct.user.id = :scopeUserId
			where (:scopeUserId is null or ca.id is not null or ct.id is not null)
			  and (:status is null or c.status = :status)
			  and (:trainingProgramId is null or c.trainingProgram.id = :trainingProgramId)
			  and (:keyword is null
			       or lower(c.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(c.classCode) like concat(concat('%', lower(:keyword)), '%')
			       or lower(coalesce(c.location, '')) like concat(concat('%', lower(:keyword)), '%'))
			""")
	Page<FapClass> searchScoped(
			@Param("scopeUserId") Long scopeUserId,
			@Param("status") ClassStatus status,
			@Param("trainingProgramId") Long trainingProgramId,
			@Param("keyword") String keyword,
			Pageable pageable);

	@Query("""
			select count(distinct c)
			from FapClass c
			join ClassAdmin ca on ca.fapClass = c
			where ca.user.id = :adminId
			  and (:status is null or c.status = :status)
			""")
	long countByAdminIdAndStatus(
			@Param("adminId") Long adminId,
			@Param("status") ClassStatus status);

	@EntityGraph(attributePaths = "trainingProgram")
	@Query("""
			select distinct c
			from FapClass c
			join ClassAdmin ca on ca.fapClass = c
			where ca.user.id = :adminId
			  and (:status is null or c.status = :status)
			  and (:fromDate is null or c.startDate >= :fromDate)
			  and (:toDate is null or c.startDate <= :toDate)
			""")
	Page<FapClass> searchByAdminId(
			@Param("adminId") Long adminId,
			@Param("status") ClassStatus status,
			@Param("fromDate") java.time.LocalDate fromDate,
			@Param("toDate") java.time.LocalDate toDate,
			Pageable pageable);

	@EntityGraph(attributePaths = "trainingProgram")
	@Query("""
			select distinct c
			from FapClass c
			join ClassEnrollment e on e.fapClass = c
			where e.user.id = :userId
			  and e.status in :eligibleStatuses
			  and (:keyword is null
			       or lower(c.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(c.classCode) like concat(concat('%', lower(:keyword)), '%')
			       or lower(coalesce(c.location, '')) like concat(concat('%', lower(:keyword)), '%')
			       or lower(c.trainingProgram.name) like concat(concat('%', lower(:keyword)), '%'))
			""")
	Page<FapClass> searchMine(
			@Param("userId") Long userId,
			@Param("eligibleStatuses") Collection<ClassEnrollmentStatus> eligibleStatuses,
			@Param("keyword") String keyword,
			Pageable pageable);

	@EntityGraph(attributePaths = "trainingProgram")
	@Query("""
			select distinct c
			from FapClass c
			join ClassEnrollment e on e.fapClass = c
			where c.id = :classId
			  and e.user.id = :userId
			  and e.status in :eligibleStatuses
			""")
	Optional<FapClass> findMineById(
			@Param("classId") Long classId,
			@Param("userId") Long userId,
			@Param("eligibleStatuses") Collection<ClassEnrollmentStatus> eligibleStatuses);
}
