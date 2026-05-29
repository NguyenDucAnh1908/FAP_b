package com.fap.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record UpdateSettingsRequest(
		@NotNull(message = "Settings map is required")
		Map<String, Map<String, @NotBlank String>> settings
) {
}

