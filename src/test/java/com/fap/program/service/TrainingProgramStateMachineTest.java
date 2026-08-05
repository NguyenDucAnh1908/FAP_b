package com.fap.program.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.ConflictException;
import com.fap.program.entity.TrainingProgram;
import com.fap.program.enums.TrainingProgramStatus;
import com.fap.program.mapper.TrainingProgramMapper;
import com.fap.program.repository.TrainingProgramRepository;
import com.fap.program.repository.TrainingProgramSyllabusRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
 * Guards the training program lifecycle: Planning -> Active, and Inactive reachable from both
 * Planning and Active. Nothing may come back out of Inactive.
 */
class TrainingProgramStateMachineTest {

	private static final long PROGRAM_ID = 11L;
	private static final long CURRENT_USER_ID = 7L;

	private final TrainingProgramRepository programRepository = mock(TrainingProgramRepository.class);
	private final TrainingProgramSyllabusRepository programSyllabusRepository =
			mock(TrainingProgramSyllabusRepository.class);
	private final SyllabusRepository syllabusRepository = mock(SyllabusRepository.class);
	private final TrainingProgramMapper trainingProgramMapper = mock(TrainingProgramMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);

	private final TrainingProgramService service = new TrainingProgramService(
			programRepository,
			programSyllabusRepository,
			syllabusRepository,
			trainingProgramMapper,
			auditLogService);

	@ParameterizedTest(name = "{0} -> {1} is allowed")
	@CsvSource({
			"Planning, Active",
			"Planning, Inactive",
			"Active, Inactive"
	})
	void allowsValidTransition(TrainingProgramStatus current, TrainingProgramStatus target) {
		TrainingProgram program = givenProgram(current);
		when(programSyllabusRepository.countByIdProgramId(PROGRAM_ID)).thenReturn(2L);

		service.updateStatus(PROGRAM_ID, target, CURRENT_USER_ID);

		assertThat(program.getStatus()).isEqualTo(target);
		assertThat(program.getUpdatedBy()).isEqualTo(CURRENT_USER_ID);
		verify(auditLogService).record(
				"UPDATE_TRAINING_PROGRAM_STATUS:" + target.name(), "training_program", PROGRAM_ID);
	}

	@ParameterizedTest(name = "{0} -> {1} is rejected")
	@CsvSource({
			"Active, Planning",
			"Inactive, Planning",
			"Inactive, Active"
	})
	void rejectsInvalidTransition(TrainingProgramStatus current, TrainingProgramStatus target) {
		TrainingProgram program = givenProgram(current);
		when(programSyllabusRepository.countByIdProgramId(PROGRAM_ID)).thenReturn(2L);

		assertThatThrownBy(() -> service.updateStatus(PROGRAM_ID, target, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("INVALID_TRAINING_PROGRAM_STATUS_TRANSITION");

		assertThat(program.getStatus()).isEqualTo(current);
		verify(auditLogService, never()).record(anyString(), anyString(), anyLong());
	}

	@ParameterizedTest(name = "{0} -> {0} is a no-op")
	@CsvSource({"Planning", "Active", "Inactive"})
	void allowsTransitionToSameStatus(TrainingProgramStatus current) {
		TrainingProgram program = givenProgram(current);

		service.updateStatus(PROGRAM_ID, current, CURRENT_USER_ID);

		assertThat(program.getStatus()).isEqualTo(current);
	}

	@Test
	void rejectsPublishingWithoutAnyAttachedSyllabus() {
		givenProgram(TrainingProgramStatus.Planning);
		when(programSyllabusRepository.countByIdProgramId(PROGRAM_ID)).thenReturn(0L);

		assertThatThrownBy(() ->
				service.updateStatus(PROGRAM_ID, TrainingProgramStatus.Active, CURRENT_USER_ID))
				.isInstanceOf(ConflictException.class)
				.extracting("code")
				.isEqualTo("TRAINING_PROGRAM_SYLLABUS_REQUIRED");
	}

	@Test
	void deactivatingDoesNotRequireAttachedSyllabuses() {
		TrainingProgram program = givenProgram(TrainingProgramStatus.Planning);
		when(programSyllabusRepository.countByIdProgramId(PROGRAM_ID)).thenReturn(0L);

		service.updateStatus(PROGRAM_ID, TrainingProgramStatus.Inactive, CURRENT_USER_ID);

		assertThat(program.getStatus()).isEqualTo(TrainingProgramStatus.Inactive);
	}

	private TrainingProgram givenProgram(TrainingProgramStatus status) {
		TrainingProgram program = new TrainingProgram();
		program.setId(PROGRAM_ID);
		program.setStatus(status);
		when(programRepository.findById(PROGRAM_ID)).thenReturn(Optional.of(program));
		return program;
	}
}
