package com.fap.program.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the transition table that TrainingProgramService enforces through StatusTransitions, so a
 * change to the lifecycle shows up here rather than only in the service tests.
 */
class TrainingProgramStatusTest {

	@Test
	void planningCanBePublishedOrDeactivated() {
		assertThat(TrainingProgramStatus.Planning.allowedTargets())
				.containsExactlyInAnyOrder(TrainingProgramStatus.Active, TrainingProgramStatus.Inactive);
	}

	@Test
	void activeCanOnlyBeDeactivated() {
		assertThat(TrainingProgramStatus.Active.allowedTargets())
				.containsExactly(TrainingProgramStatus.Inactive);
	}

	@Test
	void inactiveIsTerminal() {
		assertThat(TrainingProgramStatus.Inactive.allowedTargets()).isEmpty();
	}

	@ParameterizedTest(name = "{0} -> {1} is allowed")
	@CsvSource({
			"Planning, Active",
			"Planning, Inactive",
			"Active, Inactive"
	})
	void allowsListedTransition(TrainingProgramStatus current, TrainingProgramStatus target) {
		assertThat(current.canTransitionTo(target)).isTrue();
	}

	@ParameterizedTest(name = "{0} -> {1} is rejected")
	@CsvSource({
			"Active, Planning",
			"Inactive, Planning",
			"Inactive, Active"
	})
	void rejectsUnlistedTransition(TrainingProgramStatus current, TrainingProgramStatus target) {
		assertThat(current.canTransitionTo(target)).isFalse();
	}

	@ParameterizedTest(name = "{0} -> {0} is allowed")
	@EnumSource(TrainingProgramStatus.class)
	void allowsStayingInTheSameStatus(TrainingProgramStatus status) {
		assertThat(status.canTransitionTo(status)).isTrue();
		assertThat(status.allowedTargets()).doesNotContain(status);
	}

	@ParameterizedTest(name = "{0} -> null is rejected")
	@EnumSource(TrainingProgramStatus.class)
	void rejectsNullTarget(TrainingProgramStatus status) {
		assertThat(status.canTransitionTo(null)).isFalse();
	}
}
