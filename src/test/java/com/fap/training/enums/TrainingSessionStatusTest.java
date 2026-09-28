package com.fap.training.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the transition table that TrainingSessionService enforces through StatusTransitions, so a
 * change to the lifecycle shows up here rather than only in the service tests.
 */
class TrainingSessionStatusTest {

	@Test
	void upcomingCanBeCompletedOrCanceled() {
		assertThat(TrainingSessionStatus.Upcoming.allowedTargets())
				.containsExactlyInAnyOrder(TrainingSessionStatus.Completed, TrainingSessionStatus.Canceled);
	}

	@ParameterizedTest(name = "{0} is terminal")
	@EnumSource(value = TrainingSessionStatus.class, names = {"Completed", "Canceled"})
	void completedAndCanceledAreTerminal(TrainingSessionStatus status) {
		assertThat(status.allowedTargets()).isEmpty();
	}

	@ParameterizedTest(name = "{0} -> {1} is allowed")
	@CsvSource({
			"Upcoming, Completed",
			"Upcoming, Canceled"
	})
	void allowsListedTransition(TrainingSessionStatus current, TrainingSessionStatus target) {
		assertThat(current.canTransitionTo(target)).isTrue();
	}

	@ParameterizedTest(name = "{0} -> {1} is rejected")
	@CsvSource({
			"Completed, Upcoming",
			"Completed, Canceled",
			"Canceled, Upcoming",
			"Canceled, Completed"
	})
	void rejectsUnlistedTransition(TrainingSessionStatus current, TrainingSessionStatus target) {
		assertThat(current.canTransitionTo(target)).isFalse();
	}

	@ParameterizedTest(name = "{0} -> {0} is allowed")
	@EnumSource(TrainingSessionStatus.class)
	void allowsStayingInTheSameStatus(TrainingSessionStatus status) {
		assertThat(status.canTransitionTo(status)).isTrue();
		assertThat(status.allowedTargets()).doesNotContain(status);
	}

	@ParameterizedTest(name = "{0} -> null is rejected")
	@EnumSource(TrainingSessionStatus.class)
	void rejectsNullTarget(TrainingSessionStatus status) {
		assertThat(status.canTransitionTo(null)).isFalse();
	}

	/** Training analytics iterate {@code values()}, so the declaration order reaches the API output. */
	@Test
	void keepsDeclarationOrder() {
		assertThat(TrainingSessionStatus.values()).containsExactly(
				TrainingSessionStatus.Upcoming,
				TrainingSessionStatus.Completed,
				TrainingSessionStatus.Canceled);
	}
}
