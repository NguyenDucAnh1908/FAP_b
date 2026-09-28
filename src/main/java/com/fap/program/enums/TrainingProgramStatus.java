package com.fap.program.enums;

import com.fap.common.util.TransitionalStatus;

import java.util.Set;

public enum TrainingProgramStatus implements TransitionalStatus<TrainingProgramStatus> {
	Planning,
	Active,
	Inactive;

	@Override
	public Set<TrainingProgramStatus> allowedTargets() {
		// A switch rather than constant-specific bodies, which would make a constant's getClass()
		// differ from the enum class; the exhaustive switch still fails compilation on a new status.
		return switch (this) {
			case Planning -> Set.of(Active, Inactive);
			case Active -> Set.of(Inactive);
			case Inactive -> Set.of();
		};
	}
}
