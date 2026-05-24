package com.fap.syllabus.mapper;

import com.fap.syllabus.dto.SyllabusResponse;
import com.fap.syllabus.entity.Syllabus;
import org.springframework.stereotype.Component;

@Component
public class SyllabusMapper {

	public SyllabusResponse toResponse(Syllabus syllabus) {
		return new SyllabusResponse(
				syllabus.getId(),
				syllabus.getName(),
				syllabus.getCode(),
				syllabus.getVersion(),
				syllabus.getStatus(),
				syllabus.getLevelName(),
				syllabus.getAttendees(),
				syllabus.getDuration(),
				syllabus.getTechnicalRequirements(),
				syllabus.getCourseObjectives(),
				syllabus.getRules(),
				syllabus.getTimeAllocAssignmentLab(),
				syllabus.getTimeAllocConceptLecture(),
				syllabus.getTimeAllocGuideReview(),
				syllabus.getTimeAllocTestQuiz(),
				syllabus.getAssessQuizPct(),
				syllabus.getAssessAssignmentPct(),
				syllabus.getAssessFinalPct(),
				syllabus.getAssessmentText(),
				syllabus.getCreatedBy(),
				syllabus.getUpdatedBy(),
				syllabus.getCreatedAt(),
				syllabus.getUpdatedAt());
	}
}
