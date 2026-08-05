package com.fap.program.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.api.PageRequestFactory;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.program.dto.CreateTrainingProgramRequest;
import com.fap.program.dto.TrainingProgramResponse;
import com.fap.program.dto.TrainingProgramSyllabusItemRequest;
import com.fap.program.dto.TrainingProgramSyllabusResponse;
import com.fap.program.dto.UpdateTrainingProgramRequest;
import com.fap.program.dto.UpdateTrainingProgramSyllabusesRequest;
import com.fap.program.entity.TrainingProgram;
import com.fap.program.entity.TrainingProgramSyllabus;
import com.fap.program.entity.TrainingProgramSyllabusId;
import com.fap.program.enums.TrainingProgramStatus;
import com.fap.program.mapper.TrainingProgramMapper;
import com.fap.program.repository.TrainingProgramRepository;
import com.fap.program.repository.TrainingProgramSyllabusRepository;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.repository.SyllabusRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TrainingProgramService {

	private final TrainingProgramRepository programRepository;
	private final TrainingProgramSyllabusRepository programSyllabusRepository;
	private final SyllabusRepository syllabusRepository;
	private final TrainingProgramMapper trainingProgramMapper;
	private final AuditLogService auditLogService;

	public TrainingProgramService(
			TrainingProgramRepository programRepository,
			TrainingProgramSyllabusRepository programSyllabusRepository,
			SyllabusRepository syllabusRepository,
			TrainingProgramMapper trainingProgramMapper,
			AuditLogService auditLogService) {
		this.programRepository = programRepository;
		this.programSyllabusRepository = programSyllabusRepository;
		this.syllabusRepository = syllabusRepository;
		this.trainingProgramMapper = trainingProgramMapper;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public Page<TrainingProgramResponse> list(TrainingProgramStatus status, String keyword, int page, int limit) {
		return list(status, keyword, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<TrainingProgramResponse> list(
			TrainingProgramStatus status,
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
				"id", "createdAt", "name", "duration", "totalHours", "version", "status");
		return programRepository.search(status, normalize(keyword), pageRequest)
				.map(trainingProgramMapper::toResponse);
	}

	@Transactional
	public TrainingProgramResponse create(CreateTrainingProgramRequest request, Long currentUserId) {
		LocalDateTime now = LocalDateTime.now();
		TrainingProgram program = new TrainingProgram();
		program.setName(request.name());
		program.setDuration(request.duration());
		program.setTotalHours(request.totalHours());
		program.setVersion(request.version());
		program.setStatus(TrainingProgramStatus.Planning);
		program.setCreatedAt(now);
		program.setUpdatedAt(now);
		program.setCreatedBy(currentUserId);
		program.setUpdatedBy(currentUserId);
		TrainingProgram saved = programRepository.save(program);
		auditLogService.record("CREATE_TRAINING_PROGRAM", "training_program", saved.getId());
		return trainingProgramMapper.toResponse(saved);
	}

	@Transactional(readOnly = true)
	public TrainingProgramResponse get(Long id) {
		return trainingProgramMapper.toResponse(findProgram(id));
	}

	@Transactional
	public TrainingProgramResponse update(Long id, UpdateTrainingProgramRequest request, Long currentUserId) {
		TrainingProgram program = findProgram(id);
		ensurePlanning(program);
		program.setName(request.name());
		program.setDuration(request.duration());
		program.setTotalHours(request.totalHours());
		program.setVersion(request.version());
		program.setUpdatedAt(LocalDateTime.now());
		program.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_TRAINING_PROGRAM", "training_program", program.getId());
		return trainingProgramMapper.toResponse(program);
	}

	@Transactional
	public TrainingProgramResponse updateStatus(Long id, TrainingProgramStatus status, Long currentUserId) {
		TrainingProgram program = findProgram(id);
		validateTransition(program, status);
		program.setStatus(status);
		program.setUpdatedAt(LocalDateTime.now());
		program.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_TRAINING_PROGRAM_STATUS:" + status.name(), "training_program", program.getId());
		return trainingProgramMapper.toResponse(program);
	}

	@Transactional
	public void delete(Long id, Long currentUserId) {
		TrainingProgram program = findProgram(id);
		ensurePlanning(program);
		program.setDeleted(true);
		program.setDeletedAt(LocalDateTime.now());
		program.setUpdatedAt(LocalDateTime.now());
		program.setUpdatedBy(currentUserId);
		auditLogService.record("DELETE_TRAINING_PROGRAM", "training_program", program.getId());
	}

	@Transactional(readOnly = true)
	public List<TrainingProgramSyllabusResponse> listSyllabuses(Long id) {
		ensureProgramExists(id);
		return programSyllabusRepository.findByIdProgramIdOrderBySortOrderAsc(id).stream()
				.map(trainingProgramMapper::toResponse)
				.toList();
	}

	@Transactional
	public List<TrainingProgramSyllabusResponse> replaceSyllabuses(
			Long id,
			UpdateTrainingProgramSyllabusesRequest request,
			Long currentUserId) {
		TrainingProgram program = findProgram(id);
		ensurePlanning(program);
		validateSyllabusItems(request.syllabuses());
		programSyllabusRepository.deleteByIdProgramId(id);
		List<TrainingProgramSyllabus> items = request.syllabuses().stream()
				.map(item -> createProgramSyllabus(program, item))
				.toList();
		programSyllabusRepository.saveAll(items);
		program.setUpdatedAt(LocalDateTime.now());
		program.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_TRAINING_PROGRAM_SYLLABUSES", "training_program", program.getId());
		return items.stream()
				.sorted((left, right) -> left.getSortOrder().compareTo(right.getSortOrder()))
				.map(trainingProgramMapper::toResponse)
				.toList();
	}

	private TrainingProgramSyllabus createProgramSyllabus(
			TrainingProgram program,
			TrainingProgramSyllabusItemRequest item) {
		Syllabus syllabus = syllabusRepository.findById(item.syllabusId())
				.orElseThrow(() -> new NotFoundException("Syllabus not found"));
		if (syllabus.getStatus() != SyllabusStatus.Active) {
			throw new ConflictException("TRAINING_PROGRAM_SYLLABUS_NOT_ACTIVE", "Only active syllabuses can be attached");
		}
		TrainingProgramSyllabus programSyllabus = new TrainingProgramSyllabus();
		programSyllabus.setProgram(program);
		programSyllabus.setSyllabus(syllabus);
		programSyllabus.setId(new TrainingProgramSyllabusId(program.getId(), syllabus.getId()));
		programSyllabus.setSortOrder(item.sortOrder());
		return programSyllabus;
	}

	private void validateSyllabusItems(List<TrainingProgramSyllabusItemRequest> items) {
		Set<Long> syllabusIds = new HashSet<>();
		Set<Integer> sortOrders = new HashSet<>();
		for (TrainingProgramSyllabusItemRequest item : items) {
			if (!syllabusIds.add(item.syllabusId())) {
				throw new BadRequestException("DUPLICATE_TRAINING_PROGRAM_SYLLABUS", "Duplicate syllabus in training program");
			}
			if (!sortOrders.add(item.sortOrder())) {
				throw new BadRequestException("DUPLICATE_TRAINING_PROGRAM_SYLLABUS_SORT_ORDER", "Duplicate syllabus sort order in training program");
			}
		}
	}

	private void validateTransition(TrainingProgram program, TrainingProgramStatus target) {
		if (program.getStatus() == target) {
			return;
		}
		boolean allowed = (program.getStatus() == TrainingProgramStatus.Planning && target == TrainingProgramStatus.Active)
				|| (program.getStatus() == TrainingProgramStatus.Planning && target == TrainingProgramStatus.Inactive)
				|| (program.getStatus() == TrainingProgramStatus.Active && target == TrainingProgramStatus.Inactive);
		if (!allowed) {
			throw new ConflictException("INVALID_TRAINING_PROGRAM_STATUS_TRANSITION", "Invalid training program status transition");
		}
		if (target == TrainingProgramStatus.Active && programSyllabusRepository.countByIdProgramId(program.getId()) == 0) {
			throw new ConflictException("TRAINING_PROGRAM_SYLLABUS_REQUIRED", "Training program requires at least one active syllabus before publishing");
		}
	}

	private TrainingProgram findProgram(Long id) {
		return programRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Training program not found"));
	}

	private void ensureProgramExists(Long id) {
		if (!programRepository.existsById(id)) {
			throw new NotFoundException("Training program not found");
		}
	}

	private void ensurePlanning(TrainingProgram program) {
		if (program.getStatus() != TrainingProgramStatus.Planning) {
			throw new ConflictException("TRAINING_PROGRAM_NOT_EDITABLE", "Only planning training program can be edited");
		}
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
