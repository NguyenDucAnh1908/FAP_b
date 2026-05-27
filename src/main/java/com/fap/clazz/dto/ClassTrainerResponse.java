package com.fap.clazz.dto;

public record ClassTrainerResponse(
		Long id,
		Long userId,
		String userFullName,
		String userEmail,
		Long syllabusId,
		String syllabusName,
		String syllabusCode
) {
}
