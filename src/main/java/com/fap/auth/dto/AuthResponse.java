package com.fap.auth.dto;

import com.fap.user.dto.UserResponse;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long expiresInSeconds,
		UserResponse user
) {
}
