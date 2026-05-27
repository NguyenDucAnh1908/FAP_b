package com.fap.clazz.dto;

import jakarta.validation.constraints.NotNull;

public record ClassTrainerItemRequest(
		@NotNull Long userId,
		Long syllabusId
) {
}
