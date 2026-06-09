package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.syllabus.dto.CreateFullSyllabusRequest;
import com.fap.syllabus.dto.CreateSyllabusRequest;
import com.fap.syllabus.dto.FullSyllabusResponse;
import com.fap.syllabus.dto.QuickCreateSyllabusRequest;
import com.fap.syllabus.dto.SyllabusResponse;
import com.fap.syllabus.dto.UpdateSyllabusRequest;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusDay;
import com.fap.syllabus.entity.SyllabusOutputStandard;
import com.fap.syllabus.entity.SyllabusOutputStandardId;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.entity.SyllabusUnit;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.mapper.SyllabusMapper;
import com.fap.syllabus.mapper.SyllabusOutlineMapper;
import com.fap.syllabus.repository.SyllabusDayRepository;
import com.fap.syllabus.repository.SyllabusOutputStandardRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SyllabusService {

	private final SyllabusRepository syllabusRepository;
	private final SyllabusDayRepository dayRepository;
	private final SyllabusOutputStandardRepository outputStandardRepository;
	private final SyllabusMapper syllabusMapper;
	private final SyllabusOutlineMapper outlineMapper;
	private final AuditLogService auditLogService;

	public SyllabusService(
			SyllabusRepository syllabusRepository,
			SyllabusDayRepository dayRepository,
			SyllabusOutputStandardRepository outputStandardRepository,
			SyllabusMapper syllabusMapper,
			SyllabusOutlineMapper outlineMapper,
			AuditLogService auditLogService) {
		this.syllabusRepository = syllabusRepository;
		this.dayRepository = dayRepository;
		this.outputStandardRepository = outputStandardRepository;
		this.syllabusMapper = syllabusMapper;
		this.outlineMapper = outlineMapper;
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

	@Transactional
	public SyllabusResponse quickCreate(QuickCreateSyllabusRequest request, Long currentUserId) {
		if (syllabusRepository.existsByCodeIgnoreCase(request.code())) {
			throw new ConflictException("SYLLABUS_CODE_EXISTS", "Syllabus code already exists");
		}
		LocalDateTime now = LocalDateTime.now();
		Syllabus syllabus = new Syllabus();
		syllabus.setName(request.name().trim());
		syllabus.setCode(request.code().trim().toUpperCase());
		syllabus.setTechnicalRequirements(normalize(request.technicalRequirements()));
		syllabus.setStatus(SyllabusStatus.Drafting);
		syllabus.setCreatedAt(now);
		syllabus.setUpdatedAt(now);
		syllabus.setCreatedBy(currentUserId);
		syllabus.setUpdatedBy(currentUserId);
		Syllabus saved = syllabusRepository.save(syllabus);
		auditLogService.record("QUICK_CREATE_SYLLABUS", "syllabus", saved.getId());
		return syllabusMapper.toResponse(saved);
	}

	@Transactional
	public FullSyllabusResponse createFull(CreateFullSyllabusRequest request, Long currentUserId) {
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
		validateFullRequest(request);

		LocalDateTime now = LocalDateTime.now();
		Syllabus syllabus = new Syllabus();
		syllabus.setName(request.name().trim());
		syllabus.setCode(request.code().trim().toUpperCase());
		applyEditableFields(syllabus, request);
		syllabus.setStatus(SyllabusStatus.Drafting);
		syllabus.setCreatedAt(now);
		syllabus.setUpdatedAt(now);
		syllabus.setCreatedBy(currentUserId);
		syllabus.setUpdatedBy(currentUserId);
		Syllabus saved = syllabusRepository.save(syllabus);

		List<SyllabusOutputStandard> outputStandards = request.outputStandards().stream()
				.sorted(Comparator.naturalOrder())
				.map(standardCode -> createOutputStandard(saved, standardCode))
				.toList();
		outputStandardRepository.saveAll(outputStandards);

		List<SyllabusDay> days = request.days().stream()
				.map(dayRequest -> createDay(saved, dayRequest, currentUserId, now))
				.toList();
		dayRepository.saveAll(days);
		auditLogService.record("CREATE_FULL_SYLLABUS", "syllabus", saved.getId());
		return new FullSyllabusResponse(
				syllabusMapper.toResponse(saved),
				outputStandards.stream().map(SyllabusOutputStandard::getStandardCode).toList(),
				days.stream().map(outlineMapper::toFullResponse).toList());
	}

	@Transactional
	public FullSyllabusResponse updateFull(Long id, CreateFullSyllabusRequest request, Long currentUserId) {
		Syllabus syllabus = findSyllabus(id);
		ensureEditable(syllabus);
		if (syllabusRepository.existsByCodeIgnoreCaseAndIdNot(request.code(), id)) {
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
		validateFullRequest(request);

		syllabus.setName(request.name().trim());
		syllabus.setCode(request.code().trim().toUpperCase());
		applyEditableFields(syllabus, request);
		syllabus.setUpdatedAt(LocalDateTime.now());
		syllabus.setUpdatedBy(currentUserId);

		outputStandardRepository.deleteByIdSyllabusId(id);
		List<SyllabusOutputStandard> outputStandards = request.outputStandards().stream()
				.sorted(Comparator.naturalOrder())
				.map(standardCode -> createOutputStandard(syllabus, standardCode))
				.toList();
		outputStandardRepository.saveAll(outputStandards);

		dayRepository.deleteBySyllabusId(id);
		dayRepository.flush();
		LocalDateTime now = LocalDateTime.now();
		List<SyllabusDay> days = request.days().stream()
				.map(dayRequest -> createDay(syllabus, dayRequest, currentUserId, now))
				.toList();
		dayRepository.saveAll(days);
		auditLogService.record("UPDATE_FULL_SYLLABUS", "syllabus", syllabus.getId());
		return new FullSyllabusResponse(
				syllabusMapper.toResponse(syllabus),
				outputStandards.stream().map(SyllabusOutputStandard::getStandardCode).toList(),
				days.stream().map(outlineMapper::toFullResponse).toList());
	}

	@Transactional(readOnly = true)
	public SyllabusResponse get(Long id) {
		return syllabusMapper.toResponse(findSyllabus(id));
	}

	@Transactional(readOnly = true)
	public FullSyllabusResponse getFull(Long id) {
		Syllabus syllabus = findSyllabus(id);
		List<String> outputStandards = outputStandardRepository
				.findByIdSyllabusIdOrderByIdStandardCodeAsc(id)
				.stream()
				.map(SyllabusOutputStandard::getStandardCode)
				.toList();
		List<SyllabusDay> days = dayRepository.findBySyllabusIdOrderBySortOrderAsc(id);
		return new FullSyllabusResponse(
				syllabusMapper.toResponse(syllabus),
				outputStandards,
				days.stream().map(outlineMapper::toFullResponse).toList());
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

	private void applyEditableFields(Syllabus syllabus, CreateFullSyllabusRequest request) {
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

	private void validateFullRequest(CreateFullSyllabusRequest request) {
		Set<Integer> dayNumbers = new HashSet<>();
		Set<Integer> daySortOrders = new HashSet<>();
		Set<String> outputStandards = request.outputStandards();
		for (CreateFullSyllabusRequest.DayRequest day : request.days()) {
			if (!dayNumbers.add(day.dayNumber())) {
				throw new ConflictException("DUPLICATE_SYLLABUS_DAY_NUMBER", "Duplicate syllabus day number");
			}
			if (!daySortOrders.add(day.sortOrder())) {
				throw new ConflictException("DUPLICATE_SYLLABUS_DAY_SORT_ORDER", "Duplicate syllabus day sort order");
			}
			Set<Integer> unitSortOrders = new HashSet<>();
			for (CreateFullSyllabusRequest.UnitRequest unit : day.units()) {
				if (!unitSortOrders.add(unit.sortOrder())) {
					throw new ConflictException("DUPLICATE_SYLLABUS_UNIT_SORT_ORDER", "Duplicate syllabus unit sort order");
				}
				Set<Integer> topicSortOrders = new HashSet<>();
				for (CreateFullSyllabusRequest.TopicRequest topic : unit.topics()) {
					if (!topicSortOrders.add(topic.sortOrder())) {
						throw new ConflictException("DUPLICATE_SYLLABUS_TOPIC_SORT_ORDER", "Duplicate syllabus topic sort order");
					}
					if (!outputStandards.contains(topic.outputStandard())) {
						throw new ConflictException(
								"SYLLABUS_TOPIC_OUTPUT_STANDARD_INVALID",
								"Every topic output standard must be selected by the syllabus");
					}
				}
			}
		}
	}

	private SyllabusOutputStandard createOutputStandard(Syllabus syllabus, String standardCode) {
		SyllabusOutputStandard outputStandard = new SyllabusOutputStandard();
		outputStandard.setSyllabus(syllabus);
		outputStandard.setId(new SyllabusOutputStandardId(syllabus.getId(), standardCode));
		return outputStandard;
	}

	private SyllabusDay createDay(
			Syllabus syllabus,
			CreateFullSyllabusRequest.DayRequest request,
			Long currentUserId,
			LocalDateTime now) {
		SyllabusDay day = new SyllabusDay();
		day.setSyllabus(syllabus);
		day.setDayNumber(request.dayNumber());
		day.setSortOrder(request.sortOrder());
		day.setUnits(new ArrayList<>(request.units().stream()
				.map(unitRequest -> createUnit(day, unitRequest, currentUserId, now))
				.toList()));
		return day;
	}

	private SyllabusUnit createUnit(
			SyllabusDay day,
			CreateFullSyllabusRequest.UnitRequest request,
			Long currentUserId,
			LocalDateTime now) {
		SyllabusUnit unit = new SyllabusUnit();
		unit.setDay(day);
		unit.setName(request.name());
		unit.setSortOrder(request.sortOrder());
		unit.setTopics(new ArrayList<>(request.topics().stream()
				.map(topicRequest -> createTopic(unit, topicRequest, currentUserId, now))
				.toList()));
		return unit;
	}

	private SyllabusTopic createTopic(
			SyllabusUnit unit,
			CreateFullSyllabusRequest.TopicRequest request,
			Long currentUserId,
			LocalDateTime now) {
		SyllabusTopic topic = new SyllabusTopic();
		topic.setUnit(unit);
		topic.setName(request.name());
		topic.setOutputStandard(request.outputStandard());
		topic.setOnline(request.online());
		topic.setDurationMinutes(request.durationMinutes());
		topic.setStatus(request.status());
		topic.setSortOrder(request.sortOrder());
		if (request.materials() != null) {
			topic.setMaterials(new ArrayList<>(request.materials().stream()
					.map(materialRequest -> createMaterial(topic, materialRequest, currentUserId, now))
					.toList()));
		}
		return topic;
	}

	private MaterialFile createMaterial(
			SyllabusTopic topic,
			CreateFullSyllabusRequest.MaterialRequest request,
			Long currentUserId,
			LocalDateTime now) {
		MaterialFile material = new MaterialFile();
		material.setTopic(topic);
		material.setFileName(request.fileName());
		material.setFileUrl(request.fileUrl());
		material.setFileSize(request.fileSize());
		material.setContentType(request.contentType());
		material.setUploadedBy(currentUserId);
		material.setUploadedAt(now);
		return material;
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
