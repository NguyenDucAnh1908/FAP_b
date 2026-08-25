package com.fap.result.dto;

import java.util.List;

public record ClassCourseResultsResponse(
		Long classId,
		String className,
		String classCode,
		CourseResultSummaryResponse summary,
		List<CourseResultResponse> results
) {
}
