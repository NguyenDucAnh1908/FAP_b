package com.fap.syllabus.dto;

import java.util.List;

public record FullSyllabusUnitResponse(
		Long id,
		String name,
		Integer sortOrder,
		List<FullSyllabusTopicResponse> topics
) {
}
