package com.fap.quiz.service;

/**
 * Parameters for the native quiz and question search queries. Those bypass JPA's enum mapping and
 * compare the {@code EnumType.STRING} columns directly, so enum filters are bound by constant name.
 */
final class NativeQueryParameters {

	private NativeQueryParameters() {
	}

	/** The constant name, or {@code null} so the query's {@code :param is null} branch skips the filter. */
	static String enumName(Enum<?> value) {
		return value == null ? null : value.name();
	}
}
