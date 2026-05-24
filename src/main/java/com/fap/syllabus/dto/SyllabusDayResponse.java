package com.fap.syllabus.dto;

import java.util.List;

public record SyllabusDayResponse(
		Long id,
		Integer dayNumber,
		Integer sortOrder,
		List<SyllabusUnitResponse> units
) {
}
