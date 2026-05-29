package com.fap.syllabus.dto;

import java.time.LocalDateTime;

public record AssignedMaterialFileResponse(
		Long id,
		Long syllabusId,
		String syllabusName,
		String syllabusCode,
		Long topicId,
		String topicName,
		String fileName,
		String fileUrl,
		Long fileSize,
		String contentType,
		Long uploadedBy,
		LocalDateTime uploadedAt
) {
}
