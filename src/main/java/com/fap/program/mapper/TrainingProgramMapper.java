package com.fap.program.mapper;

import com.fap.program.dto.TrainingProgramResponse;
import com.fap.program.dto.TrainingProgramSyllabusResponse;
import com.fap.program.entity.TrainingProgram;
import com.fap.program.entity.TrainingProgramSyllabus;
import com.fap.syllabus.entity.Syllabus;
import org.springframework.stereotype.Component;

@Component
public class TrainingProgramMapper {

	public TrainingProgramResponse toResponse(TrainingProgram program) {
		return new TrainingProgramResponse(
				program.getId(),
				program.getName(),
				program.getStatus(),
				program.getDuration(),
				program.getTotalHours(),
				program.getVersion(),
				program.getCreatedBy(),
				program.getUpdatedBy(),
				program.getCreatedAt(),
				program.getUpdatedAt());
	}

	public TrainingProgramSyllabusResponse toResponse(TrainingProgramSyllabus item) {
		Syllabus syllabus = item.getSyllabus();
		return new TrainingProgramSyllabusResponse(
				syllabus.getId(),
				syllabus.getName(),
				syllabus.getCode(),
				syllabus.getVersion(),
				syllabus.getStatus(),
				item.getSortOrder());
	}
}
