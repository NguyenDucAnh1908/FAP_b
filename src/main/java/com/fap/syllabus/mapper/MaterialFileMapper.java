package com.fap.syllabus.mapper;

import com.fap.syllabus.dto.MaterialFileResponse;
import com.fap.syllabus.entity.MaterialFile;
import org.springframework.stereotype.Component;

@Component
public class MaterialFileMapper {

	public MaterialFileResponse toResponse(MaterialFile materialFile) {
		return new MaterialFileResponse(
				materialFile.getId(),
				materialFile.getTopic().getId(),
				materialFile.getFileName(),
				materialFile.getFileUrl(),
				materialFile.getFileSize(),
				materialFile.getContentType(),
				materialFile.getUploadedBy(),
				materialFile.getUploadedAt());
	}
}
