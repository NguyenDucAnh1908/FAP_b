package com.fap.training.repository;

import com.fap.common.exception.NotFoundException;
import com.fap.training.entity.TrainingRegistration;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrainingRegistrationRepositoryLookupTest {

	private static final long SESSION_ID = 61L;
	private static final long USER_ID = 500L;

	private final TrainingRegistrationRepository trainingRegistrationRepository =
			mock(TrainingRegistrationRepository.class);

	TrainingRegistrationRepositoryLookupTest() {
		doCallRealMethod().when(trainingRegistrationRepository).getByTrainingSessionIdAndUserIdOrThrow(any(), any());
	}

	@Test
	void returnsTheRegistrationWhenItExists() {
		TrainingRegistration registration = new TrainingRegistration();
		registration.setId(400L);
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.of(registration));

		assertThat(trainingRegistrationRepository.getByTrainingSessionIdAndUserIdOrThrow(SESSION_ID, USER_ID))
				.isSameAs(registration);
	}

	@Test
	void throwsNotFoundWhenTheRegistrationDoesNotExist() {
		when(trainingRegistrationRepository.findByTrainingSessionIdAndUserId(SESSION_ID, USER_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> trainingRegistrationRepository.getByTrainingSessionIdAndUserIdOrThrow(SESSION_ID, USER_ID))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("Training registration not found")
				.extracting("code")
				.isEqualTo("RESOURCE_NOT_FOUND");
	}
}
