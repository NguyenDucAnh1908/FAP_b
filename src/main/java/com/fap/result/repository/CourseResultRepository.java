package com.fap.result.repository;

import com.fap.result.entity.CourseResult;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface CourseResultRepository extends JpaRepository<CourseResult, Long> {
	@EntityGraph(attributePaths = {"fapClass", "classEnrollment", "classEnrollment.user"})
	List<CourseResult> findByFapClassIdOrderByClassEnrollmentUserFullNameAsc(Long classId);

	@EntityGraph(attributePaths = {"fapClass", "classEnrollment", "classEnrollment.user"})
	Optional<CourseResult> findByFapClassIdAndClassEnrollmentUserId(Long classId, Long userId);

	Optional<CourseResult> findByClassEnrollmentId(Long enrollmentId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"fapClass", "classEnrollment", "classEnrollment.user"})
	@Query("select r from CourseResult r where r.fapClass.id = :classId and r.classEnrollment.user.id = :userId")
	Optional<CourseResult> findForUpdate(@Param("classId") Long classId, @Param("userId") Long userId);

	long countByFapClassId(Long classId);
}
