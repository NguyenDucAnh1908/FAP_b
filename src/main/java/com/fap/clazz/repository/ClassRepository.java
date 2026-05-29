package com.fap.clazz.repository;

import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ClassRepository extends JpaRepository<FapClass, Long> {

	boolean existsByClassCodeIgnoreCase(String classCode);

	@EntityGraph(attributePaths = "trainingProgram")
	Optional<FapClass> findWithTrainingProgramById(Long id);

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
			join TrainingSession s on s.fapClass = c
			join TrainingRegistration r on r.trainingSession = s
			where r.user.id = :userId
			  and r.status in :eligibleStatuses
			  and (:keyword is null
			       or lower(c.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(c.classCode) like concat(concat('%', lower(:keyword)), '%')
			       or lower(coalesce(c.location, '')) like concat(concat('%', lower(:keyword)), '%')
			       or lower(c.trainingProgram.name) like concat(concat('%', lower(:keyword)), '%'))
			""")
	Page<FapClass> searchMine(
			@Param("userId") Long userId,
			@Param("eligibleStatuses") Collection<TrainingRegistrationStatus> eligibleStatuses,
			@Param("keyword") String keyword,
			Pageable pageable);

	@EntityGraph(attributePaths = "trainingProgram")
	@Query("""
			select distinct c
			from FapClass c
			join TrainingSession s on s.fapClass = c
			join TrainingRegistration r on r.trainingSession = s
			where c.id = :classId
			  and r.user.id = :userId
			  and r.status in :eligibleStatuses
			""")
	Optional<FapClass> findMineById(
			@Param("classId") Long classId,
			@Param("userId") Long userId,
			@Param("eligibleStatuses") Collection<TrainingRegistrationStatus> eligibleStatuses);
}
