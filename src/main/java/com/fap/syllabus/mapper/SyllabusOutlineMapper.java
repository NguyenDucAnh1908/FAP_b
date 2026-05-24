package com.fap.syllabus.mapper;

import com.fap.syllabus.dto.SyllabusDayResponse;
import com.fap.syllabus.dto.SyllabusTopicResponse;
import com.fap.syllabus.dto.SyllabusUnitResponse;
import com.fap.syllabus.entity.SyllabusDay;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.entity.SyllabusUnit;
import org.springframework.stereotype.Component;

@Component
public class SyllabusOutlineMapper {

	public SyllabusDayResponse toResponse(SyllabusDay day) {
		return new SyllabusDayResponse(
				day.getId(),
				day.getDayNumber(),
				day.getSortOrder(),
				day.getUnits().stream().map(this::toResponse).toList());
	}

	public SyllabusUnitResponse toResponse(SyllabusUnit unit) {
		return new SyllabusUnitResponse(
				unit.getId(),
				unit.getName(),
				unit.getSortOrder(),
				unit.getTopics().stream().map(this::toResponse).toList());
	}

	public SyllabusTopicResponse toResponse(SyllabusTopic topic) {
		return new SyllabusTopicResponse(
				topic.getId(),
				topic.getName(),
				topic.getOutputStandard(),
				topic.isOnline(),
				topic.getDurationMinutes(),
				topic.getStatus(),
				topic.getSortOrder());
	}
}
