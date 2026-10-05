package com.fap.clazz.mapper;

import com.fap.clazz.dto.ClassEnrollmentResponse;
import com.fap.clazz.entity.ClassEnrollment;
import org.springframework.stereotype.Component;

@Component
public class ClassEnrollmentMapper {

	public ClassEnrollmentResponse toResponse(ClassEnrollment enrollment) {
		return new ClassEnrollmentResponse(
				enrollment.getId(),
				enrollment.getFapClass().getId(),
				enrollment.getFapClass().getName(),
				enrollment.getFapClass().getClassCode(),
				enrollment.getUser().getId(),
				enrollment.getUser().getFullName(),
				enrollment.getUser().getEmail(),
				enrollment.getStatus(),
				enrollment.getSource(),
				enrollment.getEnrolledAt(),
				enrollment.getWithdrawnAt(),
				enrollment.getCompletedAt(),
				enrollment.getReviewedAt(),
				enrollment.getReviewedBy(),
				enrollment.getCreatedAt(),
				enrollment.getUpdatedAt());
	}
}
