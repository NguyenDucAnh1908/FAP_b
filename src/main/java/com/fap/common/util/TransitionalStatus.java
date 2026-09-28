package com.fap.common.util;

import java.util.Set;

/**
 * A status enum whose lifecycle is a fixed one-step transition table. Staying in the same status is
 * a no-op that is always allowed and is not listed in {@link #allowedTargets()}.
 *
 * <p>Only for lifecycles changed through a single status endpoint. Statuses whose legal moves
 * depend on the operation (enrollment, registration, attempts) keep their per-operation checks.
 */
public interface TransitionalStatus<S extends Enum<S> & TransitionalStatus<S>> {

	/** Statuses reachable from this one in a single step, excluding this status itself. */
	Set<S> allowedTargets();

	default boolean canTransitionTo(S target) {
		return target == this || (target != null && allowedTargets().contains(target));
	}
}
