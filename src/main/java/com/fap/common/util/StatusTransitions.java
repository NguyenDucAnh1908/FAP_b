package com.fap.common.util;

import com.fap.common.exception.ConflictException;

/**
 * Shared guard for {@link TransitionalStatus} lifecycles. Guards specific to one transition (for
 * example "a quiz needs questions before publishing") stay in the owning service, after this check.
 */
public final class StatusTransitions {

	private StatusTransitions() {
	}

	/**
	 * Returns when {@code target} equals {@code current} (a no-op) or is an allowed next status;
	 * otherwise throws {@link ConflictException} with the given code and message. A {@code null}
	 * target is rejected rather than raising a NullPointerException.
	 */
	public static <S extends Enum<S> & TransitionalStatus<S>> void requireAllowed(
			S current,
			S target,
			String code,
			String message) {
		if (current == target) {
			return;
		}
		if (current == null || !current.canTransitionTo(target)) {
			throw new ConflictException(code, message);
		}
	}
}
