package com.fap.clazz.service;

import com.fap.clazz.repository.ClassAdminRepository;
import com.fap.clazz.repository.ClassEnrollmentRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.common.exception.ForbiddenException;
import com.fap.common.security.FapUserPrincipal;
import com.fap.training.repository.TrainingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassAccessServiceTest {

	@Mock
	private ClassRepository classRepository;
	@Mock
	private ClassAdminRepository classAdminRepository;
	@Mock
	private ClassTrainerRepository classTrainerRepository;
	@Mock
	private TrainingSessionRepository trainingSessionRepository;
	@Mock
	private ClassEnrollmentRepository classEnrollmentRepository;

	private ClassAccessService service;

	@BeforeEach
	void setUp() {
		service = new ClassAccessService(
				classRepository,
				classAdminRepository,
				classTrainerRepository,
				trainingSessionRepository,
				classEnrollmentRepository);
	}

	@Test
	void superAdminCanViewEnrollmentRoster() {
		when(classRepository.existsById(10L)).thenReturn(true);

		assertDoesNotThrow(() -> service.assertCanViewEnrollmentRoster(principal(1L, "Super Admin"), 10L));
	}

	@Test
	void assignedClassAdminCanViewEnrollmentRoster() {
		when(classRepository.existsById(10L)).thenReturn(true);
		when(classAdminRepository.existsByFapClassIdAndUserId(10L, 2L)).thenReturn(true);

		assertDoesNotThrow(() -> service.assertCanViewEnrollmentRoster(principal(2L, "Class Admin"), 10L));
	}

	@Test
	void assignedTrainerCanViewEnrollmentRoster() {
		when(classRepository.existsById(10L)).thenReturn(true);
		when(classTrainerRepository.existsByFapClassIdAndUserId(10L, 3L)).thenReturn(true);

		assertDoesNotThrow(() -> service.assertCanViewEnrollmentRoster(principal(3L, "Trainer"), 10L));
	}

	@Test
	void traineeCannotViewEnrollmentRoster() {
		when(classRepository.existsById(10L)).thenReturn(true);

		assertThrows(
				ForbiddenException.class,
				() -> service.assertCanViewEnrollmentRoster(principal(4L, "Trainee"), 10L));
	}

	@Test
	void assignedTrainerCannotManageEnrollmentRoster() {
		when(classRepository.existsById(10L)).thenReturn(true);

		assertThrows(
				ForbiddenException.class,
				() -> service.assertCanManageEnrollmentRoster(principal(3L, "Trainer"), 10L));
	}

	@Test
	void superAdminCanManageEnrollmentRoster() {
		when(classRepository.existsById(10L)).thenReturn(true);

		assertDoesNotThrow(() -> service.assertCanManageEnrollmentRoster(principal(1L, "Super Admin"), 10L));
	}

	@Test
	void assignedClassAdminCanManageEnrollmentRoster() {
		when(classRepository.existsById(10L)).thenReturn(true);
		when(classAdminRepository.existsByFapClassIdAndUserId(10L, 2L)).thenReturn(true);

		assertDoesNotThrow(() -> service.assertCanManageEnrollmentRoster(principal(2L, "Class Admin"), 10L));
	}

	@Test
	void unassignedTrainerCannotManageEnrollmentRoster() {
		when(classRepository.existsById(10L)).thenReturn(true);

		assertThrows(
				ForbiddenException.class,
				() -> service.assertCanManageEnrollmentRoster(principal(3L, "Trainer"), 10L));
	}

	@Test
	void assignedTrainerCanCreateSessionOnlyForSelf() {
		when(classRepository.existsById(10L)).thenReturn(true);
		when(classTrainerRepository.existsByFapClassIdAndUserId(10L, 3L)).thenReturn(true);

		assertDoesNotThrow(() -> service.assertCanCreateSession(principal(3L, "Trainer"), 10L, 3L));
		assertThrows(
				ForbiddenException.class,
				() -> service.assertCanCreateSession(principal(3L, "Trainer"), 10L, 4L));
	}

	private FapUserPrincipal principal(Long id, String role) {
		return new FapUserPrincipal(id, "user" + id + "@fap.local", "", Set.of(role), true, List.of());
	}
}
