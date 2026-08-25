package com.fap.result.dto;

import com.fap.result.enums.CourseResultStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCourseResultRequest(
		@NotNull CourseResultStatus status,
		@NotBlank @Size(max = 1000) String reason
) {
}
