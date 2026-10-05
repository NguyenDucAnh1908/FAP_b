package com.fap.quiz.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the transition table that QuizService enforces through StatusTransitions, so a change to
 * the lifecycle shows up here rather than only in the service tests.
 */
class QuizStatusTest {

	@Test
	void draftCanOnlyBePublished() {
		assertThat(QuizStatus.Draft.allowedTargets()).containsExactly(QuizStatus.Published);
	}

	@Test
	void publishedCanOnlyBeClosed() {
		assertThat(QuizStatus.Published.allowedTargets()).containsExactly(QuizStatus.Closed);
	}

	@Test
	void closedIsTerminal() {
		assertThat(QuizStatus.Closed.allowedTargets()).isEmpty();
	}

	@ParameterizedTest(name = "{0} -> {1} is allowed")
	@CsvSource({
			"Draft, Published",
			"Published, Closed"
	})
	void allowsListedTransition(QuizStatus current, QuizStatus target) {
		assertThat(current.canTransitionTo(target)).isTrue();
	}

	@ParameterizedTest(name = "{0} -> {1} is rejected")
	@CsvSource({
			"Draft, Closed",
			"Published, Draft",
			"Closed, Draft",
			"Closed, Published"
	})
	void rejectsUnlistedTransition(QuizStatus current, QuizStatus target) {
		assertThat(current.canTransitionTo(target)).isFalse();
	}

	@ParameterizedTest(name = "{0} -> {0} is allowed")
	@EnumSource(QuizStatus.class)
	void allowsStayingInTheSameStatus(QuizStatus status) {
		assertThat(status.canTransitionTo(status)).isTrue();
		assertThat(status.allowedTargets()).doesNotContain(status);
	}

	@ParameterizedTest(name = "{0} -> null is rejected")
	@EnumSource(QuizStatus.class)
	void rejectsNullTarget(QuizStatus status) {
		assertThat(status.canTransitionTo(null)).isFalse();
	}
}
