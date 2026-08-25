package com.fap.result.dto;

public record CourseResultSummaryResponse(
		long total,
		long inProgress,
		long passed,
		long failed,
		long withdrawn,
		long published
) {
}
