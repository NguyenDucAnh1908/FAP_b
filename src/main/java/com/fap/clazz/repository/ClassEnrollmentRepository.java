package com.fap.clazz.repository;

import com.fap.clazz.entity.ClassEnrollment;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

	@Query("""
			select count(distinct e.user.id)
			from ClassEnrollment e
			where e.status in :statuses
			  and (:classAdminId is null
			       or exists (select ca.id from ClassAdmin ca
			                  where ca.fapClass = e.fapClass and ca.user.id = :classAdminId))
			  and (:fromDate is null or e.fapClass.startDate >= :fromDate)
			  and (:toDate is null or e.fapClass.startDate <= :toDate)
			""")
	long countDistinctParticipantsForAnalytics(
			@Param("classAdminId") Long classAdminId,
			@Param("statuses") Collection<ClassEnrollmentStatus> statuses,
			@Param("fromDate") java.time.LocalDate fromDate,
			@Param("toDate") java.time.LocalDate toDate);

	@EntityGraph(attributePaths = {"fapClass", "fapClass.trainingProgram", "user", "user.roles"})
	Optional<ClassEnrollment> findByFapClassIdAndUserId(Long classId, Long userId);

	long countByFapClassIdAndStatus(Long classId, ClassEnrollmentStatus status);

	boolean existsByFapClassIdAndUserIdAndStatusIn(
			Long classId,
			Long userId,
			Collection<ClassEnrollmentStatus> statuses);

	@EntityGraph(attributePaths = {"fapClass", "fapClass.trainingProgram", "user"})
	@Query("""
			select e
			from ClassEnrollment e
			where e.fapClass.id = :classId
			  and (:status is null or e.status = :status)
			  and (:keyword is null
			       or lower(e.user.fullName) like concat(concat('%', lower(:keyword)), '%')
			       or lower(e.user.email) like concat(concat('%', lower(:keyword)), '%'))
			""")
	Page<ClassEnrollment> searchByClass(
			@Param("classId") Long classId,
			@Param("status") ClassEnrollmentStatus status,
			@Param("keyword") String keyword,
			Pageable pageable);

	@EntityGraph(attributePaths = {"fapClass", "fapClass.trainingProgram", "user"})
	@Query("""
			select e
			from ClassEnrollment e
			where e.user.id = :userId
			  and (:status is null or e.status = :status)
			  and (:keyword is null
			       or lower(e.fapClass.name) like concat(concat('%', lower(:keyword)), '%')
			       or lower(e.fapClass.classCode) like concat(concat('%', lower(:keyword)), '%'))
			""")
	Page<ClassEnrollment> searchMine(
			@Param("userId") Long userId,
			@Param("status") ClassEnrollmentStatus status,
			@Param("keyword") String keyword,
			Pageable pageable);

	@EntityGraph(attributePaths = {"fapClass", "fapClass.trainingProgram", "user"})
	List<ClassEnrollment> findByFapClassIdAndStatusOrderByCreatedAtAscIdAsc(
			Long classId,
			ClassEnrollmentStatus status);

	@EntityGraph(attributePaths = {"fapClass", "user"})
	List<ClassEnrollment> findByFapClassIdAndStatusInOrderByCreatedAtAscIdAsc(
			Long classId,
			Collection<ClassEnrollmentStatus> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"fapClass", "user"})
	Optional<ClassEnrollment> findFirstByFapClassIdAndStatusOrderByCreatedAtAscIdAsc(
			Long classId,
			ClassEnrollmentStatus status);
}
