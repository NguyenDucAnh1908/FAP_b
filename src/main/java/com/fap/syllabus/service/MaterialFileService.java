package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.syllabus.dto.AssignedMaterialFileResponse;
import com.fap.syllabus.dto.CreateMaterialFileRequest;
import com.fap.syllabus.dto.CreateMaterialRequest;
import com.fap.syllabus.dto.MaterialFileResponse;
import com.fap.syllabus.dto.UpdateMaterialFileRequest;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.mapper.MaterialFileMapper;
import com.fap.syllabus.repository.MaterialFileRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import com.fap.syllabus.repository.SyllabusTopicRepository;
import com.fap.training.enums.TrainingRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
public class MaterialFileService {

	private static final Collection<TrainingRegistrationStatus> ELIGIBLE_REGISTRATION_STATUSES = List.of(
			TrainingRegistrationStatus.Registered,
			TrainingRegistrationStatus.Completed);

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

	@Transactional(readOnly = true)
	public Page<MaterialFileResponse> listLibrary(Long syllabusId, Long topicId, String keyword, int page, int limit) {
		PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "uploadedAt", "id"));
		return materialFileRepository.search(syllabusId, topicId, normalize(keyword), pageRequest)
				.map(materialFileMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public MaterialFileResponse get(Long materialId) {
		return materialFileMapper.toResponse(findMaterial(materialId));
	}

	@Transactional(readOnly = true)
	public Page<AssignedMaterialFileResponse> assignedToUser(Long currentUserId, String keyword, int page, int limit) {
		PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "uploadedAt", "id"));
		return materialFileRepository.searchAssignedToUser(
						currentUserId,
						ELIGIBLE_REGISTRATION_STATUSES,
						normalize(keyword),
						pageRequest)
				.map(materialFileMapper::toAssignedResponse);
	}

	@Transactional
	public MaterialFileResponse create(CreateMaterialRequest request, Long currentUserId) {
		CreateMaterialFileRequest materialRequest = new CreateMaterialFileRequest(
				request.fileName(),
				request.fileUrl(),
				request.fileSize(),
				request.contentType());
		return create(request.syllabusId(), request.topicId(), materialRequest, currentUserId);
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
		applyFields(materialFile, request.fileName(), request.fileUrl(), request.fileSize(), request.contentType());
		materialFile.setUploadedBy(currentUserId);
		materialFile.setUploadedAt(LocalDateTime.now());
		MaterialFile saved = materialFileRepository.save(materialFile);
		auditLogService.record("CREATE_MATERIAL_FILE", "syllabus", syllabusId);
		return materialFileMapper.toResponse(saved);
	}

	@Transactional
	public MaterialFileResponse update(Long materialId, UpdateMaterialFileRequest request) {
		MaterialFile materialFile = findMaterial(materialId);
		Syllabus syllabus = materialFile.getTopic().getUnit().getDay().getSyllabus();
		ensureEditable(syllabus);
		applyFields(materialFile, request.fileName(), request.fileUrl(), request.fileSize(), request.contentType());
		auditLogService.record("UPDATE_MATERIAL_FILE", "syllabus", syllabus.getId());
		return materialFileMapper.toResponse(materialFile);
	}

	@Transactional
	public void delete(Long materialId) {
		MaterialFile materialFile = findMaterial(materialId);
		Syllabus syllabus = materialFile.getTopic().getUnit().getDay().getSyllabus();
		ensureEditable(syllabus);
		materialFileRepository.delete(materialFile);
		auditLogService.record("DELETE_MATERIAL_FILE", "syllabus", syllabus.getId());
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
		ensureEditable(syllabus);
		return syllabus;
	}

	private void ensureEditable(Syllabus syllabus) {
		if (syllabus.getStatus() == SyllabusStatus.Active || syllabus.getStatus() == SyllabusStatus.Inactive) {
			throw new ConflictException("SYLLABUS_NOT_EDITABLE", "Only Drafting or Pending syllabus can be edited");
		}
	}

	private SyllabusTopic findTopic(Long syllabusId, Long topicId) {
		return topicRepository.findByIdAndUnitDaySyllabusId(topicId, syllabusId)
				.orElseThrow(() -> new NotFoundException("Syllabus topic not found"));
	}

	private MaterialFile findMaterial(Long materialId) {
		return materialFileRepository.findWithTopicById(materialId)
				.orElseThrow(() -> new NotFoundException("Material file not found"));
	}

	private void applyFields(
			MaterialFile materialFile,
			String fileName,
			String fileUrl,
			Long fileSize,
			String contentType) {
		materialFile.setFileName(fileName.trim());
		materialFile.setFileUrl(fileUrl.trim());
		materialFile.setFileSize(fileSize);
		materialFile.setContentType(normalize(contentType));
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
