package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.syllabus.dto.CreateSyllabusDayRequest;
import com.fap.syllabus.dto.CreateSyllabusTopicRequest;
import com.fap.syllabus.dto.CreateSyllabusUnitRequest;
import com.fap.syllabus.dto.SyllabusDayResponse;
import com.fap.syllabus.dto.SyllabusTopicResponse;
import com.fap.syllabus.dto.SyllabusUnitResponse;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusDay;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.entity.SyllabusUnit;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.mapper.SyllabusOutlineMapper;
import com.fap.syllabus.repository.SyllabusDayRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import com.fap.syllabus.repository.SyllabusTopicRepository;
import com.fap.syllabus.repository.SyllabusUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SyllabusOutlineService {

	private final SyllabusRepository syllabusRepository;
	private final SyllabusDayRepository dayRepository;
	private final SyllabusUnitRepository unitRepository;
	private final SyllabusTopicRepository topicRepository;
	private final SyllabusOutlineMapper outlineMapper;
	private final AuditLogService auditLogService;

	public SyllabusOutlineService(
			SyllabusRepository syllabusRepository,
			SyllabusDayRepository dayRepository,
			SyllabusUnitRepository unitRepository,
			SyllabusTopicRepository topicRepository,
			SyllabusOutlineMapper outlineMapper,
			AuditLogService auditLogService) {
		this.syllabusRepository = syllabusRepository;
		this.dayRepository = dayRepository;
		this.unitRepository = unitRepository;
		this.topicRepository = topicRepository;
		this.outlineMapper = outlineMapper;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public List<SyllabusDayResponse> getOutline(Long syllabusId) {
		ensureSyllabusExists(syllabusId);
		return dayRepository.findBySyllabusIdOrderBySortOrderAsc(syllabusId).stream()
				.map(outlineMapper::toResponse)
				.toList();
	}

	@Transactional
	public SyllabusDayResponse createDay(Long syllabusId, CreateSyllabusDayRequest request) {
		Syllabus syllabus = findEditableSyllabus(syllabusId);
		SyllabusDay day = new SyllabusDay();
		day.setSyllabus(syllabus);
		day.setDayNumber(request.dayNumber());
		day.setSortOrder(request.sortOrder());
		SyllabusDay saved = dayRepository.save(day);
		auditLogService.record("CREATE_SYLLABUS_DAY", "syllabus", syllabusId);
		return outlineMapper.toResponse(saved);
	}

	@Transactional
	public SyllabusDayResponse updateDay(Long syllabusId, Long dayId, CreateSyllabusDayRequest request) {
		findEditableSyllabus(syllabusId);
		SyllabusDay day = findDay(syllabusId, dayId);
		day.setDayNumber(request.dayNumber());
		day.setSortOrder(request.sortOrder());
		auditLogService.record("UPDATE_SYLLABUS_DAY", "syllabus", syllabusId);
		return outlineMapper.toResponse(day);
	}

	@Transactional
	public void deleteDay(Long syllabusId, Long dayId) {
		findEditableSyllabus(syllabusId);
		SyllabusDay day = findDay(syllabusId, dayId);
		dayRepository.delete(day);
		auditLogService.record("DELETE_SYLLABUS_DAY", "syllabus", syllabusId);
	}

	@Transactional
	public SyllabusUnitResponse createUnit(Long syllabusId, Long dayId, CreateSyllabusUnitRequest request) {
		findEditableSyllabus(syllabusId);
		SyllabusDay day = findDay(syllabusId, dayId);
		SyllabusUnit unit = new SyllabusUnit();
		unit.setDay(day);
		unit.setName(request.name());
		unit.setSortOrder(request.sortOrder());
		SyllabusUnit saved = unitRepository.save(unit);
		auditLogService.record("CREATE_SYLLABUS_UNIT", "syllabus", syllabusId);
		return outlineMapper.toResponse(saved);
	}

	@Transactional
	public SyllabusUnitResponse updateUnit(Long syllabusId, Long unitId, CreateSyllabusUnitRequest request) {
		findEditableSyllabus(syllabusId);
		SyllabusUnit unit = findUnit(syllabusId, unitId);
		unit.setName(request.name());
		unit.setSortOrder(request.sortOrder());
		auditLogService.record("UPDATE_SYLLABUS_UNIT", "syllabus", syllabusId);
		return outlineMapper.toResponse(unit);
	}

	@Transactional
	public void deleteUnit(Long syllabusId, Long unitId) {
		findEditableSyllabus(syllabusId);
		SyllabusUnit unit = findUnit(syllabusId, unitId);
		unitRepository.delete(unit);
		auditLogService.record("DELETE_SYLLABUS_UNIT", "syllabus", syllabusId);
	}

	@Transactional
	public SyllabusTopicResponse createTopic(Long syllabusId, Long unitId, CreateSyllabusTopicRequest request) {
		findEditableSyllabus(syllabusId);
		SyllabusUnit unit = findUnit(syllabusId, unitId);
		SyllabusTopic topic = new SyllabusTopic();
		topic.setUnit(unit);
		applyTopic(topic, request);
		SyllabusTopic saved = topicRepository.save(topic);
		auditLogService.record("CREATE_SYLLABUS_TOPIC", "syllabus", syllabusId);
		return outlineMapper.toResponse(saved);
	}

	@Transactional
	public SyllabusTopicResponse updateTopic(Long syllabusId, Long topicId, CreateSyllabusTopicRequest request) {
		findEditableSyllabus(syllabusId);
		SyllabusTopic topic = findTopic(syllabusId, topicId);
		applyTopic(topic, request);
		auditLogService.record("UPDATE_SYLLABUS_TOPIC", "syllabus", syllabusId);
		return outlineMapper.toResponse(topic);
	}

	@Transactional
	public void deleteTopic(Long syllabusId, Long topicId) {
		findEditableSyllabus(syllabusId);
		SyllabusTopic topic = findTopic(syllabusId, topicId);
		topicRepository.delete(topic);
		auditLogService.record("DELETE_SYLLABUS_TOPIC", "syllabus", syllabusId);
	}

	private void applyTopic(SyllabusTopic topic, CreateSyllabusTopicRequest request) {
		topic.setName(request.name());
		topic.setOutputStandard(request.outputStandard());
		topic.setOnline(request.online());
		topic.setDurationMinutes(request.durationMinutes());
		topic.setStatus(request.status());
		topic.setSortOrder(request.sortOrder());
	}

	private void ensureSyllabusExists(Long syllabusId) {
		if (!syllabusRepository.existsById(syllabusId)) {
			throw new NotFoundException("Syllabus not found");
		}
	}

	private Syllabus findEditableSyllabus(Long syllabusId) {
		Syllabus syllabus = syllabusRepository.findById(syllabusId)
				.orElseThrow(() -> new NotFoundException("Syllabus not found"));
		if (syllabus.getStatus() == SyllabusStatus.Active || syllabus.getStatus() == SyllabusStatus.Inactive) {
			throw new ConflictException("SYLLABUS_NOT_EDITABLE", "Only Drafting or Pending syllabus can be edited");
		}
		return syllabus;
	}

	private SyllabusDay findDay(Long syllabusId, Long dayId) {
		return dayRepository.findByIdAndSyllabusId(dayId, syllabusId)
				.orElseThrow(() -> new NotFoundException("Syllabus day not found"));
	}

	private SyllabusUnit findUnit(Long syllabusId, Long unitId) {
		return unitRepository.findByIdAndDaySyllabusId(unitId, syllabusId)
				.orElseThrow(() -> new NotFoundException("Syllabus unit not found"));
	}

	private SyllabusTopic findTopic(Long syllabusId, Long topicId) {
		return topicRepository.findByIdAndUnitDaySyllabusId(topicId, syllabusId)
				.orElseThrow(() -> new NotFoundException("Syllabus topic not found"));
	}
}
