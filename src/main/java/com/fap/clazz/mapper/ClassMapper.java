package com.fap.clazz.mapper;

import com.fap.clazz.dto.ClassResponse;
import com.fap.clazz.entity.FapClass;
import com.fap.program.entity.TrainingProgram;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.repository.ClassEnrollmentRepository;
import org.springframework.stereotype.Component;

@Component
public class ClassMapper {
	private final ClassEnrollmentRepository classEnrollmentRepository;

	public ClassMapper(ClassEnrollmentRepository classEnrollmentRepository) {
		this.classEnrollmentRepository = classEnrollmentRepository;
	}

	public ClassResponse toResponse(FapClass fapClass) {
		TrainingProgram program = fapClass.getTrainingProgram();
		return new ClassResponse(
				fapClass.getId(),
				fapClass.getName(),
				fapClass.getClassCode(),
				program.getId(),
				program.getName(),
				fapClass.getStatus(),
				fapClass.getLocation(),
				fapClass.getLocationDetail(),
				fapClass.getFsu(),
				fapClass.getClassTime(),
				fapClass.getStartDate(),
				fapClass.getEndDate(),
				fapClass.getDuration(),
				fapClass.getCapacity(),
				classEnrollmentRepository.countByFapClassIdAndStatus(fapClass.getId(), ClassEnrollmentStatus.Enrolled),
				classEnrollmentRepository.countByFapClassIdAndStatus(fapClass.getId(), ClassEnrollmentStatus.Waitlisted),
				fapClass.isSelfEnrollmentEnabled(),
				fapClass.getEnrollmentStartDate(),
				fapClass.getEnrollmentEndDate(),
				fapClass.getMinimumAttendanceRate(),
				fapClass.getCreatedBy(),
				fapClass.getUpdatedBy(),
				fapClass.getCreatedAt(),
				fapClass.getUpdatedAt());
	}
}
