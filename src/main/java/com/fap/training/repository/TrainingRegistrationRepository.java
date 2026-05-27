package com.fap.training.repository;

import com.fap.training.entity.TrainingRegistration;
import com.fap.training.enums.TrainingRegistrationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainingRegistrationRepository extends JpaRepository<TrainingRegistration, Long> {

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	Optional<TrainingRegistration> findByTrainingSessionIdAndUserId(Long trainingSessionId, Long userId);

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	List<TrainingRegistration> findByTrainingSessionIdAndStatusInOrderByRegisteredAtAscIdAsc(
			Long trainingSessionId,
			Collection<TrainingRegistrationStatus> statuses);

	@EntityGraph(attributePaths = {"trainingSession", "user"})
	Optional<TrainingRegistration> findFirstByTrainingSessionIdAndStatusOrderByRegisteredAtAscIdAsc(
			Long trainingSessionId,
			TrainingRegistrationStatus status);

	long countByTrainingSessionIdAndStatus(Long trainingSessionId, TrainingRegistrationStatus status);
}
