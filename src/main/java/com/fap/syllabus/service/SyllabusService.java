package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.api.PageRequestFactory;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.syllabus.dto.CreateFullSyllabusRequest;
import com.fap.syllabus.dto.CloneSyllabusRequest;
import com.fap.syllabus.dto.CreateSyllabusRequest;
import com.fap.syllabus.dto.FullSyllabusResponse;
import com.fap.syllabus.dto.QuickCreateSyllabusRequest;
import com.fap.syllabus.dto.SyllabusResponse;
import com.fap.syllabus.dto.UpdateSyllabusRequest;
import com.fap.syllabus.entity.MaterialFile;
import com.fap.syllabus.entity.MaterialFileContent;
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
import com.fap.syllabus.repository.MaterialFileContentRepository;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SyllabusService {
	private static final String UNAVAILABLE_FILE_URL = "unavailable";

	private final SyllabusRepository syllabusRepository;
	private final SyllabusDayRepository dayRepository;
	private final MaterialFileContentRepository materialFileContentRepository;
	private final SyllabusOutputStandardRepository outputStandardRepository;
	private final SyllabusMapper syllabusMapper;
	private final SyllabusOutlineMapper outlineMapper;
	private final AuditLogService auditLogService;

	public SyllabusService(
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

	@Transactional(readOnly = true)
	public Page<SyllabusResponse> list(SyllabusStatus status, String levelName, String keyword, int page, int limit) {
		return list(status, levelName, keyword, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<SyllabusResponse> list(
			SyllabusStatus status,
			String levelName,
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
				Sort.by(Sort.Direction.DESC, "createdAt"),
				"id", "createdAt", "name", "code", "version", "status", "levelName");
		return syllabusRepository.search(status, normalizeLevelName(levelName), normalize(keyword), pageRequest)
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
	public FullSyllabusResponse cloneVersion(Long sourceId, CloneSyllabusRequest request, Long currentUserId) {
		Syllabus source = findSyllabus(sourceId);
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
				.map(item -> createOutputStandard(saved, item.getStandardCode()))
				.toList();
		outputStandardRepository.saveAll(outputStandards);

		List<SyllabusDay> sourceDays = dayRepository.findBySyllabusIdOrderBySortOrderAsc(sourceId);
		Set<Long> materialIds = sourceDays.stream()
				.flatMap(day -> day.getUnits().stream())
				.flatMap(unit -> unit.getTopics().stream())
				.flatMap(topic -> topic.getMaterials().stream())
				.map(MaterialFile::getId)
				.collect(Collectors.toSet());
		Map<Long, byte[]> contentByMaterialId = new HashMap<>();
		materialFileContentRepository.findAllById(materialIds).forEach(content ->
				contentByMaterialId.put(
						content.getMaterialFileId(),
						Arrays.copyOf(content.getFileData(), content.getFileData().length)));

		List<MaterialContentRestore> contentRestores = new ArrayList<>();
		List<SyllabusDay> clonedDays = sourceDays.stream()
				.map(day -> cloneDay(saved, day, contentByMaterialId, contentRestores))
				.toList();
		dayRepository.saveAll(clonedDays);
		dayRepository.flush();
		restoreMaterialContents(contentRestores);

		auditLogService.record("CLONE_SYLLABUS_VERSION:" + sourceId, "syllabus", saved.getId());
		return new FullSyllabusResponse(
				syllabusMapper.toResponse(saved),
				outputStandards.stream().map(SyllabusOutputStandard::getStandardCode).toList(),
				clonedDays.stream().map(outlineMapper::toFullResponse).toList());
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
		Map<Long, ExistingMaterialSnapshot> existingMaterials = captureExistingMaterials(id, request);
		List<MaterialContentRestore> contentRestores = new ArrayList<>();

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
				.map(dayRequest -> createDay(
						syllabus, dayRequest, currentUserId, now, existingMaterials, contentRestores))
				.toList();
		dayRepository.saveAll(days);
		dayRepository.flush();
		restoreMaterialContents(contentRestores);
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
		return createDay(syllabus, request, currentUserId, now, Map.of(), new ArrayList<>());
	}

	private SyllabusDay createDay(
			Syllabus syllabus,
			CreateFullSyllabusRequest.DayRequest request,
			Long currentUserId,
			LocalDateTime now,
			Map<Long, ExistingMaterialSnapshot> existingMaterials,
			List<MaterialContentRestore> contentRestores) {
		SyllabusDay day = new SyllabusDay();
		day.setSyllabus(syllabus);
		day.setDayNumber(request.dayNumber());
		day.setSortOrder(request.sortOrder());
		day.setUnits(new ArrayList<>(request.units().stream()
				.map(unitRequest -> createUnit(
						day, unitRequest, currentUserId, now, existingMaterials, contentRestores))
				.toList()));
		return day;
	}

	private SyllabusUnit createUnit(
			SyllabusDay day,
			CreateFullSyllabusRequest.UnitRequest request,
			Long currentUserId,
			LocalDateTime now,
			Map<Long, ExistingMaterialSnapshot> existingMaterials,
			List<MaterialContentRestore> contentRestores) {
		SyllabusUnit unit = new SyllabusUnit();
		unit.setDay(day);
		unit.setName(request.name());
		unit.setSortOrder(request.sortOrder());
		unit.setTopics(new ArrayList<>(request.topics().stream()
				.map(topicRequest -> createTopic(
						unit, topicRequest, currentUserId, now, existingMaterials, contentRestores))
				.toList()));
		return unit;
	}

	private SyllabusTopic createTopic(
			SyllabusUnit unit,
			CreateFullSyllabusRequest.TopicRequest request,
			Long currentUserId,
			LocalDateTime now,
			Map<Long, ExistingMaterialSnapshot> existingMaterials,
			List<MaterialContentRestore> contentRestores) {
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
					.map(materialRequest -> createMaterial(
							topic, materialRequest, currentUserId, now, existingMaterials, contentRestores))
					.toList()));
		}
		return topic;
	}

	private MaterialFile createMaterial(
			SyllabusTopic topic,
			CreateFullSyllabusRequest.MaterialRequest request,
			Long currentUserId,
			LocalDateTime now,
			Map<Long, ExistingMaterialSnapshot> existingMaterials,
			List<MaterialContentRestore> contentRestores) {
		MaterialFile material = new MaterialFile();
		material.setTopic(topic);
		ExistingMaterialSnapshot existing = existingMaterials.get(request.id());
		if (existing != null && existing.fileData() != null) {
			material.setFileName(existing.fileName());
			material.setFileUrl("pending");
			material.setFileSize(existing.fileSize());
			material.setContentType(existing.contentType());
			material.setUploadedBy(existing.uploadedBy());
			material.setUploadedAt(existing.uploadedAt());
			contentRestores.add(new MaterialContentRestore(material, existing.fileData()));
		} else {
			material.setFileName(request.fileName());
			material.setFileUrl(request.fileUrl());
			material.setFileSize(request.fileSize());
			material.setContentType(request.contentType());
			material.setUploadedBy(currentUserId);
			material.setUploadedAt(now);
		}
		return material;
	}

	private SyllabusDay cloneDay(
			Syllabus syllabus,
			SyllabusDay source,
			Map<Long, byte[]> contentByMaterialId,
			List<MaterialContentRestore> contentRestores) {
		SyllabusDay cloned = new SyllabusDay();
		cloned.setSyllabus(syllabus);
		cloned.setDayNumber(source.getDayNumber());
		cloned.setSortOrder(source.getSortOrder());
		cloned.setUnits(new ArrayList<>(source.getUnits().stream()
				.map(unit -> cloneUnit(cloned, unit, contentByMaterialId, contentRestores))
				.toList()));
		return cloned;
	}

	private SyllabusUnit cloneUnit(
			SyllabusDay day,
			SyllabusUnit source,
			Map<Long, byte[]> contentByMaterialId,
			List<MaterialContentRestore> contentRestores) {
		SyllabusUnit cloned = new SyllabusUnit();
		cloned.setDay(day);
		cloned.setName(source.getName());
		cloned.setSortOrder(source.getSortOrder());
		cloned.setTopics(new ArrayList<>(source.getTopics().stream()
				.map(topic -> cloneTopic(cloned, topic, contentByMaterialId, contentRestores))
				.toList()));
		return cloned;
	}

	private SyllabusTopic cloneTopic(
			SyllabusUnit unit,
			SyllabusTopic source,
			Map<Long, byte[]> contentByMaterialId,
			List<MaterialContentRestore> contentRestores) {
		SyllabusTopic cloned = new SyllabusTopic();
		cloned.setUnit(unit);
		cloned.setName(source.getName());
		cloned.setOutputStandard(source.getOutputStandard());
		cloned.setOnline(source.isOnline());
		cloned.setDurationMinutes(source.getDurationMinutes());
		cloned.setStatus(source.getStatus());
		cloned.setSortOrder(source.getSortOrder());
		cloned.setMaterials(new ArrayList<>(source.getMaterials().stream()
				.map(material -> cloneMaterial(cloned, material, contentByMaterialId, contentRestores))
				.toList()));
		return cloned;
	}

	private MaterialFile cloneMaterial(
			SyllabusTopic topic,
			MaterialFile source,
			Map<Long, byte[]> contentByMaterialId,
			List<MaterialContentRestore> contentRestores) {
		MaterialFile cloned = new MaterialFile();
		cloned.setTopic(topic);
		cloned.setFileName(source.getFileName());
		cloned.setFileSize(source.getFileSize());
		cloned.setContentType(source.getContentType());
		cloned.setUploadedBy(source.getUploadedBy());
		cloned.setUploadedAt(source.getUploadedAt());

		byte[] fileData = contentByMaterialId.get(source.getId());
		if (fileData != null) {
			cloned.setFileUrl("pending");
			contentRestores.add(new MaterialContentRestore(cloned, fileData));
		} else if (isInternalDownloadPath(source.getFileUrl())) {
			cloned.setFileUrl(UNAVAILABLE_FILE_URL);
		} else {
			cloned.setFileUrl(source.getFileUrl());
		}
		return cloned;
	}

	private Map<Long, ExistingMaterialSnapshot> captureExistingMaterials(
			Long syllabusId,
			CreateFullSyllabusRequest request) {
		Map<Long, MaterialFile> existingById = dayRepository.findBySyllabusIdOrderBySortOrderAsc(syllabusId)
				.stream()
				.flatMap(day -> day.getUnits().stream())
				.flatMap(unit -> unit.getTopics().stream())
				.flatMap(topic -> topic.getMaterials().stream())
				.collect(Collectors.toMap(MaterialFile::getId, Function.identity()));
		List<CreateFullSyllabusRequest.MaterialRequest> requestedMaterials = request.days().stream()
				.flatMap(day -> day.units().stream())
				.flatMap(unit -> unit.topics().stream())
				.flatMap(topic -> topic.materials() == null ? java.util.stream.Stream.empty() : topic.materials().stream())
				.toList();
		for (CreateFullSyllabusRequest.MaterialRequest material : requestedMaterials) {
			if (material.id() == null && isInternalDownloadPath(material.fileUrl())) {
				throw new BadRequestException(
						"INVALID_MATERIAL_REFERENCE",
						"Internal material URL requires a valid material id");
			}
		}
		Set<Long> requestedIds = requestedMaterials.stream()
				.map(CreateFullSyllabusRequest.MaterialRequest::id)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toSet());

		for (Long materialId : requestedIds) {
			if (!existingById.containsKey(materialId)) {
				throw new BadRequestException(
						"INVALID_MATERIAL_REFERENCE",
						"Material does not belong to this syllabus");
			}
		}

		Map<Long, byte[]> contentById = new HashMap<>();
		materialFileContentRepository.findAllById(requestedIds).forEach(content ->
				contentById.put(content.getMaterialFileId(), Arrays.copyOf(
						content.getFileData(), content.getFileData().length)));

		Map<Long, ExistingMaterialSnapshot> snapshots = new HashMap<>();
		for (Long materialId : requestedIds) {
			MaterialFile material = existingById.get(materialId);
			byte[] fileData = contentById.get(materialId);
			CreateFullSyllabusRequest.MaterialRequest requestedMaterial = requestedMaterials.stream()
					.filter(item -> materialId.equals(item.id()))
					.findFirst()
					.orElseThrow();
			if ((isInternalDownloadPath(material.getFileUrl())
					|| isInternalDownloadPath(requestedMaterial.fileUrl())) && fileData == null) {
				throw new ConflictException(
						"MATERIAL_CONTENT_MISSING",
						"Material file content is missing; upload the file again");
			}
			snapshots.put(materialId, new ExistingMaterialSnapshot(
					material.getFileName(),
					material.getFileSize(),
					material.getContentType(),
					material.getUploadedBy(),
					material.getUploadedAt(),
					fileData));
		}
		return snapshots;
	}

	private void restoreMaterialContents(List<MaterialContentRestore> contentRestores) {
		for (MaterialContentRestore restore : contentRestores) {
			MaterialFile material = restore.material();
			material.setFileUrl(MaterialFileService.downloadPath(material.getId()));
			MaterialFileContent content = new MaterialFileContent();
			content.setMaterialFile(material);
			content.setFileData(restore.fileData());
			materialFileContentRepository.save(content);
		}
	}

	private boolean isInternalDownloadPath(String fileUrl) {
		return fileUrl != null
				&& fileUrl.startsWith("/api/v1/materials/")
				&& fileUrl.endsWith("/download");
	}

	private record ExistingMaterialSnapshot(
			String fileName,
			Long fileSize,
			String contentType,
			Long uploadedBy,
			LocalDateTime uploadedAt,
			byte[] fileData) {
	}

	private record MaterialContentRestore(MaterialFile material, byte[] fileData) {
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

	private String normalizeLevelName(String levelName) {
		String normalized = normalize(levelName);
		if (normalized == null
				|| normalized.equalsIgnoreCase("all")
				|| normalized.equalsIgnoreCase("all levels")) {
			return null;
		}
		return normalized.toLowerCase();
	}
}
