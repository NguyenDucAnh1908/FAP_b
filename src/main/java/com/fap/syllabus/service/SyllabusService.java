package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.syllabus.dto.CreateSyllabusRequest;
import com.fap.syllabus.dto.SyllabusResponse;
import com.fap.syllabus.dto.UpdateSyllabusRequest;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.mapper.SyllabusMapper;
import com.fap.syllabus.repository.SyllabusRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SyllabusService {

	private final SyllabusRepository syllabusRepository;
	private final SyllabusMapper syllabusMapper;
	private final AuditLogService auditLogService;

	public SyllabusService(
			SyllabusRepository syllabusRepository,
			SyllabusMapper syllabusMapper,
			AuditLogService auditLogService) {
		this.syllabusRepository = syllabusRepository;
		this.syllabusMapper = syllabusMapper;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public Page<SyllabusResponse> list(SyllabusStatus status, String levelName, String keyword, int page, int limit) {
		PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
		return syllabusRepository.search(status, normalize(levelName), normalize(keyword), pageRequest)
				.map(syllabusMapper::toResponse);
	}

	@Transactional
	public SyllabusResponse create(CreateSyllabusRequest request, Long currentUserId) {
		if (syllabusRepository.existsByCodeIgnoreCase(request.code())) {
			throw new ConflictException("SYLLABUS_CODE_EXISTS", "Syllabus code already exists");
		}
		validatePercentTotals(
				request.timeAllocAssignmentLab(),
				request.timeAllocConceptLecture(),
				request.timeAllocGuideReview(),
				request.timeAllocTestQuiz(),
				request.assessQuizPct(),
				request.assessAssignmentPct(),
				request.assessFinalPct());
		LocalDateTime now = LocalDateTime.now();
		Syllabus syllabus = new Syllabus();
		syllabus.setName(request.name());
		syllabus.setCode(request.code().trim().toUpperCase());
		applyEditableFields(syllabus, request);
		syllabus.setStatus(SyllabusStatus.Drafting);
		syllabus.setCreatedAt(now);
		syllabus.setUpdatedAt(now);
		syllabus.setCreatedBy(currentUserId);
		syllabus.setUpdatedBy(currentUserId);
		Syllabus saved = syllabusRepository.save(syllabus);
		auditLogService.record("CREATE_SYLLABUS", "syllabus", saved.getId());
		return syllabusMapper.toResponse(saved);
	}

	@Transactional(readOnly = true)
	public SyllabusResponse get(Long id) {
		return syllabusMapper.toResponse(findSyllabus(id));
	}

	@Transactional
	public SyllabusResponse update(Long id, UpdateSyllabusRequest request, Long currentUserId) {
		Syllabus syllabus = findSyllabus(id);
		ensureEditable(syllabus);
		validatePercentTotals(
				request.timeAllocAssignmentLab(),
				request.timeAllocConceptLecture(),
				request.timeAllocGuideReview(),
				request.timeAllocTestQuiz(),
				request.assessQuizPct(),
				request.assessAssignmentPct(),
				request.assessFinalPct());
		applyEditableFields(syllabus, request);
		syllabus.setUpdatedAt(LocalDateTime.now());
		syllabus.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_SYLLABUS", "syllabus", syllabus.getId());
		return syllabusMapper.toResponse(syllabus);
	}

	@Transactional
	public SyllabusResponse updateStatus(Long id, SyllabusStatus status, Long currentUserId) {
		Syllabus syllabus = findSyllabus(id);
		validateTransition(syllabus.getStatus(), status);
		validateReadyForSubmission(syllabus, status);
		syllabus.setStatus(status);
		syllabus.setUpdatedAt(LocalDateTime.now());
		syllabus.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_SYLLABUS_STATUS:" + status.name(), "syllabus", syllabus.getId());
		return syllabusMapper.toResponse(syllabus);
	}

	@Transactional
	public void delete(Long id, Long currentUserId) {
		Syllabus syllabus = findSyllabus(id);
		ensureEditable(syllabus);
		syllabus.setDeleted(true);
		syllabus.setDeletedAt(LocalDateTime.now());
		syllabus.setUpdatedAt(LocalDateTime.now());
		syllabus.setUpdatedBy(currentUserId);
		auditLogService.record("DELETE_SYLLABUS", "syllabus", syllabus.getId());
	}

	private Syllabus findSyllabus(Long id) {
		return syllabusRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Syllabus not found"));
	}

	private void applyEditableFields(Syllabus syllabus, CreateSyllabusRequest request) {
		syllabus.setVersion(request.version());
		syllabus.setLevelName(request.levelName());
		syllabus.setAttendees(request.attendees());
		syllabus.setDuration(request.duration());
		syllabus.setTechnicalRequirements(request.technicalRequirements());
		syllabus.setCourseObjectives(request.courseObjectives());
		syllabus.setRules(request.rules());
		syllabus.setTimeAllocAssignmentLab(request.timeAllocAssignmentLab());
		syllabus.setTimeAllocConceptLecture(request.timeAllocConceptLecture());
		syllabus.setTimeAllocGuideReview(request.timeAllocGuideReview());
		syllabus.setTimeAllocTestQuiz(request.timeAllocTestQuiz());
		syllabus.setAssessQuizPct(request.assessQuizPct());
		syllabus.setAssessAssignmentPct(request.assessAssignmentPct());
		syllabus.setAssessFinalPct(request.assessFinalPct());
		syllabus.setAssessmentText(request.assessmentText());
	}

	private void applyEditableFields(Syllabus syllabus, UpdateSyllabusRequest request) {
		syllabus.setName(request.name());
		syllabus.setVersion(request.version());
		syllabus.setLevelName(request.levelName());
		syllabus.setAttendees(request.attendees());
		syllabus.setDuration(request.duration());
		syllabus.setTechnicalRequirements(request.technicalRequirements());
		syllabus.setCourseObjectives(request.courseObjectives());
		syllabus.setRules(request.rules());
		syllabus.setTimeAllocAssignmentLab(request.timeAllocAssignmentLab());
		syllabus.setTimeAllocConceptLecture(request.timeAllocConceptLecture());
		syllabus.setTimeAllocGuideReview(request.timeAllocGuideReview());
		syllabus.setTimeAllocTestQuiz(request.timeAllocTestQuiz());
		syllabus.setAssessQuizPct(request.assessQuizPct());
		syllabus.setAssessAssignmentPct(request.assessAssignmentPct());
		syllabus.setAssessFinalPct(request.assessFinalPct());
		syllabus.setAssessmentText(request.assessmentText());
	}

	private void ensureEditable(Syllabus syllabus) {
		if (syllabus.getStatus() == SyllabusStatus.Active || syllabus.getStatus() == SyllabusStatus.Inactive) {
			throw new ConflictException("SYLLABUS_NOT_EDITABLE", "Only Drafting or Pending syllabus can be edited");
		}
	}

	private void validateTransition(SyllabusStatus current, SyllabusStatus target) {
		if (current == target) {
			return;
		}
		boolean allowed = (current == SyllabusStatus.Drafting && target == SyllabusStatus.Pending)
				|| (current == SyllabusStatus.Pending && target == SyllabusStatus.Active)
				|| (current == SyllabusStatus.Drafting && target == SyllabusStatus.Inactive)
				|| (current == SyllabusStatus.Pending && target == SyllabusStatus.Inactive);
		if (!allowed) {
			throw new ConflictException("INVALID_SYLLABUS_STATUS_TRANSITION", "Invalid syllabus status transition");
		}
	}

	private void validateReadyForSubmission(Syllabus syllabus, SyllabusStatus target) {
		if (target != SyllabusStatus.Pending && target != SyllabusStatus.Active) {
			return;
		}
		if (syllabusRepository.countTopicsBySyllabusId(syllabus.getId()) == 0) {
			throw new ConflictException("SYLLABUS_OUTLINE_REQUIRED", "Syllabus outline is required before submit or publish");
		}
		if (syllabusRepository.countOutputStandardsBySyllabusId(syllabus.getId()) == 0) {
			throw new ConflictException("SYLLABUS_OUTPUT_STANDARDS_REQUIRED", "Syllabus output standards are required before submit or publish");
		}
		if (syllabusRepository.countTopicsWithoutSelectedOutputStandard(syllabus.getId()) > 0) {
			throw new ConflictException("SYLLABUS_TOPIC_OUTPUT_STANDARD_INVALID", "Every topic output standard must be selected by the syllabus");
		}
	}

	private void validatePercentTotals(
			Integer assignmentLab,
			Integer conceptLecture,
			Integer guideReview,
			Integer testQuiz,
			Integer quiz,
			Integer assignment,
			Integer finalAssessment) {
		if (assignmentLab + conceptLecture + guideReview + testQuiz != 100) {
			throw new BadRequestException("INVALID_SYLLABUS_TIME_ALLOCATION", "Syllabus time allocation total must be 100");
		}
		if (quiz + assignment + finalAssessment != 100) {
			throw new BadRequestException("INVALID_SYLLABUS_ASSESSMENT", "Syllabus assessment total must be 100");
		}
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
