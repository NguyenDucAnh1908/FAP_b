package com.fap.training.mapper;

import com.fap.training.dto.TrainingRegistrationResponse;
import com.fap.training.entity.TrainingRegistration;
import com.fap.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class TrainingRegistrationMapper {

	public TrainingRegistrationResponse toResponse(TrainingRegistration registration) {
		User user = registration.getUser();
		return new TrainingRegistrationResponse(
				registration.getId(),
				registration.getTrainingSession().getId(),
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				registration.getStatus(),
				registration.getRegisteredAt(),
				registration.getCancelledAt(),
				registration.getCompletedAt());
	}
}
