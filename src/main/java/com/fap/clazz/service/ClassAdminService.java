package com.fap.clazz.service;

import com.fap.clazz.dto.ClassAdminResponse;
import com.fap.clazz.dto.UpdateClassAdminsRequest;
import com.fap.clazz.entity.ClassAdmin;
import com.fap.clazz.entity.ClassAdminId;
import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.mapper.ClassAdminMapper;
import com.fap.clazz.repository.ClassAdminRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.notification.service.NotificationService;
import com.fap.user.entity.User;
import com.fap.user.enums.UserStatus;
import com.fap.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ClassAdminService {

	private static final String CLASS_ADMIN_ROLE = "Class Admin";

	private final ClassRepository classRepository;
	private final ClassAdminRepository classAdminRepository;
	private final UserRepository userRepository;
	private final ClassAdminMapper classAdminMapper;
	private final AuditLogService auditLogService;
	private final NotificationService notificationService;

	public ClassAdminService(
			ClassRepository classRepository,
			ClassAdminRepository classAdminRepository,
			UserRepository userRepository,
			ClassAdminMapper classAdminMapper,
			AuditLogService auditLogService,
			NotificationService notificationService) {
		this.classRepository = classRepository;
		this.classAdminRepository = classAdminRepository;
		this.userRepository = userRepository;
		this.classAdminMapper = classAdminMapper;
		this.auditLogService = auditLogService;
		this.notificationService = notificationService;
	}

	@Transactional(readOnly = true)
	public List<ClassAdminResponse> list(Long classId) {
		ensureClassExists(classId);
		return classAdminRepository.findByFapClassIdOrderByUserFullNameAsc(classId).stream()
				.map(classAdminMapper::toResponse)
				.toList();
	}

	@Transactional
	public List<ClassAdminResponse> replace(Long classId, UpdateClassAdminsRequest request) {
		FapClass fapClass = findPlanningClass(classId);
		validateNoDuplicateUsers(request.userIds());
		classAdminRepository.deleteByFapClassId(classId);
		List<ClassAdmin> admins = request.userIds().stream()
				.map(userId -> createAdminAssignment(fapClass, userId))
				.toList();
		classAdminRepository.saveAll(admins);
		auditLogService.record("UPDATE_CLASS_ADMINS", "class", classId);
		admins.forEach(admin -> notificationService.create(
				admin.getUser().getId(),
				"Class admin assignment",
				"You have been assigned as class admin for " + fapClass.getClassCode() + " - " + fapClass.getName()));
		return admins.stream()
				.map(classAdminMapper::toResponse)
				.toList();
	}

	private ClassAdmin createAdminAssignment(FapClass fapClass, Long userId) {
		User user = userRepository.findWithRolesById(userId)
				.orElseThrow(() -> new NotFoundException("User not found"));
		if (user.getStatus() != UserStatus.Active || user.getRoles().stream().noneMatch(role -> CLASS_ADMIN_ROLE.equals(role.getName()))) {
			throw new ConflictException("CLASS_ADMIN_ROLE_REQUIRED", "Assigned user must be an active Class Admin");
		}
		ClassAdmin classAdmin = new ClassAdmin();
		classAdmin.setId(new ClassAdminId(fapClass.getId(), user.getId()));
		classAdmin.setFapClass(fapClass);
		classAdmin.setUser(user);
		return classAdmin;
	}

	private void validateNoDuplicateUsers(List<Long> userIds) {
		Set<Long> uniqueUserIds = new HashSet<>();
		for (Long userId : userIds) {
			if (!uniqueUserIds.add(userId)) {
				throw new BadRequestException("DUPLICATE_CLASS_ADMIN", "Duplicate class admin assignment");
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
