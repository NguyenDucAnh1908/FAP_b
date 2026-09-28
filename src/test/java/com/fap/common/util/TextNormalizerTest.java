package com.fap.common.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "\t", " \n "})
	void blankBecomesNull(String value) {
		assertThat(TextNormalizer.blankToNull(value)).isNull();
	}

	@Test
	void textIsTrimmedButOtherwiseKept() {
		assertThat(TextNormalizer.blankToNull("  Java Basics ")).isEqualTo("Java Basics");
	}
}
