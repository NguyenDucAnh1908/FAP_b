package com.fap.syllabus.enums;

import com.fap.common.util.TransitionalStatus;

import java.util.Set;

public enum SyllabusStatus implements TransitionalStatus<SyllabusStatus> {
	Drafting,
	Pending,
	Active,
	Inactive;

	/**
	 * The content of a published syllabus is never edited in place (changes go through a cloned
	 * Drafting version), but it can still be retired, so Active may move to Inactive. Only Inactive
	 * is terminal.
	 */
	@Override
	public Set<SyllabusStatus> allowedTargets() {
		return switch (this) {
			case Drafting -> Set.of(Pending, Inactive);
			case Pending -> Set.of(Active, Inactive);
			case Active -> Set.of(Inactive);
			case Inactive -> Set.of();
		};
	}
}
