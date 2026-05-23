package com.fap.role.dto;

import com.fap.role.enums.PermissionLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePermissionRequest(
		@NotNull Long roleId,
		@NotBlank String resource,
		@NotNull PermissionLevel permissionLevel
) {
}
