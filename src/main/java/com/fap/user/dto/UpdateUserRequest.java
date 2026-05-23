package com.fap.user.dto;

import com.fap.user.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record UpdateUserRequest(
		@NotBlank @Size(max = 255) String fullName,
		@NotBlank @Email @Size(max = 255) String email,
		@Size(max = 20) String phone,
		LocalDate dateOfBirth,
		@NotNull Gender gender,
		@Size(max = 512) String avatarUrl,
		@NotEmpty Set<Long> roleIds
) {
}
