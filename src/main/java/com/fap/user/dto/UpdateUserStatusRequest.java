package com.fap.user.dto;

import com.fap.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
		@NotNull UserStatus status
) {
}
