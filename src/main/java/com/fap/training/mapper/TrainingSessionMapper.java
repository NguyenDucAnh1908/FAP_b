package com.fap.training.mapper;

import com.fap.clazz.entity.FapClass;
import com.fap.training.dto.TrainingSessionResponse;
import com.fap.training.entity.TrainingSession;
import com.fap.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class TrainingSessionMapper {

	public TrainingSessionResponse toResponse(TrainingSession session) {
		FapClass fapClass = session.getFapClass();
		User trainer = session.getTrainer();
		return new TrainingSessionResponse(
				session.getId(),
				fapClass.getId(),
				fapClass.getName(),
				fapClass.getClassCode(),
				session.getTitle(),
				session.getDescription(),
				trainer.getId(),
				trainer.getFullName(),
				trainer.getEmail(),
				session.getRoom(),
				session.getSessionDate(),
				session.getStartTime(),
				session.getEndTime(),
				session.getSessionType(),
				session.getMeetingLink(),
				session.getCapacity(),
				session.getEnrolledCount(),
				session.getStatus(),
				session.getCreatedAt(),
				session.getUpdatedAt());
	}
}
