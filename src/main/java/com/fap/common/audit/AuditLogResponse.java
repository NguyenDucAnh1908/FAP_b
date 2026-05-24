package com.fap.common.audit;

import java.time.LocalDateTime;

public record AuditLogResponse(
		Long id,
		Long userId,
		String action,
		String entityType,
		Long entityId,
		String ipAddress,
		LocalDateTime createdAt
) {
}
