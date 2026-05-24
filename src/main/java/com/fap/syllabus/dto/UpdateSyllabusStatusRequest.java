package com.fap.syllabus.dto;

import com.fap.syllabus.enums.SyllabusStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSyllabusStatusRequest(
		@NotNull SyllabusStatus status
) {
}
