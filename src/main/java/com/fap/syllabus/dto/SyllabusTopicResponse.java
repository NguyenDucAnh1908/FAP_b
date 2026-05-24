package com.fap.syllabus.dto;

import com.fap.syllabus.enums.SyllabusTopicStatus;

public record SyllabusTopicResponse(
		Long id,
		String name,
		String outputStandard,
		Boolean online,
		Integer durationMinutes,
		SyllabusTopicStatus status,
		Integer sortOrder
) {
}
