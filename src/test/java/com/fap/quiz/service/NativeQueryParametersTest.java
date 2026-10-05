package com.fap.quiz.service;

import com.fap.quiz.enums.QuestionType;
import com.fap.quiz.enums.QuizStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NativeQueryParametersTest {

	@Test
	void bindsTheConstantNameStoredInTheColumn() {
		assertThat(NativeQueryParameters.enumName(QuizStatus.Published)).isEqualTo("Published");
		assertThat(NativeQueryParameters.enumName(QuestionType.single)).isEqualTo("single");
	}

	@Test
	void missingFilterStaysNullSoTheQueryIgnoresIt() {
		assertThat(NativeQueryParameters.enumName(null)).isNull();
	}
}
