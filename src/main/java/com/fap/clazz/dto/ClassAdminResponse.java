package com.fap.clazz.dto;

public record ClassAdminResponse(
		Long userId,
		String userFullName,
		String userEmail
) {
}
