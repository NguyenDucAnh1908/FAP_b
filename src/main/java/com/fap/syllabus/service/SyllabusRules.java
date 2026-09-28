package com.fap.syllabus.service;

import com.fap.common.exception.BadRequestException;
import com.fap.common.exception.ConflictException;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusOutputStandard;
import com.fap.syllabus.entity.SyllabusOutputStandardId;
import com.fap.syllabus.enums.SyllabusStatus;

/**
 * Rules shared by the syllabus services. Static rather than a bean because none of them needs a
 * dependency, which also keeps every service constructor unchanged.
 */
final class SyllabusRules {

	private SyllabusRules() {
	}

	static void ensureEditable(Syllabus syllabus) {
		if (syllabus.getStatus() == SyllabusStatus.Active || syllabus.getStatus() == SyllabusStatus.Inactive) {
			throw new ConflictException("SYLLABUS_NOT_EDITABLE", "Only Drafting or Pending syllabus can be edited");
		}
	}

	static void validatePercentTotals(
			Integer assignmentLab,
			Integer conceptLecture,
			Integer guideReview,
			Integer testQuiz,
			Integer quiz,
			Integer assignment,
			Integer finalAssessment) {
		if (assignmentLab + conceptLecture + guideReview + testQuiz != 100) {
			throw new BadRequestException("INVALID_SYLLABUS_TIME_ALLOCATION", "Syllabus time allocation total must be 100");
		}
		if (quiz + assignment + finalAssessment != 100) {
			throw new BadRequestException("INVALID_SYLLABUS_ASSESSMENT", "Syllabus assessment total must be 100");
		}
	}

	static SyllabusOutputStandard createOutputStandard(Syllabus syllabus, String standardCode) {
		SyllabusOutputStandard outputStandard = new SyllabusOutputStandard();
		outputStandard.setSyllabus(syllabus);
		outputStandard.setId(new SyllabusOutputStandardId(syllabus.getId(), standardCode));
		return outputStandard;
	}
}
