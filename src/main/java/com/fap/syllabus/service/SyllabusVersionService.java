package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.syllabus.dto.CloneSyllabusRequest;
import com.fap.syllabus.dto.FullSyllabusResponse;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusDay;
import com.fap.syllabus.entity.SyllabusOutputStandard;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.entity.SyllabusUnit;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.mapper.SyllabusMapper;
import com.fap.syllabus.mapper.SyllabusOutlineMapper;
import com.fap.syllabus.repository.MaterialFileContentRepository;
import com.fap.syllabus.repository.SyllabusDayRepository;
import com.fap.syllabus.repository.SyllabusOutputStandardRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Clones a published (Active or Inactive) syllabus as a new Drafting version, including its outline,
 * output standards and stored material bytes.
 */
@Service
public class SyllabusVersionService {
	private static final String UNAVAILABLE_FILE_URL = "unavailable";

	private final SyllabusRepository syllabusRepository;
	private final SyllabusDayRepository dayRepository;
	private final MaterialFileContentRepository materialFileContentRepository;
	private final SyllabusOutputStandardRepository outputStandardRepository;
	private final SyllabusMapper syllabusMapper;
	private final SyllabusOutlineMapper outlineMapper;
	private final AuditLogService auditLogService;

	public SyllabusVersionService(
			SyllabusRepository syllabusRepository,
			SyllabusDayRepository dayRepository,
			MaterialFileContentRepository materialFileContentRepository,
			SyllabusOutputStandardRepository outputStandardRepository,
			SyllabusMapper syllabusMapper,
			SyllabusOutlineMapper outlineMapper,
			AuditLogService auditLogService) {
		this.syllabusRepository = syllabusRepository;
		this.dayRepository = dayRepository;
		this.materialFileContentRepository = materialFileContentRepository;
		this.outputStandardRepository = outputStandardRepository;
		this.syllabusMapper = syllabusMapper;
		this.outlineMapper = outlineMapper;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public FullSyllabusResponse cloneVersion(Long sourceId, CloneSyllabusRequest request, Long currentUserId) {
		Syllabus source = syllabusRepository.getOrThrow(sourceId);
		if (source.getStatus() != SyllabusStatus.Active && source.getStatus() != SyllabusStatus.Inactive) {
			throw new ConflictException(
					"SYLLABUS_CLONE_NOT_ALLOWED",
					"Only Active or Inactive syllabuses can be cloned as a new version");
		}
		if (syllabusRepository.existsByCodeIgnoreCase(request.code())) {
			throw new ConflictException("SYLLABUS_CODE_EXISTS", "Syllabus code already exists");
		}

		LocalDateTime now = LocalDateTime.now();
		Syllabus cloned = new Syllabus();
		copyVersionFields(source, cloned);
		cloned.setName(request.name().trim());
		cloned.setCode(request.code().trim().toUpperCase());
		cloned.setVersion(request.version().trim());
		cloned.setStatus(SyllabusStatus.Drafting);
		cloned.setCreatedBy(currentUserId);
		cloned.setUpdatedBy(currentUserId);
		cloned.setCreatedAt(now);
		cloned.setUpdatedAt(now);
		Syllabus saved = syllabusRepository.save(cloned);

		List<SyllabusOutputStandard> outputStandards = outputStandardRepository
				.findByIdSyllabusIdOrderByIdStandardCodeAsc(sourceId)
				.stream()
				.map(item -> SyllabusRules.createOutputStandard(saved, item.getStandardCode()))
				.toList();
		outputStandardRepository.saveAll(outputStandards);

		List<SyllabusDay> sourceDays = dayRepository.findBySyllabusIdOrderBySortOrderAsc(sourceId);
		// Only which materials have bytes is read here; the bytes are copied inside Oracle below.
		Set<Long> storedMaterialIds = materialFileContentRepository.findStoredMaterialIdsBySyllabusId(sourceId);

		List<MaterialContentCopy> contentCopies = new ArrayList<>();
		List<SyllabusDay> clonedDays = sourceDays.stream()
				.map(day -> cloneDay(saved, day, storedMaterialIds, contentCopies))
				.toList();
		dayRepository.saveAll(clonedDays);
		dayRepository.flush();
		copyMaterialContents(contentCopies);

		auditLogService.record("CLONE_SYLLABUS_VERSION:" + sourceId, "syllabus", saved.getId());
		return new FullSyllabusResponse(
				syllabusMapper.toResponse(saved),
				outputStandards.stream().map(SyllabusOutputStandard::getStandardCode).toList(),
				clonedDays.stream().map(outlineMapper::toFullResponse).toList());
	}

	private void copyVersionFields(Syllabus source, Syllabus target) {
		target.setLevelName(source.getLevelName());
		target.setAttendees(source.getAttendees());
		target.setDuration(source.getDuration());
		target.setTechnicalRequirements(source.getTechnicalRequirements());
		target.setCourseObjectives(source.getCourseObjectives());
		target.setRules(source.getRules());
		target.setTimeAllocAssignmentLab(source.getTimeAllocAssignmentLab());
		target.setTimeAllocConceptLecture(source.getTimeAllocConceptLecture());
		target.setTimeAllocGuideReview(source.getTimeAllocGuideReview());
		target.setTimeAllocTestQuiz(source.getTimeAllocTestQuiz());
		target.setAssessQuizPct(source.getAssessQuizPct());
		target.setAssessAssignmentPct(source.getAssessAssignmentPct());
		target.setAssessFinalPct(source.getAssessFinalPct());
		target.setAssessmentText(source.getAssessmentText());
	}

	private SyllabusDay cloneDay(
			Syllabus syllabus,
			SyllabusDay source,
			Set<Long> storedMaterialIds,
			List<MaterialContentCopy> contentCopies) {
		SyllabusDay cloned = new SyllabusDay();
		cloned.setSyllabus(syllabus);
		cloned.setDayNumber(source.getDayNumber());
		cloned.setSortOrder(source.getSortOrder());
		cloned.setUnits(new ArrayList<>(source.getUnits().stream()
				.map(unit -> cloneUnit(cloned, unit, storedMaterialIds, contentCopies))
				.toList()));
		return cloned;
	}

	private SyllabusUnit cloneUnit(
			SyllabusDay day,
			SyllabusUnit source,
			Set<Long> storedMaterialIds,
			List<MaterialContentCopy> contentCopies) {
		SyllabusUnit cloned = new SyllabusUnit();
		cloned.setDay(day);
		cloned.setName(source.getName());
		cloned.setSortOrder(source.getSortOrder());
		cloned.setTopics(new ArrayList<>(source.getTopics().stream()
				.map(topic -> cloneTopic(cloned, topic, storedMaterialIds, contentCopies))
				.toList()));
		return cloned;
	}

	private SyllabusTopic cloneTopic(
			SyllabusUnit unit,
			SyllabusTopic source,
			Set<Long> storedMaterialIds,
			List<MaterialContentCopy> contentCopies) {
		SyllabusTopic cloned = new SyllabusTopic();
		cloned.setUnit(unit);
		cloned.setName(source.getName());
		cloned.setOutputStandard(source.getOutputStandard());
		cloned.setOnline(source.isOnline());
		cloned.setDurationMinutes(source.getDurationMinutes());
		cloned.setStatus(source.getStatus());
		cloned.setSortOrder(source.getSortOrder());
		cloned.setMaterials(new ArrayList<>(source.getMaterials().stream()
				.map(material -> cloneMaterial(cloned, material, storedMaterialIds, contentCopies))
				.toList()));
		return cloned;
	}

	private MaterialFile cloneMaterial(
			SyllabusTopic topic,
			MaterialFile source,
			Set<Long> storedMaterialIds,
			List<MaterialContentCopy> contentCopies) {
		MaterialFile cloned = new MaterialFile();
		cloned.setTopic(topic);
		cloned.setFileName(source.getFileName());
		cloned.setFileSize(source.getFileSize());
		cloned.setContentType(source.getContentType());
		cloned.setUploadedBy(source.getUploadedBy());
		cloned.setUploadedAt(source.getUploadedAt());

		if (storedMaterialIds.contains(source.getId())) {
			cloned.setFileUrl(MaterialFileService.PENDING_FILE_URL);
			contentCopies.add(new MaterialContentCopy(cloned, source.getId()));
		} else if (MaterialFileService.isInternalDownloadPath(source.getFileUrl())) {
			cloned.setFileUrl(UNAVAILABLE_FILE_URL);
		} else {
			cloned.setFileUrl(source.getFileUrl());
		}
		return cloned;
	}

	private void copyMaterialContents(List<MaterialContentCopy> contentCopies) {
		for (MaterialContentCopy copy : contentCopies) {
			MaterialFile material = copy.material();
			material.setFileUrl(MaterialFileService.downloadPath(material.getId()));
			materialFileContentRepository.copyStoredContent(copy.sourceMaterialId(), material.getId());
			material.setContentStored(true);
		}
	}

	private record MaterialContentCopy(MaterialFile material, Long sourceMaterialId) {
	}
}
