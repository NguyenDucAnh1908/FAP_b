package com.fap.clazz.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateClassAdminsRequest(
		@NotEmpty List<@NotNull Long> userIds
) {
}
