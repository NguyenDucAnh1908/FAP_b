package com.fap.clazz.service;

import com.fap.clazz.repository.ClassAdminRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.common.exception.ForbiddenException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.security.FapUserPrincipal;
import com.fap.training.entity.TrainingSession;
import com.fap.training.repository.TrainingSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassAccessService {

	private static final String SUPER_ADMIN_ROLE = "Super Admin";
	private static final String CLASS_ADMIN_ROLE = "Class Admin";
	private static final String TRAINER_ROLE = "Trainer";

	private final ClassRepository classRepository;
	private final ClassAdminRepository classAdminRepository;
	private final ClassTrainerRepository classTrainerRepository;
	private final TrainingSessionRepository trainingSessionRepository;

	public ClassAccessService(
			ClassRepository classRepository,
			ClassAdminRepository classAdminRepository,
			ClassTrainerRepository classTrainerRepository,
			TrainingSessionRepository trainingSessionRepository) {
		this.classRepository = classRepository;
		this.classAdminRepository = classAdminRepository;
		this.classTrainerRepository = classTrainerRepository;
		this.trainingSessionRepository = trainingSessionRepository;
	}

	@Transactional(readOnly = true)
	public void assertCanViewClass(FapUserPrincipal principal, Long classId) {
		ensureClassExists(classId);
		if (isSuperAdmin(principal)
				|| isAssignedClassAdmin(principal, classId)
				|| isAssignedClassTrainer(principal, classId)) {
			return;
		}
		throw new ForbiddenException("You are not assigned to this class");
	}

	@Transactional(readOnly = true)
	public void assertCanManageClass(FapUserPrincipal principal, Long classId) {
		ensureClassExists(classId);
		if (isSuperAdmin(principal) || isAssignedClassAdmin(principal, classId)) {
			return;
		}
		throw new ForbiddenException("You cannot manage this class");
	}

	@Transactional(readOnly = true)
	public void assertCanViewSession(FapUserPrincipal principal, Long trainingSessionId) {
		TrainingSession session = findSession(trainingSessionId);
		Long classId = session.getFapClass().getId();
		if (isSuperAdmin(principal)
				|| isAssignedClassAdmin(principal, classId)
				|| isSessionTrainer(principal, session)
				|| isAssignedClassTrainer(principal, classId)) {
			return;
		}
		throw new ForbiddenException("You are not assigned to this training session");
	}

	@Transactional(readOnly = true)
	public void assertCanManageSession(FapUserPrincipal principal, Long trainingSessionId) {
		TrainingSession session = findSession(trainingSessionId);
		Long classId = session.getFapClass().getId();
		if (isSuperAdmin(principal)
				|| isAssignedClassAdmin(principal, classId)
				|| isSessionTrainer(principal, session)) {
			return;
		}
		throw new ForbiddenException("You cannot manage this training session");
	}

	private void ensureClassExists(Long classId) {
		if (!classRepository.existsById(classId)) {
			throw new NotFoundException("Class not found");
		}
	}

	private TrainingSession findSession(Long trainingSessionId) {
		return trainingSessionRepository.findWithClassAndTrainerById(trainingSessionId)
				.orElseThrow(() -> new NotFoundException("Training session not found"));
	}

	private boolean isSuperAdmin(FapUserPrincipal principal) {
		return principal.roles().contains(SUPER_ADMIN_ROLE);
	}

	private boolean isAssignedClassAdmin(FapUserPrincipal principal, Long classId) {
		return principal.roles().contains(CLASS_ADMIN_ROLE)
				&& classAdminRepository.existsByFapClassIdAndUserId(classId, principal.id());
	}

	private boolean isAssignedClassTrainer(FapUserPrincipal principal, Long classId) {
		return principal.roles().contains(TRAINER_ROLE)
				&& classTrainerRepository.existsByFapClassIdAndUserId(classId, principal.id());
	}

	private boolean isSessionTrainer(FapUserPrincipal principal, TrainingSession session) {
		return principal.roles().contains(TRAINER_ROLE)
				&& session.getTrainer().getId().equals(principal.id());
	}
}
