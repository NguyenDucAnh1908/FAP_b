package com.fap.user.dto;

import com.fap.role.dto.RoleResponse;
import com.fap.user.enums.Gender;
import com.fap.user.enums.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
		Long id,
		String fullName,
		String email,
		String phone,
		LocalDate dateOfBirth,
		Gender gender,
		String avatarUrl,
		UserStatus status,
		List<RoleResponse> roles,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
