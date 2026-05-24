package com.fap.common.audit;

import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

	public AuditLogResponse toResponse(AuditLog auditLog) {
		return new AuditLogResponse(
				auditLog.getId(),
				auditLog.getUserId(),
				auditLog.getAction(),
				auditLog.getEntityType(),
				auditLog.getEntityId(),
				auditLog.getIpAddress(),
				auditLog.getCreatedAt());
	}
}
