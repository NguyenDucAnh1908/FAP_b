package com.fap.syllabus.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class SyllabusStatusTest {

	@Test
	void draftingMaySubmitOrRetire() {
		assertThat(SyllabusStatus.Drafting.allowedTargets())
				.containsExactlyInAnyOrder(SyllabusStatus.Pending, SyllabusStatus.Inactive);
	}

	@Test
	void pendingMayPublishOrRetire() {
		assertThat(SyllabusStatus.Pending.allowedTargets())
				.containsExactlyInAnyOrder(SyllabusStatus.Active, SyllabusStatus.Inactive);
	}

	@ParameterizedTest
	@EnumSource(value = SyllabusStatus.class, names = {"Active", "Inactive"})
	void publishedAndRetiredAreTerminal(SyllabusStatus status) {
		assertThat(status.allowedTargets()).isEmpty();
	}

	@ParameterizedTest(name = "{0} -> {1} is allowed")
	@CsvSource({
			"Drafting, Pending",
			"Drafting, Inactive",
			"Pending, Active",
			"Pending, Inactive"
	})
	void allowsListedTransitions(SyllabusStatus current, SyllabusStatus target) {
		assertThat(current.canTransitionTo(target)).isTrue();
	}

	@ParameterizedTest(name = "{0} -> {1} is rejected")
	@CsvSource({
			"Drafting, Active",
			"Pending, Drafting",
			"Active, Drafting",
			"Active, Pending",
			"Active, Inactive",
			"Inactive, Drafting",
			"Inactive, Pending",
			"Inactive, Active"
	})
	void rejectsUnlistedTransitions(SyllabusStatus current, SyllabusStatus target) {
		assertThat(current.canTransitionTo(target)).isFalse();
	}

	@ParameterizedTest
	@EnumSource(SyllabusStatus.class)
	void staysInTheSameStatusButNeverListsItself(SyllabusStatus status) {
		assertThat(status.canTransitionTo(status)).isTrue();
		assertThat(status.allowedTargets()).doesNotContain(status);
	}

	@ParameterizedTest
	@EnumSource(SyllabusStatus.class)
	void rejectsMissingTarget(SyllabusStatus status) {
		assertThat(status.canTransitionTo(null)).isFalse();
	}
}
