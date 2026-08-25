package com.fap.syllabus.mapper;

import com.fap.syllabus.dto.AssignedMaterialFileResponse;
import com.fap.syllabus.dto.MaterialFileResponse;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.repository.MaterialFileContentRepository;
import org.springframework.stereotype.Component;

@Component
public class MaterialFileMapper {
	private final MaterialFileContentRepository materialFileContentRepository;

	public MaterialFileMapper(MaterialFileContentRepository materialFileContentRepository) {
		this.materialFileContentRepository = materialFileContentRepository;
	}

	public MaterialFileResponse toResponse(MaterialFile materialFile) {
		return new MaterialFileResponse(
				materialFile.getId(),
				materialFile.getTopic().getId(),
				materialFile.getFileName(),
				materialFile.getFileUrl(),
				materialFile.getFileSize(),
				materialFile.getContentType(),
				materialFile.getUploadedBy(),
				materialFile.getUploadedAt(),
				materialFileContentRepository.existsById(materialFile.getId()));
	}

	public AssignedMaterialFileResponse toAssignedResponse(MaterialFile materialFile) {
		Syllabus syllabus = materialFile.getTopic().getUnit().getDay().getSyllabus();
		return new AssignedMaterialFileResponse(
				materialFile.getId(),
				syllabus.getId(),
				syllabus.getName(),
				syllabus.getCode(),
				materialFile.getTopic().getId(),
				materialFile.getTopic().getName(),
				materialFile.getFileName(),
				materialFile.getFileUrl(),
				materialFile.getFileSize(),
				materialFile.getContentType(),
				materialFile.getUploadedBy(),
				materialFile.getUploadedAt(),
				materialFileContentRepository.existsById(materialFile.getId()));
	}
}
