package com.fap.clazz.enums;

import com.fap.common.util.TransitionalStatus;

import java.util.Set;

public enum ClassStatus implements TransitionalStatus<ClassStatus> {
	Planning,
	Active,
	Closed;

	@Override
	public Set<ClassStatus> allowedTargets() {
		return switch (this) {
			case Planning -> Set.of(Active);
			case Active -> Set.of(Closed);
			case Closed -> Set.of();
		};
	}
}
