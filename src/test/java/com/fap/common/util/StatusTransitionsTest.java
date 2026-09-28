package com.fap.common.util;

import com.fap.common.exception.ConflictException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusTransitionsTest {

	private enum Light implements TransitionalStatus<Light> {
		Red, Green, Off;

		@Override
		public Set<Light> allowedTargets() {
			return switch (this) {
				case Red -> Set.of(Green);
				case Green -> Set.of(Red, Off);
				case Off -> Set.of();
			};
		}
	}

	@Test
	void sameStatusIsAlwaysAllowed() {
		assertThatCode(() -> StatusTransitions.requireAllowed(Light.Off, Light.Off, "CODE", "message"))
				.doesNotThrowAnyException();
	}

	@Test
	void listedTargetIsAllowed() {
		assertThatCode(() -> StatusTransitions.requireAllowed(Light.Green, Light.Off, "CODE", "message"))
				.doesNotThrowAnyException();
	}

	@Test
	void unlistedTargetIsRejectedWithTheGivenCodeAndMessage() {
		assertThatThrownBy(() -> StatusTransitions.requireAllowed(Light.Red, Light.Off, "INVALID_LIGHT", "No"))
				.isInstanceOf(ConflictException.class)
				.hasMessage("No")
				.extracting("code")
				.isEqualTo("INVALID_LIGHT");
	}

	@Test
	void nullTargetIsRejectedNotNullPointer() {
		assertThatThrownBy(() -> StatusTransitions.requireAllowed(Light.Red, null, "INVALID_LIGHT", "No"))
				.isInstanceOf(ConflictException.class);
	}
}
