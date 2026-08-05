package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.ForbiddenException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.api.PageRequestFactory;
import com.fap.common.util.FileValidator;
import com.fap.syllabus.dto.AssignedMaterialFileResponse;
import com.fap.syllabus.dto.CreateMaterialFileRequest;
import com.fap.syllabus.dto.CreateMaterialRequest;
import com.fap.syllabus.dto.MaterialFileDownload;
import com.fap.syllabus.dto.MaterialFileResponse;
import com.fap.syllabus.dto.UpdateMaterialFileRequest;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.MaterialFileContent;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.mapper.MaterialFileMapper;
import com.fap.syllabus.repository.MaterialFileContentRepository;
import com.fap.syllabus.repository.MaterialFileRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import com.fap.syllabus.repository.SyllabusTopicRepository;
import com.fap.training.enums.TrainingRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
public class MaterialFileService {

	private static final Collection<TrainingRegistrationStatus> ELIGIBLE_REGISTRATION_STATUSES = List.of(
			TrainingRegistrationStatus.Registered,
			TrainingRegistrationStatus.Completed);

	private static final String DEFAULT_DOWNLOAD_CONTENT_TYPE = "application/octet-stream";

	/** Placeholder held only between save() and the id-dependent download URL being set. */
	private static final String PENDING_FILE_URL = "pending";

	public static String downloadPath(Long materialId) {
		return "/api/v1/materials/" + materialId + "/download";
	}

	private final SyllabusRepository syllabusRepository;
	private final SyllabusTopicRepository topicRepository;
	private final MaterialFileRepository materialFileRepository;
	private final MaterialFileContentRepository materialFileContentRepository;
	private final MaterialFileMapper materialFileMapper;
	private final FileValidator fileValidator;
	private final AuditLogService auditLogService;

	public MaterialFileService(
			SyllabusRepository syllabusRepository,
			SyllabusTopicRepository topicRepository,
			MaterialFileRepository materialFileRepository,
			MaterialFileContentRepository materialFileContentRepository,
			MaterialFileMapper materialFileMapper,
			FileValidator fileValidator,
			AuditLogService auditLogService) {
		this.syllabusRepository = syllabusRepository;
		this.topicRepository = topicRepository;
		this.materialFileRepository = materialFileRepository;
		this.materialFileContentRepository = materialFileContentRepository;
		this.materialFileMapper = materialFileMapper;
		this.fileValidator = fileValidator;
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
		return listLibrary(syllabusId, topicId, keyword, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<MaterialFileResponse> listLibrary(
			Long syllabusId,
			Long topicId,
			String keyword,
			int page,
			int limit,
			String sortBy,
			String order) {
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.DESC, "uploadedAt", "id"),
				"id", "uploadedAt", "fileName", "contentType", "fileSize");
		return materialFileRepository.search(syllabusId, topicId, normalize(keyword), pageRequest)
				.map(materialFileMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public MaterialFileResponse get(Long materialId) {
		return materialFileMapper.toResponse(findMaterial(materialId));
	}

	@Transactional(readOnly = true)
	public Page<AssignedMaterialFileResponse> assignedToUser(Long currentUserId, String keyword, int page, int limit) {
		return assignedToUser(currentUserId, keyword, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<AssignedMaterialFileResponse> assignedToUser(
			Long currentUserId,
			String keyword,
			int page,
			int limit,
			String sortBy,
			String order) {
		PageRequest pageRequest = PageRequestFactory.create(
				page,
				limit,
				sortBy,
				order,
				Sort.by(Sort.Direction.DESC, "uploadedAt", "id"),
				"id", "uploadedAt", "fileName", "contentType", "fileSize");
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

	/**
	 * Stores an uploaded file's bytes inside Oracle and points {@code fileUrl} at the download
	 * endpoint, so callers see internal uploads and external links through the same field.
	 */
	@Transactional
	public MaterialFileResponse upload(Long syllabusId, Long topicId, MultipartFile file, Long currentUserId) {
		findEditableSyllabus(syllabusId);
		SyllabusTopic topic = findTopic(syllabusId, topicId);
		fileValidator.validateUpload(file);
		String fileName = fileValidator.sanitizeFileName(file.getOriginalFilename());

		byte[] data;
		try {
			data = file.getBytes();
		} catch (IOException exception) {
			throw new BadRequestException("FILE_UNREADABLE", "Uploaded file could not be read");
		}

		MaterialFile materialFile = new MaterialFile();
		materialFile.setTopic(topic);
		materialFile.setFileName(fileName);
		materialFile.setFileSize((long) data.length);
		materialFile.setContentType(normalize(file.getContentType()));
		materialFile.setUploadedBy(currentUserId);
		materialFile.setUploadedAt(LocalDateTime.now());
		// file_url is NOT NULL, and the download path needs the generated id — save first, then set it.
		materialFile.setFileUrl(PENDING_FILE_URL);
		MaterialFile saved = materialFileRepository.save(materialFile);
		saved.setFileUrl(downloadPath(saved.getId()));

		MaterialFileContent content = new MaterialFileContent();
		content.setMaterialFile(saved);
		content.setFileData(data);
		materialFileContentRepository.save(content);

		auditLogService.record("UPLOAD_MATERIAL_FILE", "syllabus", syllabusId);
		return materialFileMapper.toResponse(saved);
	}

	/**
	 * Reads an internally stored material after an ownership check.
	 *
	 * <p>{@code canManageMaterials} is decided by the caller from an action-based permission check,
	 * never from a role name or level comparison. Trainees hold {@code learning_material:view}
	 * globally, so without the registration probe below any trainee could read any material.
	 */
	@Transactional(readOnly = true)
	public MaterialFileDownload download(Long materialId, Long currentUserId, boolean canManageMaterials) {
		MaterialFile materialFile = findMaterial(materialId);
		if (!canManageMaterials
				&& !materialFileRepository.existsAssignedToUser(
						materialId, currentUserId, ELIGIBLE_REGISTRATION_STATUSES)) {
			throw new ForbiddenException("You are not assigned to this material");
		}
		MaterialFileContent content = materialFileContentRepository.findById(materialId)
				.orElseThrow(() -> new NotFoundException("Material file has no stored content"));
		return new MaterialFileDownload(
				materialFile.getFileName(),
				materialFile.getContentType() == null
						? DEFAULT_DOWNLOAD_CONTENT_TYPE
						: materialFile.getContentType(),
				content.getFileData());
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
