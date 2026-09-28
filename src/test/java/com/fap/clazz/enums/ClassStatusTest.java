package com.fap.clazz.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ClassStatusTest {

	@Test
	void planningCanOnlyBeActivated() {
		assertThat(ClassStatus.Planning.allowedTargets()).containsExactly(ClassStatus.Active);
	}

	@Test
	void activeCanOnlyBeClosed() {
		assertThat(ClassStatus.Active.allowedTargets()).containsExactly(ClassStatus.Closed);
	}

	@Test
	void closedIsTerminal() {
		assertThat(ClassStatus.Closed.allowedTargets()).isEmpty();
	}

	@ParameterizedTest
	@EnumSource(ClassStatus.class)
	void stayingInTheSameStatusIsAllowedButNotListed(ClassStatus status) {
		assertThat(status.allowedTargets()).doesNotContain(status);
		assertThat(status.canTransitionTo(status)).isTrue();
		assertThat(status.canTransitionTo(null)).isFalse();
	}

	/** A constant-specific body would make getClass() differ from the enum type (JPA, Jackson). */
	@ParameterizedTest
	@EnumSource(ClassStatus.class)
	void constantsHaveNoClassBody(ClassStatus status) {
		assertThat(status.getClass()).isEqualTo(ClassStatus.class);
	}
}
