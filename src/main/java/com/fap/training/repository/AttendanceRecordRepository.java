package com.fap.training.repository;

import com.fap.training.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	Optional<AttendanceRecord> findByTrainingSessionIdAndUserId(Long trainingSessionId, Long userId);

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	List<AttendanceRecord> findByTrainingSessionIdOrderByUserFullNameAsc(Long trainingSessionId);

	long countByTrainingSessionId(Long trainingSessionId);
}
