package com.fap.syllabus.dto;

import com.fap.syllabus.enums.SyllabusTopicStatus;

import java.util.List;

public record FullSyllabusTopicResponse(
		Long id,
		String name,
		String outputStandard,
		Boolean online,
		Integer durationMinutes,
		SyllabusTopicStatus status,
		Integer sortOrder,
		List<MaterialFileResponse> materials
) {
}
