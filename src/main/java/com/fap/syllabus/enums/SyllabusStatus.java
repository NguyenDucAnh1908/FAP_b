package com.fap.syllabus.enums;

import com.fap.common.util.TransitionalStatus;

import java.util.Set;

public enum SyllabusStatus implements TransitionalStatus<SyllabusStatus> {
	Drafting,
	Pending,
	Active,
	Inactive;

	/**
	 * Active and Inactive are terminal: a published syllabus is never edited in place, it is cloned
	 * as a new Drafting version instead.
	 */
	@Override
	public Set<SyllabusStatus> allowedTargets() {
		return switch (this) {
			case Drafting -> Set.of(Pending, Inactive);
			case Pending -> Set.of(Active, Inactive);
			case Active, Inactive -> Set.of();
		};
	}
}
