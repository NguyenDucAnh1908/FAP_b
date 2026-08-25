package com.fap.result.repository;

import com.fap.result.entity.CourseResultAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseResultAdjustmentRepository extends JpaRepository<CourseResultAdjustment, Long> {
	List<CourseResultAdjustment> findByCourseResultIdOrderByAdjustedAtDescIdDesc(Long courseResultId);
}
