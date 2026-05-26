package com.fap.clazz.mapper;

import com.fap.clazz.dto.ClassResponse;
import com.fap.clazz.entity.FapClass;
import com.fap.program.entity.TrainingProgram;
import org.springframework.stereotype.Component;

@Component
public class ClassMapper {

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
				fapClass.getCreatedBy(),
				fapClass.getUpdatedBy(),
				fapClass.getCreatedAt(),
				fapClass.getUpdatedAt());
	}
}
