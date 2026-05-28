package com.fap.training.mapper;

import com.fap.clazz.entity.FapClass;
import com.fap.training.dto.MyAttendanceResponse;
import com.fap.training.dto.MyTrainingRegistrationResponse;
import com.fap.training.dto.MyTrainingSessionResponse;
import com.fap.training.entity.AttendanceRecord;
import com.fap.training.entity.TrainingRegistration;
import com.fap.training.entity.TrainingSession;
import com.fap.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class MyTrainingMapper {

	public MyTrainingRegistrationResponse toRegistrationResponse(TrainingRegistration registration) {
		TrainingSession session = registration.getTrainingSession();
		FapClass fapClass = session.getFapClass();
		User trainer = session.getTrainer();
		return new MyTrainingRegistrationResponse(
				registration.getId(),
				registration.getStatus(),
				registration.getRegisteredAt(),
				registration.getCancelledAt(),
				registration.getCompletedAt(),
				session.getId(),
				session.getTitle(),
				session.getStatus(),
				session.getSessionType(),
				session.getSessionDate(),
				session.getStartTime(),
				session.getEndTime(),
				session.getRoom(),
				session.getMeetingLink(),
				fapClass.getId(),
				fapClass.getName(),
				fapClass.getClassCode(),
				trainer.getId(),
				trainer.getFullName(),
				trainer.getEmail());
	}

	public MyTrainingSessionResponse toSessionResponse(TrainingRegistration registration) {
		TrainingSession session = registration.getTrainingSession();
		FapClass fapClass = session.getFapClass();
		User trainer = session.getTrainer();
		return new MyTrainingSessionResponse(
				session.getId(),
				session.getTitle(),
				session.getDescription(),
				session.getStatus(),
				session.getSessionType(),
				session.getSessionDate(),
				session.getStartTime(),
				session.getEndTime(),
				session.getRoom(),
				session.getMeetingLink(),
				session.getCapacity(),
				session.getEnrolledCount(),
				fapClass.getId(),
				fapClass.getName(),
				fapClass.getClassCode(),
				trainer.getId(),
				trainer.getFullName(),
				trainer.getEmail(),
				registration.getId(),
				registration.getStatus(),
				registration.getRegisteredAt(),
				registration.getCancelledAt(),
				registration.getCompletedAt());
	}

	public MyAttendanceResponse toAttendanceResponse(AttendanceRecord record) {
		TrainingSession session = record.getTrainingSession();
		FapClass fapClass = session.getFapClass();
		User trainer = session.getTrainer();
		return new MyAttendanceResponse(
				record.getId(),
				record.getStatus(),
				record.getCheckedInAt(),
				record.getCheckInMethod(),
				record.getCorrectionReason(),
				record.getCreatedAt(),
				record.getUpdatedAt(),
				session.getId(),
				session.getTitle(),
				session.getStatus(),
				session.getSessionType(),
				session.getSessionDate(),
				session.getStartTime(),
				session.getEndTime(),
				session.getRoom(),
				session.getMeetingLink(),
				fapClass.getId(),
				fapClass.getName(),
				fapClass.getClassCode(),
				trainer.getId(),
				trainer.getFullName(),
				trainer.getEmail());
	}
}
