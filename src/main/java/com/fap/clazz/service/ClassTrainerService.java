package com.fap.clazz.service;

import com.fap.clazz.dto.ClassTrainerItemRequest;
import com.fap.clazz.dto.ClassTrainerResponse;
import com.fap.clazz.dto.UpdateClassTrainersRequest;
import com.fap.clazz.entity.ClassTrainer;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.mapper.ClassTrainerMapper;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.notification.service.NotificationService;
import com.fap.program.repository.TrainingProgramSyllabusRepository;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.repository.SyllabusRepository;
import com.fap.user.entity.User;
import com.fap.user.enums.UserStatus;
import com.fap.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ClassTrainerService {

	private static final String TRAINER_ROLE = "Trainer";

	private final ClassRepository classRepository;
	private final ClassTrainerRepository classTrainerRepository;
	private final UserRepository userRepository;
	private final SyllabusRepository syllabusRepository;
	private final TrainingProgramSyllabusRepository trainingProgramSyllabusRepository;
	private final ClassTrainerMapper classTrainerMapper;
	private final AuditLogService auditLogService;
	private final NotificationService notificationService;

	public ClassTrainerService(
			ClassRepository classRepository,
			ClassTrainerRepository classTrainerRepository,
			UserRepository userRepository,
			SyllabusRepository syllabusRepository,
			TrainingProgramSyllabusRepository trainingProgramSyllabusRepository,
			ClassTrainerMapper classTrainerMapper,
			AuditLogService auditLogService,
			NotificationService notificationService) {
		this.classRepository = classRepository;
		this.classTrainerRepository = classTrainerRepository;
		this.userRepository = userRepository;
		this.syllabusRepository = syllabusRepository;
		this.trainingProgramSyllabusRepository = trainingProgramSyllabusRepository;
		this.classTrainerMapper = classTrainerMapper;
		this.auditLogService = auditLogService;
		this.notificationService = notificationService;
	}

	@Transactional(readOnly = true)
	public List<ClassTrainerResponse> list(Long classId) {
		ensureClassExists(classId);
		return classTrainerRepository.findByFapClassIdOrderByIdAsc(classId).stream()
				.map(classTrainerMapper::toResponse)
				.toList();
	}

	@Transactional
	public List<ClassTrainerResponse> replace(Long classId, UpdateClassTrainersRequest request) {
		FapClass fapClass = findPlanningClass(classId);
		validateNoDuplicateTrainerScopes(request.trainers());
		classTrainerRepository.deleteByFapClassId(classId);
		List<ClassTrainer> trainers = request.trainers().stream()
				.map(item -> createTrainerAssignment(fapClass, item))
				.toList();
		classTrainerRepository.saveAll(trainers);
		auditLogService.record("UPDATE_CLASS_TRAINERS", "class", classId);
		trainers.forEach(trainer -> notificationService.create(
				trainer.getUser().getId(),
				"Class trainer assignment",
				"You have been assigned as trainer for " + fapClass.getClassCode() + " - " + fapClass.getName()));
		return trainers.stream()
				.map(classTrainerMapper::toResponse)
				.toList();
	}

	private ClassTrainer createTrainerAssignment(FapClass fapClass, ClassTrainerItemRequest item) {
		User user = userRepository.findWithRolesById(item.userId())
				.orElseThrow(() -> new NotFoundException("User not found"));
		if (user.getStatus() != UserStatus.Active || user.getRoles().stream().noneMatch(role -> TRAINER_ROLE.equals(role.getName()))) {
			throw new ConflictException("CLASS_TRAINER_ROLE_REQUIRED", "Assigned user must be an active Trainer");
		}
		Syllabus syllabus = null;
		if (item.syllabusId() != null) {
			Long programId = fapClass.getTrainingProgram().getId();
			if (!trainingProgramSyllabusRepository.existsByIdProgramIdAndIdSyllabusId(programId, item.syllabusId())) {
				throw new ConflictException("CLASS_TRAINER_SYLLABUS_NOT_IN_PROGRAM", "Trainer syllabus must belong to the class training program");
			}
			syllabus = syllabusRepository.findById(item.syllabusId())
					.orElseThrow(() -> new NotFoundException("Syllabus not found"));
		}
		ClassTrainer classTrainer = new ClassTrainer();
		classTrainer.setFapClass(fapClass);
		classTrainer.setUser(user);
		classTrainer.setSyllabus(syllabus);
		return classTrainer;
	}

	private void validateNoDuplicateTrainerScopes(List<ClassTrainerItemRequest> trainers) {
		Set<String> scopes = new HashSet<>();
		for (ClassTrainerItemRequest item : trainers) {
			String scope = item.userId() + ":" + (item.syllabusId() == null ? "ALL" : item.syllabusId());
			if (!scopes.add(scope)) {
				throw new BadRequestException("DUPLICATE_CLASS_TRAINER_SCOPE", "Duplicate class trainer assignment");
			}
		}
	}

	private void ensureClassExists(Long classId) {
		if (!classRepository.existsById(classId)) {
			throw new NotFoundException("Class not found");
		}
	}

	private FapClass findPlanningClass(Long classId) {
		FapClass fapClass = classRepository.findWithTrainingProgramById(classId)
				.orElseThrow(() -> new NotFoundException("Class not found"));
		if (fapClass.getStatus() != ClassStatus.Planning) {
			throw new ConflictException("CLASS_NOT_EDITABLE", "Only planning class can be edited");
		}
		return fapClass;
	}
}
