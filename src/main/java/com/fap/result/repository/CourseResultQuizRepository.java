package com.fap.result.repository;

import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.result.entity.CourseResultQuiz;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CourseResultQuizRepository extends JpaRepository<CourseResultQuiz, Long> {
	@EntityGraph(attributePaths = "quiz")
	List<CourseResultQuiz> findByCourseResultIdOrderByIdAsc(Long courseResultId);

	void deleteByCourseResultId(Long courseResultId);

	@EntityGraph(attributePaths = "quiz")
	List<CourseResultQuiz> findByCourseResultFapClassIdOrderByIdAsc(Long classId);

	/**
	 * One bulk delete of the quiz snapshots of a class's results whose enrollment is in
	 * {@code enrollmentStatuses}, instead of a select plus a delete per snapshot. Selecting by class
	 * keeps it clear of Oracle's 1000-element IN-list limit.
	 */
	@Modifying(flushAutomatically = true)
	@Query("""
			delete from CourseResultQuiz q
			where q.courseResult.id in (
			    select r.id
			    from CourseResult r
			    where r.fapClass.id = :classId
			      and r.classEnrollment.status in :enrollmentStatuses)
			""")
	int deleteForClassEnrollmentStatuses(
			@Param("classId") Long classId,
			@Param("enrollmentStatuses") Collection<ClassEnrollmentStatus> enrollmentStatuses);
}
