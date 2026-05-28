package com.fap.training.repository;

import com.fap.training.entity.TrainingFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrainingFeedbackRepository extends JpaRepository<TrainingFeedback, Long> {

	boolean existsByTrainingSessionIdAndUserId(Long trainingSessionId, Long userId);

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	Page<TrainingFeedback> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	Page<TrainingFeedback> findByTrainingSessionIdOrderByCreatedAtDesc(Long trainingSessionId, Pageable pageable);

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	Optional<TrainingFeedback> findByTrainingSessionIdAndUserId(Long trainingSessionId, Long userId);

	@Query("""
			select count(f) as feedbackCount,
			       avg(f.ratingContent) as averageContentRating,
			       avg(f.ratingTrainer) as averageTrainerRating,
			       avg(f.ratingOrganization) as averageOrganizationRating
			from TrainingFeedback f
			where f.trainingSession.id = :trainingSessionId
			""")
	FeedbackSummary summarizeByTrainingSessionId(@Param("trainingSessionId") Long trainingSessionId);

	interface FeedbackSummary {
		Long getFeedbackCount();

		Double getAverageContentRating();

		Double getAverageTrainerRating();

		Double getAverageOrganizationRating();
	}
}
