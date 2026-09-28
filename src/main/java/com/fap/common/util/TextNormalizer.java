package com.fap.common.util;

/** Normalization of optional free-text inputs (filters, keywords, optional fields). */
public final class TextNormalizer {

	private TextNormalizer() {
	}

	/** {@code null} for a missing or blank value, otherwise the value trimmed. */
	public static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
