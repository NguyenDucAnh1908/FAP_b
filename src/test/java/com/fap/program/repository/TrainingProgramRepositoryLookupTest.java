package com.fap.program.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.program.entity.TrainingProgram;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingProgramRepositoryLookupTest {

	private static final long PROGRAM_ID = 11L;

	private final TrainingProgramRepository programRepository = mock(TrainingProgramRepository.class);

	TrainingProgramRepositoryLookupTest() {
		doCallRealMethod().when(programRepository).getTrainingProgramOrThrow(any());
	}

	@Test
	void returnsTheProgramWhenItExists() {
		TrainingProgram program = new TrainingProgram();
		program.setId(PROGRAM_ID);
		when(programRepository.findById(PROGRAM_ID)).thenReturn(Optional.of(program));

		assertThat(programRepository.getTrainingProgramOrThrow(PROGRAM_ID)).isSameAs(program);
	}

	@Test
	void throwsNotFoundWhenTheProgramDoesNotExist() {
		when(programRepository.findById(PROGRAM_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> programRepository.getTrainingProgramOrThrow(PROGRAM_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training program not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");
	}
}
