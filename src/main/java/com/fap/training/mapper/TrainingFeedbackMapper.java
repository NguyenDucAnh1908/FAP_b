package com.fap.training.mapper;

import com.fap.training.dto.TrainingFeedbackResponse;
import com.fap.training.entity.TrainingFeedback;
import com.fap.training.entity.TrainingSession;
import com.fap.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class TrainingFeedbackMapper {

	public TrainingFeedbackResponse toResponse(TrainingFeedback feedback) {
		TrainingSession session = feedback.getTrainingSession();
		User user = feedback.getUser();
		return new TrainingFeedbackResponse(
				feedback.getId(),
				session.getId(),
				session.getTitle(),
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				feedback.getRatingContent(),
				feedback.getRatingTrainer(),
				feedback.getRatingOrganization(),
				feedback.getComment(),
				feedback.getCreatedAt(),
				feedback.getUpdatedAt());
	}
}
