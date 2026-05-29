package com.fap.syllabus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SyllabusImportResponse(
		int totalRows,
		int successCount,
		int failedCount,
		List<ImportError> errors,
		List<SyllabusResponse> createdSyllabuses
) {

	public record ImportError(
			int row,
			String field,
			String message
	) {
	}
}

