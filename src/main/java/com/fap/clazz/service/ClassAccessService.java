package com.fap.clazz.service;

import com.fap.clazz.repository.ClassAdminRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.clazz.repository.ClassEnrollmentRepository;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.common.exception.ForbiddenException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.security.FapUserPrincipal;
import com.fap.common.security.RoleNames;
import com.fap.training.entity.TrainingSession;
import com.fap.training.repository.TrainingSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassAccessService {

	private final ClassRepository classRepository;
	private final ClassAdminRepository classAdminRepository;
	private final ClassTrainerRepository classTrainerRepository;
	private final TrainingSessionRepository trainingSessionRepository;
	private final ClassEnrollmentRepository classEnrollmentRepository;

	public ClassAccessService(
			ClassRepository classRepository,
			ClassAdminRepository classAdminRepository,
			ClassTrainerRepository classTrainerRepository,
			TrainingSessionRepository trainingSessionRepository,
			ClassEnrollmentRepository classEnrollmentRepository) {
		this.classRepository = classRepository;
		this.classAdminRepository = classAdminRepository;
		this.classTrainerRepository = classTrainerRepository;
		this.trainingSessionRepository = trainingSessionRepository;
		this.classEnrollmentRepository = classEnrollmentRepository;
	}

	@Transactional(readOnly = true)
	public void assertCanViewClass(FapUserPrincipal principal, Long classId) {
		ensureClassExists(classId);
		if (isSuperAdmin(principal)
				|| isAssignedClassAdmin(principal, classId)
				|| isAssignedClassTrainer(principal, classId)
				|| isEnrolledTrainee(principal, classId)) {
			return;
		}
		throw new ForbiddenException("You are not assigned to this class")
				.withMessageKey("error.ACCESS_DENIED.class_not_assigned");
	}

	@Transactional(readOnly = true)
	public void assertCanViewEnrollmentRoster(FapUserPrincipal principal, Long classId) {
		ensureClassExists(classId);
		if (isSuperAdmin(principal)
				|| isAssignedClassAdmin(principal, classId)
				|| isAssignedClassTrainer(principal, classId)) {
			return;
		}
		throw new ForbiddenException("You cannot view the class enrollment roster")
				.withMessageKey("error.ACCESS_DENIED.class_roster_view");
	}

	@Transactional(readOnly = true)
	public void assertCanManageClass(FapUserPrincipal principal, Long classId) {
		ensureClassExists(classId);
		if (isSuperAdmin(principal) || isAssignedClassAdmin(principal, classId)) {
			return;
		}
		throw new ForbiddenException("You cannot manage this class")
				.withMessageKey("error.ACCESS_DENIED.class_manage");
	}

	@Transactional(readOnly = true)
	public void assertCanManageEnrollmentRoster(FapUserPrincipal principal, Long classId) {
		ensureClassExists(classId);
		if (isSuperAdmin(principal)
				|| isAssignedClassAdmin(principal, classId)) {
			return;
		}
		throw new ForbiddenException("You cannot manage the class enrollment roster")
				.withMessageKey("error.ACCESS_DENIED.class_roster_manage");
	}

	@Transactional(readOnly = true)
	public void assertCanCreateSession(FapUserPrincipal principal, Long classId, Long trainerId) {
		ensureClassExists(classId);
		if (isSuperAdmin(principal) || isAssignedClassAdmin(principal, classId)) {
			return;
		}
		if (principal.id().equals(trainerId) && isAssignedClassTrainer(principal, classId)) {
			return;
		}
		throw new ForbiddenException("You cannot create this training session")
				.withMessageKey("error.ACCESS_DENIED.training_session_create");
	}

	@Transactional(readOnly = true)
	public void assertCanViewSession(FapUserPrincipal principal, Long trainingSessionId) {
		TrainingSession session = trainingSessionRepository.getWithClassAndTrainerOrThrow(trainingSessionId);
		Long classId = session.getFapClass().getId();
		if (isSuperAdmin(principal)
				|| isAssignedClassAdmin(principal, classId)
				|| isSessionTrainer(principal, session)
				|| isAssignedClassTrainer(principal, classId)
				|| isEnrolledTrainee(principal, classId)) {
			return;
		}
		throw new ForbiddenException("You are not assigned to this training session")
				.withMessageKey("error.ACCESS_DENIED.training_session_not_assigned");
	}

	@Transactional(readOnly = true)
	public void assertCanManageSession(FapUserPrincipal principal, Long trainingSessionId) {
		TrainingSession session = trainingSessionRepository.getWithClassAndTrainerOrThrow(trainingSessionId);
		Long classId = session.getFapClass().getId();
		if (isSuperAdmin(principal)
				|| isAssignedClassAdmin(principal, classId)
				|| isSessionTrainer(principal, session)) {
			return;
		}
		throw new ForbiddenException("You cannot manage this training session")
				.withMessageKey("error.ACCESS_DENIED.training_session_manage");
	}

	private void ensureClassExists(Long classId) {
		if (!classRepository.existsById(classId)) {
			throw new NotFoundException("class", "Class not found");
		}
	}

	private boolean isSuperAdmin(FapUserPrincipal principal) {
		return principal.roles().contains(RoleNames.SUPER_ADMIN);
	}

	private boolean isAssignedClassAdmin(FapUserPrincipal principal, Long classId) {
		return principal.roles().contains(RoleNames.CLASS_ADMIN)
				&& classAdminRepository.existsByFapClassIdAndUserId(classId, principal.id());
	}

	private boolean isAssignedClassTrainer(FapUserPrincipal principal, Long classId) {
		return principal.roles().contains(RoleNames.TRAINER)
				&& classTrainerRepository.existsByFapClassIdAndUserId(classId, principal.id());
	}

	private boolean isSessionTrainer(FapUserPrincipal principal, TrainingSession session) {
		return principal.roles().contains(RoleNames.TRAINER)
				&& session.getTrainer().getId().equals(principal.id());
	}

	private boolean isEnrolledTrainee(FapUserPrincipal principal, Long classId) {
		return principal.roles().contains(RoleNames.TRAINEE)
				&& classEnrollmentRepository.existsByFapClassIdAndUserIdAndStatusIn(
						classId,
						principal.id(),
						java.util.List.of(ClassEnrollmentStatus.Enrolled, ClassEnrollmentStatus.Completed));
	}
}
