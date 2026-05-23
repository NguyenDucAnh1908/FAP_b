package com.fap.role.dto;

import com.fap.role.enums.PermissionLevel;

public record PermissionResponse(
		Long roleId,
		String roleName,
		String resource,
		PermissionLevel permissionLevel
) {
}
