package com.fap.settings.dto;

import java.util.Map;

public record SettingsResponse(
		Map<String, Map<String, String>> settings
) {
}

