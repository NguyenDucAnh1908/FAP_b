package com.fap.common.security;

import java.util.Set;

public record CurrentUser(
		Long id,
		String email,
		Set<String> roles
) {
}
