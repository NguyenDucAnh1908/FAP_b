package com.fap.syllabus.dto;

import java.util.List;

public record FullSyllabusResponse(
		SyllabusResponse syllabus,
		List<String> outputStandards,
		List<FullSyllabusDayResponse> days
) {
}
