package com.fap.role.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdatePermissionMatrixRequest(
		@NotEmpty List<@Valid UpdatePermissionRequest> permissions
) {
}
