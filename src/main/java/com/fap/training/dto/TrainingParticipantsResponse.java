package com.fap.training.dto;

import java.util.List;

public record TrainingParticipantsResponse(
		Long trainingSessionId,
		Integer capacity,
		Integer enrolledCount,
		List<TrainingRegistrationResponse> registered,
		List<TrainingRegistrationResponse> waitlist,
		List<TrainingRegistrationResponse> completed
) {
}
