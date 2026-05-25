package com.fap.program.dto;

import com.fap.syllabus.enums.SyllabusStatus;

public record TrainingProgramSyllabusResponse(
		Long syllabusId,
		String syllabusName,
		String syllabusCode,
		String syllabusVersion,
		SyllabusStatus syllabusStatus,
		Integer sortOrder
) {
}
