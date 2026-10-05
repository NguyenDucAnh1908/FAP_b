package com.fap.training.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.training.entity.TrainingSession;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrainingSessionRepositoryLookupTest {

	private static final long SESSION_ID = 41L;

	private final TrainingSessionRepository trainingSessionRepository = mock(TrainingSessionRepository.class);

	TrainingSessionRepositoryLookupTest() {
		doCallRealMethod().when(trainingSessionRepository).getWithClassAndTrainerOrThrow(any());
		doCallRealMethod().when(trainingSessionRepository).getWithClassAndTrainerForUpdateOrThrow(any());
	}

	@Test
	void returnsTheSessionWithClassAndTrainerWhenItExists() {
		TrainingSession session = session();
		when(trainingSessionRepository.findWithClassAndTrainerById(SESSION_ID)).thenReturn(Optional.of(session));

		assertThat(trainingSessionRepository.getWithClassAndTrainerOrThrow(SESSION_ID)).isSameAs(session);
		verify(trainingSessionRepository, never()).findWithClassAndTrainerByIdForUpdate(any());
	}

	@Test
	void throwsNotFoundWhenTheSessionDoesNotExist() {
		when(trainingSessionRepository.findWithClassAndTrainerById(SESSION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> trainingSessionRepository.getWithClassAndTrainerOrThrow(SESSION_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training session not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");
	}

	/** Registration changes seat counts, so its lookup must go through the pessimistic-lock finder. */
	@Test
	void lockingLookupReturnsTheSessionFromTheLockingFinder() {
		TrainingSession session = session();
		when(trainingSessionRepository.findWithClassAndTrainerByIdForUpdate(SESSION_ID))
				.thenReturn(Optional.of(session));

		assertThat(trainingSessionRepository.getWithClassAndTrainerForUpdateOrThrow(SESSION_ID)).isSameAs(session);
		verify(trainingSessionRepository, never()).findWithClassAndTrainerById(any());
	}

	@Test
	void lockingLookupThrowsNotFoundWhenTheSessionDoesNotExist() {
		when(trainingSessionRepository.findWithClassAndTrainerByIdForUpdate(SESSION_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> trainingSessionRepository.getWithClassAndTrainerForUpdateOrThrow(SESSION_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training session not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");
	}

	private TrainingSession session() {
		TrainingSession session = new TrainingSession();
		session.setId(SESSION_ID);
		return session;
	}
}
