package com.fap.training.mapper;

import com.fap.training.dto.AttendanceRecordResponse;
import com.fap.training.entity.AttendanceRecord;
import com.fap.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AttendanceRecordMapper {

	public AttendanceRecordResponse toResponse(AttendanceRecord record) {
		User user = record.getUser();
		return new AttendanceRecordResponse(
				record.getId(),
				record.getTrainingSession().getId(),
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				record.getStatus(),
				record.getCheckedInAt(),
				record.getCheckInMethod(),
				record.getUpdatedBy(),
				record.getCorrectionReason(),
				record.getCreatedAt(),
				record.getUpdatedAt());
	}
}
