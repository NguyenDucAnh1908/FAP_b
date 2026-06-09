package com.fap.syllabus.mapper;

import com.fap.syllabus.dto.FullSyllabusDayResponse;
import com.fap.syllabus.dto.FullSyllabusTopicResponse;
import com.fap.syllabus.dto.FullSyllabusUnitResponse;
import com.fap.syllabus.dto.SyllabusDayResponse;
import com.fap.syllabus.dto.SyllabusTopicResponse;
import com.fap.syllabus.dto.SyllabusUnitResponse;
import com.fap.syllabus.entity.SyllabusDay;
import com.fap.syllabus.entity.SyllabusTopic;
import com.fap.syllabus.entity.SyllabusUnit;
import org.springframework.stereotype.Component;

@Component
public class SyllabusOutlineMapper {

	private final MaterialFileMapper materialFileMapper;

	public SyllabusOutlineMapper(MaterialFileMapper materialFileMapper) {
		this.materialFileMapper = materialFileMapper;
	}

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

	public FullSyllabusDayResponse toFullResponse(SyllabusDay day) {
		return new FullSyllabusDayResponse(
				day.getId(),
				day.getDayNumber(),
				day.getSortOrder(),
				day.getUnits().stream().map(this::toFullResponse).toList());
	}

	public FullSyllabusUnitResponse toFullResponse(SyllabusUnit unit) {
		return new FullSyllabusUnitResponse(
				unit.getId(),
				unit.getName(),
				unit.getSortOrder(),
				unit.getTopics().stream().map(this::toFullResponse).toList());
	}

	public FullSyllabusTopicResponse toFullResponse(SyllabusTopic topic) {
		return new FullSyllabusTopicResponse(
				topic.getId(),
				topic.getName(),
				topic.getOutputStandard(),
				topic.isOnline(),
				topic.getDurationMinutes(),
				topic.getStatus(),
				topic.getSortOrder(),
				topic.getMaterials().stream().map(materialFileMapper::toResponse).toList());
	}
}
