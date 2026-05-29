package com.fap.training.dto;

import com.fap.clazz.dto.ClassResponse;

import java.util.List;

public record MyClassDetailResponse(
		ClassResponse classInfo,
		List<MyClassSyllabusResponse> syllabuses
) {
}
