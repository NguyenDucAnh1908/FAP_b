package com.fap.syllabus.service;

import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusOutputStandard;
import com.fap.syllabus.enums.SyllabusStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyllabusRulesTest {

	@ParameterizedTest
	@EnumSource(value = SyllabusStatus.class, names = {"Drafting", "Pending"})
	void draftingAndPendingSyllabusesAreEditable(SyllabusStatus status) {
		assertThatCode(() -> SyllabusRules.ensureEditable(syllabus(status))).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(value = SyllabusStatus.class, names = {"Active", "Inactive"})
	void activeAndInactiveSyllabusesAreNotEditable(SyllabusStatus status) {
		assertThatThrownBy(() -> SyllabusRules.ensureEditable(syllabus(status)))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Only Drafting or Pending syllabus can be edited")
				.extracting("code")
				.isEqualTo("SYLLABUS_NOT_EDITABLE");
	}

	@Test
	void acceptsTotalsOfExactlyOneHundred() {
		assertThatCode(() -> SyllabusRules.validatePercentTotals(40, 30, 20, 10, 20, 30, 50))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsTimeAllocationNotTotallingOneHundred() {
		assertThatThrownBy(() -> SyllabusRules.validatePercentTotals(40, 30, 20, 9, 20, 30, 50))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Syllabus time allocation total must be 100")
				.extracting("code")
				.isEqualTo("INVALID_SYLLABUS_TIME_ALLOCATION");
	}

	@Test
	void rejectsAssessmentNotTotallingOneHundred() {
		assertThatThrownBy(() -> SyllabusRules.validatePercentTotals(40, 30, 20, 10, 20, 30, 51))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Syllabus assessment total must be 100")
				.extracting("code")
				.isEqualTo("INVALID_SYLLABUS_ASSESSMENT");
	}

	@Test
	void checksTimeAllocationBeforeAssessment() {
		assertThatThrownBy(() -> SyllabusRules.validatePercentTotals(0, 0, 0, 0, 0, 0, 0))
				.extracting("code")
				.isEqualTo("INVALID_SYLLABUS_TIME_ALLOCATION");
	}

	@Test
	void outputStandardIsKeyedBySyllabusIdAndCode() {
		Syllabus syllabus = syllabus(SyllabusStatus.Drafting);

		SyllabusOutputStandard standard = SyllabusRules.createOutputStandard(syllabus, "H4SD");

		assertThat(standard.getSyllabus()).isSameAs(syllabus);
		assertThat(standard.getId().getSyllabusId()).isEqualTo(12L);
		assertThat(standard.getStandardCode()).isEqualTo("H4SD");
	}

	private static Syllabus syllabus(SyllabusStatus status) {
		Syllabus syllabus = new Syllabus();
		syllabus.setId(12L);
		syllabus.setStatus(status);
		return syllabus;
	}
}
