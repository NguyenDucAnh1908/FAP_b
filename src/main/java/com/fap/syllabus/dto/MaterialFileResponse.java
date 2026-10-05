package com.fap.syllabus.dto;

import java.time.LocalDateTime;

public record MaterialFileResponse(
		Long id,
		Long topicId,
		String fileName,
		String fileUrl,
		Long fileSize,
		String contentType,
		Long uploadedBy,
		LocalDateTime uploadedAt,
		boolean storedContent
) {
}
