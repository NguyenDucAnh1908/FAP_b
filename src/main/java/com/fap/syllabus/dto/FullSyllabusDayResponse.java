package com.fap.syllabus.dto;

import java.util.List;

public record FullSyllabusDayResponse(
		Long id,
		Integer dayNumber,
		Integer sortOrder,
		List<FullSyllabusUnitResponse> units
) {
}
