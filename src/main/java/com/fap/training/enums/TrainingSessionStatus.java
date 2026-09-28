package com.fap.training.enums;

import com.fap.common.util.TransitionalStatus;

import java.util.Set;

public enum TrainingSessionStatus implements TransitionalStatus<TrainingSessionStatus> {
	Upcoming,
	Completed,
	Canceled;

	@Override
	public Set<TrainingSessionStatus> allowedTargets() {
		// A switch rather than constant-specific bodies, which would make a constant's getClass()
		// differ from the enum class; the exhaustive switch still fails compilation on a new status.
		return switch (this) {
			case Upcoming -> Set.of(Completed, Canceled);
			case Completed -> Set.of();
			case Canceled -> Set.of();
		};
	}
}
