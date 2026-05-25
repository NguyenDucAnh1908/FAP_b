package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.syllabus.dto.CreateMaterialFileRequest;
import com.fap.syllabus.dto.MaterialFileResponse;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.mapper.MaterialFileMapper;
import com.fap.syllabus.repository.MaterialFileRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import com.fap.syllabus.repository.SyllabusTopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaterialFileService {

	private final SyllabusRepository syllabusRepository;
	private final SyllabusTopicRepository topicRepository;
	private final MaterialFileRepository materialFileRepository;
	private final MaterialFileMapper materialFileMapper;
	private final AuditLogService auditLogService;

	public MaterialFileService(
			SyllabusRepository syllabusRepository,
			SyllabusTopicRepository topicRepository,
			MaterialFileRepository materialFileRepository,
			MaterialFileMapper materialFileMapper,
			AuditLogService auditLogService) {
		this.syllabusRepository = syllabusRepository;
		this.topicRepository = topicRepository;
		this.materialFileRepository = materialFileRepository;
		this.materialFileMapper = materialFileMapper;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public List<MaterialFileResponse> list(Long syllabusId, Long topicId) {
		findTopic(syllabusId, topicId);
		return materialFileRepository.findByTopicIdOrderByUploadedAtDesc(topicId).stream()
				.map(materialFileMapper::toResponse)
				.toList();
	}

	@Transactional
	public MaterialFileResponse create(
			Long syllabusId,
			Long topicId,
			CreateMaterialFileRequest request,
			Long currentUserId) {
		findEditableSyllabus(syllabusId);
		SyllabusTopic topic = findTopic(syllabusId, topicId);
		MaterialFile materialFile = new MaterialFile();
		materialFile.setTopic(topic);
		materialFile.setFileName(request.fileName());
		materialFile.setFileUrl(request.fileUrl());
		materialFile.setFileSize(request.fileSize());
		materialFile.setContentType(request.contentType());
		materialFile.setUploadedBy(currentUserId);
		materialFile.setUploadedAt(LocalDateTime.now());
		MaterialFile saved = materialFileRepository.save(materialFile);
		auditLogService.record("CREATE_MATERIAL_FILE", "syllabus", syllabusId);
		return materialFileMapper.toResponse(saved);
	}

	@Transactional
	public void delete(Long syllabusId, Long topicId, Long materialId) {
		findEditableSyllabus(syllabusId);
		findTopic(syllabusId, topicId);
		MaterialFile materialFile = materialFileRepository.findByIdAndTopicId(materialId, topicId)
				.orElseThrow(() -> new NotFoundException("Material file not found"));
		materialFileRepository.delete(materialFile);
		auditLogService.record("DELETE_MATERIAL_FILE", "syllabus", syllabusId);
	}

	private Syllabus findEditableSyllabus(Long syllabusId) {
		Syllabus syllabus = syllabusRepository.findById(syllabusId)
				.orElseThrow(() -> new NotFoundException("Syllabus not found"));
		if (syllabus.getStatus() == SyllabusStatus.Active || syllabus.getStatus() == SyllabusStatus.Inactive) {
			throw new ConflictException("SYLLABUS_NOT_EDITABLE", "Only Drafting or Pending syllabus can be edited");
		}
		return syllabus;
	}

	private SyllabusTopic findTopic(Long syllabusId, Long topicId) {
		return topicRepository.findByIdAndUnitDaySyllabusId(topicId, syllabusId)
				.orElseThrow(() -> new NotFoundException("Syllabus topic not found"));
	}
}
