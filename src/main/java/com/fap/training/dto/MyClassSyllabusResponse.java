package com.fap.training.dto;

import com.fap.syllabus.enums.SyllabusStatus;

public record MyClassSyllabusResponse(
		Long syllabusId,
		String name,
		String code,
		String version,
		SyllabusStatus status,
		String levelName,
		String duration,
		Integer sortOrder
) {
}
