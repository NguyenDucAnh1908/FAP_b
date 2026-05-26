package com.fap.clazz.dto;

import com.fap.clazz.enums.ClassStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateClassStatusRequest(
		@NotNull ClassStatus status
) {
}
