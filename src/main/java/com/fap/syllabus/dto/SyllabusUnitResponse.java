package com.fap.syllabus.dto;

import java.util.List;

public record SyllabusUnitResponse(
		Long id,
		String name,
		Integer sortOrder,
		List<SyllabusTopicResponse> topics
) {
}
