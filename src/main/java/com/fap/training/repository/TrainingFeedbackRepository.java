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

	@Query("""
			select count(f) as feedbackCount,
			       avg(f.ratingContent) as averageContentRating,
			       avg(f.ratingTrainer) as averageTrainerRating,
			       avg(f.ratingOrganization) as averageOrganizationRating
			from TrainingFeedback f
			where (:classAdminId is null
			       or exists (select ca.id from ClassAdmin ca
			                  where ca.fapClass = f.trainingSession.fapClass
			                    and ca.user.id = :classAdminId))
			  and (:fromDate is null or f.trainingSession.sessionDate >= :fromDate)
			  and (:toDate is null or f.trainingSession.sessionDate <= :toDate)
			""")
	AnalyticsFeedbackSummary summarizeForAnalytics(
			@Param("classAdminId") Long classAdminId,
			@Param("fromDate") java.time.LocalDate fromDate,
			@Param("toDate") java.time.LocalDate toDate);

	interface AnalyticsFeedbackSummary {
		Long getFeedbackCount();

		Double getAverageContentRating();

		Double getAverageTrainerRating();

		Double getAverageOrganizationRating();
	}

	boolean existsByTrainingSessionIdAndUserId(Long trainingSessionId, Long userId);

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	Page<TrainingFeedback> findByUserId(Long userId, Pageable pageable);

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
