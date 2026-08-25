package com.fap.clazz.service;

import com.fap.clazz.entity.FapClass;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.mapper.ClassMapper;
import com.fap.clazz.repository.ClassAdminRepository;
import com.fap.clazz.repository.ClassRepository;
import com.fap.clazz.repository.ClassTrainerRepository;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.program.entity.TrainingProgram;
import com.fap.program.enums.TrainingProgramStatus;
import com.fap.program.repository.TrainingProgramRepository;
import com.fap.result.service.CourseResultService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the class lifecycle: Planning -> Active -> Closed, one way only. Activation is the
 * expensive step — it is the point where the class must actually be ready to run, so each
 * readiness guard gets its own case.
 */
class ClassStateMachineTest {

	private static final long CLASS_ID = 21L;
	private static final long CURRENT_USER_ID = 7L;

	private final ClassRepository classRepository = mock(ClassRepository.class);
	private final ClassAdminRepository classAdminRepository = mock(ClassAdminRepository.class);
	private final ClassTrainerRepository classTrainerRepository = mock(ClassTrainerRepository.class);
	private final TrainingProgramRepository trainingProgramRepository = mock(TrainingProgramRepository.class);
	private final ClassMapper classMapper = mock(ClassMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final ClassEnrollmentService classEnrollmentService = mock(ClassEnrollmentService.class);
	private final CourseResultService courseResultService = mock(CourseResultService.class);

	private final ClassService service = new ClassService(
			classRepository,
			classAdminRepository,
			classTrainerRepository,
			trainingProgramRepository,
			classMapper,
			auditLogService,
			classEnrollmentService,
			courseResultService);

	@ParameterizedTest(name = "{0} -> {1} is allowed")
	@CsvSource({
			"Planning, Active",
			"Active, Closed"
	})
	void allowsValidTransition(ClassStatus current, ClassStatus target) {
		FapClass fapClass = givenClass(current, TrainingProgramStatus.Active);
		givenClassIsReadyForActivation();

		service.updateStatus(CLASS_ID, target, CURRENT_USER_ID);

		assertThat(fapClass.getStatus()).isEqualTo(target);
		assertThat(fapClass.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		verify(auditLogService).record("UPDATE_CLASS_STATUS:" + target.name(), "class", CLASS_ID);
		if (target == ClassStatus.Closed) {
			verify(courseResultService).finalizeForClosure(fapClass, CURRENT_USER_ID);
		}
	}

	@ParameterizedTest(name = "{0} -> {1} is rejected")
	@CsvSource({
			"Planning, Closed",
			"Active, Planning",
			"Closed, Planning",
			"Closed, Active"
	})
	void rejectsInvalidTransition(ClassStatus current, ClassStatus target) {
		FapClass fapClass = givenClass(current, TrainingProgramStatus.Active);
		givenClassIsReadyForActivation();

		assertThatThrownBy(() -> service.updateStatus(CLASS_ID, target, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("INVALID_CLASS_STATUS_TRANSITION");

		assertThat(fapClass.getStatus()).isEqualTo(current);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@ParameterizedTest(name = "{0} -> {0} is a no-op")
	@CsvSource({"Planning", "Active", "Closed"})
	void allowsTransitionToSameStatus(ClassStatus current) {
		FapClass fapClass = givenClass(current, TrainingProgramStatus.Active);
		givenClassIsReadyForActivation();

		service.updateStatus(CLASS_ID, current, CURRENT_USER_ID);

		assertThat(fapClass.getStatus()).isEqualTo(current);
	}

	@Test
	void rejectsActivationWhenTrainingProgramIsNotActive() {
		givenClass(ClassStatus.Planning, TrainingProgramStatus.Planning);
		givenClassIsReadyForActivation();

		assertThatThrownBy(() -> service.updateStatus(CLASS_ID, ClassStatus.Active, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_TRAINING_PROGRAM_NOT_ACTIVE");
	}

	@Test
	void rejectsActivationWhenStartDateIsMissing() {
		FapClass fapClass = givenClass(ClassStatus.Planning, TrainingProgramStatus.Active);
		givenClassIsReadyForActivation();
		fapClass.setStartDate(null);

		assertThatThrownBy(() -> service.updateStatus(CLASS_ID, ClassStatus.Active, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_SCHEDULE_REQUIRED");
	}

	@Test
	void rejectsActivationWhenEndDateIsMissing() {
		FapClass fapClass = givenClass(ClassStatus.Planning, TrainingProgramStatus.Active);
		givenClassIsReadyForActivation();
		fapClass.setEndDate(null);

		assertThatThrownBy(() -> service.updateStatus(CLASS_ID, ClassStatus.Active, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_SCHEDULE_REQUIRED");
	}

	@Test
	void rejectsActivationWhenEndDatePrecedesStartDate() {
		FapClass fapClass = givenClass(ClassStatus.Planning, TrainingProgramStatus.Active);
		givenClassIsReadyForActivation();
		fapClass.setStartDate(LocalDate.of(2026, 3, 10));
		fapClass.setEndDate(LocalDate.of(2026, 3, 1));

		assertThatThrownBy(() -> service.updateStatus(CLASS_ID, ClassStatus.Active, CURRENT_USER_ID))
				.isInstanceOf(BadRequestException.class)
				.extracting("code")
				.isEqualTo("INVALID_CLASS_DATE_RANGE");
	}

	@Test
	void rejectsActivationWithoutAnyClassAdmin() {
		givenClass(ClassStatus.Planning, TrainingProgramStatus.Active);
		givenClassIsReadyForActivation();
		when(classAdminRepository.existsByFapClassId(CLASS_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.updateStatus(CLASS_ID, ClassStatus.Active, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_ADMIN_REQUIRED");
	}

	@Test
	void rejectsActivationWithoutAnyTrainer() {
		givenClass(ClassStatus.Planning, TrainingProgramStatus.Active);
		givenClassIsReadyForActivation();
		when(classTrainerRepository.existsByFapClassId(CLASS_ID)).thenReturn(false);

		assertThatThrownBy(() -> service.updateStatus(CLASS_ID, ClassStatus.Active, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("CLASS_TRAINER_REQUIRED");
	}

	/**
	 * Closing runs no readiness guards: a class that is already running must always be closable,
	 * even if staff assignments were removed in the meantime.
	 */
	@Test
	void closingSkipsActivationReadinessChecks() {
		FapClass fapClass = givenClass(ClassStatus.Active, TrainingProgramStatus.Inactive);
		fapClass.setStartDate(null);
		fapClass.setEndDate(null);
		when(classAdminRepository.existsByFapClassId(CLASS_ID)).thenReturn(false);
		when(classTrainerRepository.existsByFapClassId(CLASS_ID)).thenReturn(false);

		service.updateStatus(CLASS_ID, ClassStatus.Closed, CURRENT_USER_ID);

		assertThat(fapClass.getStatus()).isEqualTo(ClassStatus.Closed);
	}

	private FapClass givenClass(ClassStatus status, TrainingProgramStatus programStatus) {
		TrainingProgram program = new TrainingProgram();
		program.setId(99L);
		program.setStatus(programStatus);

		FapClass fapClass = new FapClass();
		fapClass.setId(CLASS_ID);
		fapClass.setStatus(status);
		fapClass.setTrainingProgram(program);
		fapClass.setStartDate(LocalDate.of(2026, 3, 1));
		fapClass.setEndDate(LocalDate.of(2026, 3, 31));
		when(classRepository.findWithTrainingProgramById(CLASS_ID)).thenReturn(Optional.of(fapClass));
		when(classRepository.findWithTrainingProgramByIdForUpdate(CLASS_ID)).thenReturn(Optional.of(fapClass));
		return fapClass;
	}

	private void givenClassIsReadyForActivation() {
		when(classAdminRepository.existsByFapClassId(CLASS_ID)).thenReturn(true);
		when(classTrainerRepository.existsByFapClassId(CLASS_ID)).thenReturn(true);
	}
}
