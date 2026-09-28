package com.fap.quiz.enums;

import com.fap.common.util.TransitionalStatus;

import java.util.Set;

public enum QuizStatus implements TransitionalStatus<QuizStatus> {
	Draft,
	Published,
	Closed;

	@Override
	public Set<QuizStatus> allowedTargets() {
		// A switch rather than constant-specific bodies, which would make a constant's getClass()
		// differ from the enum class; the exhaustive switch still fails compilation on a new status.
		return switch (this) {
			case Draft -> Set.of(Published);
			case Published -> Set.of(Closed);
			case Closed -> Set.of();
		};
	}
}
