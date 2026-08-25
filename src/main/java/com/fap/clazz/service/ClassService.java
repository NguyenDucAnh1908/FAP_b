package com.fap.clazz.service;

import com.fap.clazz.dto.ClassResponse;
import com.fap.clazz.dto.CreateClassRequest;
import com.fap.clazz.dto.UpdateClassRequest;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.mapper.ClassMapper;
import com.fap.clazz.repository.ClassAdminRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.api.PageRequestFactory;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.program.entity.TrainingProgram;
import com.fap.program.enums.TrainingProgramStatus;
import com.fap.program.repository.TrainingProgramRepository;
import com.fap.common.security.FapUserPrincipal;
import com.fap.result.service.CourseResultService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ClassService {

	private final ClassRepository classRepository;
	private final ClassAdminRepository classAdminRepository;
	private final ClassTrainerRepository classTrainerRepository;
	private final TrainingProgramRepository trainingProgramRepository;
	private final ClassMapper classMapper;
	private final AuditLogService auditLogService;
	private final ClassEnrollmentService classEnrollmentService;
	private final CourseResultService courseResultService;

	public ClassService(
			ClassRepository classRepository,
			ClassAdminRepository classAdminRepository,
			ClassTrainerRepository classTrainerRepository,
			TrainingProgramRepository trainingProgramRepository,
			ClassMapper classMapper,
			AuditLogService auditLogService,
			ClassEnrollmentService classEnrollmentService,
			CourseResultService courseResultService) {
		this.classRepository = classRepository;
		this.classAdminRepository = classAdminRepository;
		this.classTrainerRepository = classTrainerRepository;
		this.trainingProgramRepository = trainingProgramRepository;
		this.classMapper = classMapper;
		this.auditLogService = auditLogService;
		this.classEnrollmentService = classEnrollmentService;
		this.courseResultService = courseResultService;
	}

	@Transactional(readOnly = true)
	public Page<ClassResponse> list(
			ClassStatus status,
			Long trainingProgramId,
			String keyword,
			int page,
			int limit) {
		return listScoped(null, status, trainingProgramId, keyword, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<ClassResponse> listScoped(
			FapUserPrincipal principal,
			ClassStatus status,
			Long trainingProgramId,
			String keyword,
			int page,
			int limit) {
		return listScoped(principal, status, trainingProgramId, keyword, page, limit, null, null);
	}

	@Transactional(readOnly = true)
	public Page<ClassResponse> listScoped(
			FapUserPrincipal principal,
			ClassStatus status,
			Long trainingProgramId,
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
				"id", "createdAt", "name", "classCode", "startDate", "endDate", "status");
		return classRepository.searchScoped(scopeUserId(principal), status, trainingProgramId, normalize(keyword), pageRequest)
				.map(classMapper::toResponse);
	}

	@Transactional
	public ClassResponse create(CreateClassRequest request, Long currentUserId) {
		if (classRepository.existsByClassCodeIgnoreCase(request.classCode())) {
			throw new ConflictException("CLASS_CODE_EXISTS", "Class code already exists");
		}
		validateDateRange(request.startDate(), request.endDate());
		validateEnrollmentDateRange(request.enrollmentStartDate(), request.enrollmentEndDate());
		TrainingProgram program = trainingProgramRepository.findById(request.trainingProgramId())
				.orElseThrow(() -> new NotFoundException("Training program not found"));
		if (program.getStatus() != TrainingProgramStatus.Active) {
			throw new ConflictException("CLASS_TRAINING_PROGRAM_NOT_ACTIVE", "Class requires an active training program");
		}
		LocalDateTime now = LocalDateTime.now();
		FapClass fapClass = new FapClass();
		fapClass.setName(request.name());
		fapClass.setClassCode(request.classCode().trim().toUpperCase());
		fapClass.setTrainingProgram(program);
		fapClass.setStatus(ClassStatus.Planning);
		applyFields(fapClass, request);
		fapClass.setCreatedAt(now);
		fapClass.setUpdatedAt(now);
		fapClass.setCreatedBy(currentUserId);
		fapClass.setUpdatedBy(currentUserId);
		FapClass saved = classRepository.save(fapClass);
		auditLogService.record("CREATE_CLASS", "class", saved.getId());
		return classMapper.toResponse(saved);
	}

	@Transactional(readOnly = true)
	public ClassResponse get(Long id) {
		return classMapper.toResponse(findClass(id));
	}

	@Transactional
	public ClassResponse update(Long id, UpdateClassRequest request, Long currentUserId) {
		FapClass fapClass = findClass(id);
		ensurePlanning(fapClass);
		validateDateRange(request.startDate(), request.endDate());
		validateEnrollmentDateRange(request.enrollmentStartDate(), request.enrollmentEndDate());
		if (request.capacity() != null) {
			classEnrollmentService.validateCapacity(id, request.capacity());
		}
		applyFields(fapClass, request);
		fapClass.setUpdatedAt(LocalDateTime.now());
		fapClass.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_CLASS", "class", fapClass.getId());
		return classMapper.toResponse(fapClass);
	}

	@Transactional
	public ClassResponse updateStatus(Long id, ClassStatus status, Long currentUserId) {
		FapClass fapClass = findClassForUpdate(id);
		validateTransition(fapClass.getStatus(), status);
		if (fapClass.getStatus() == ClassStatus.Planning && status == ClassStatus.Active) {
			validateReadyForActivation(fapClass);
		}
		if (fapClass.getStatus() == ClassStatus.Active && status == ClassStatus.Closed) {
			courseResultService.finalizeForClosure(fapClass, currentUserId);
		}
		fapClass.setStatus(status);
		fapClass.setUpdatedAt(LocalDateTime.now());
		fapClass.setUpdatedBy(currentUserId);
		auditLogService.record("UPDATE_CLASS_STATUS:" + status.name(), "class", fapClass.getId());
		return classMapper.toResponse(fapClass);
	}

	@Transactional
	public void delete(Long id, Long currentUserId) {
		FapClass fapClass = findClass(id);
		ensurePlanning(fapClass);
		fapClass.setDeleted(true);
		fapClass.setDeletedAt(LocalDateTime.now());
		fapClass.setUpdatedAt(LocalDateTime.now());
		fapClass.setUpdatedBy(currentUserId);
		auditLogService.record("DELETE_CLASS", "class", fapClass.getId());
	}

	private FapClass findClass(Long id) {
		return classRepository.findWithTrainingProgramById(id)
				.orElseThrow(() -> new NotFoundException("Class not found"));
	}

	private void applyFields(FapClass fapClass, CreateClassRequest request) {
		fapClass.setLocation(request.location());
		fapClass.setLocationDetail(request.locationDetail());
		fapClass.setFsu(request.fsu());
		fapClass.setClassTime(request.classTime());
		fapClass.setStartDate(request.startDate());
		fapClass.setEndDate(request.endDate());
		fapClass.setDuration(request.duration());
		fapClass.setCapacity(request.capacity());
		fapClass.setSelfEnrollmentEnabled(request.selfEnrollmentEnabled());
		fapClass.setEnrollmentStartDate(request.enrollmentStartDate());
		fapClass.setEnrollmentEndDate(request.enrollmentEndDate());
	}

	private void applyFields(FapClass fapClass, UpdateClassRequest request) {
		if (request.name() != null) {
			fapClass.setName(request.name());
		}
		fapClass.setLocation(request.location());
		fapClass.setLocationDetail(request.locationDetail());
		fapClass.setFsu(request.fsu());
		fapClass.setClassTime(request.classTime());
		fapClass.setStartDate(request.startDate());
		fapClass.setEndDate(request.endDate());
		fapClass.setDuration(request.duration());
		if (request.capacity() != null) {
			fapClass.setCapacity(request.capacity());
		}
		if (request.selfEnrollmentEnabled() != null) {
			fapClass.setSelfEnrollmentEnabled(request.selfEnrollmentEnabled());
		}
		fapClass.setEnrollmentStartDate(request.enrollmentStartDate());
		fapClass.setEnrollmentEndDate(request.enrollmentEndDate());
	}

	private void validateTransition(ClassStatus current, ClassStatus target) {
		if (current == target) {
			return;
		}
		boolean allowed = (current == ClassStatus.Planning && target == ClassStatus.Active)
				|| (current == ClassStatus.Active && target == ClassStatus.Closed);
		if (!allowed) {
			throw new ConflictException("INVALID_CLASS_STATUS_TRANSITION", "Invalid class status transition");
		}
	}

	private void validateReadyForActivation(FapClass fapClass) {
		if (fapClass.getTrainingProgram().getStatus() != TrainingProgramStatus.Active) {
			throw new ConflictException("CLASS_TRAINING_PROGRAM_NOT_ACTIVE", "Class requires an active training program");
		}
		if (fapClass.getStartDate() == null || fapClass.getEndDate() == null) {
			throw new ConflictException("CLASS_SCHEDULE_REQUIRED", "Class requires start date and end date before activation");
		}
		validateDateRange(fapClass.getStartDate(), fapClass.getEndDate());
		if (!classAdminRepository.existsByFapClassId(fapClass.getId())) {
			throw new ConflictException("CLASS_ADMIN_REQUIRED", "Class requires at least one class admin before activation");
		}
		if (!classTrainerRepository.existsByFapClassId(fapClass.getId())) {
			throw new ConflictException("CLASS_TRAINER_REQUIRED", "Class requires at least one trainer before activation");
		}
	}

	private void ensurePlanning(FapClass fapClass) {
		if (fapClass.getStatus() != ClassStatus.Planning) {
			throw new ConflictException("CLASS_NOT_EDITABLE", "Only planning class can be edited");
		}
	}

	private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new BadRequestException("INVALID_CLASS_DATE_RANGE", "Class start date must be before or equal to end date");
		}
	}

	private FapClass findClassForUpdate(Long id) {
		return classRepository.findWithTrainingProgramByIdForUpdate(id)
				.orElseThrow(() -> new NotFoundException("Class not found"));
	}

	private void validateEnrollmentDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			throw new BadRequestException("INVALID_CLASS_ENROLLMENT_DATE_RANGE", "Enrollment start date must be before or equal to end date");
		}
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private Long scopeUserId(FapUserPrincipal principal) {
		return principal == null || principal.roles().contains("Super Admin") ? null : principal.id();
	}
}
