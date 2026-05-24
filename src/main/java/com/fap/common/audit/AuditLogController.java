package com.fap.common.audit;

import com.fap.common.api.PageResponse;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

	private final AuditLogService auditLogService;

	public AuditLogController(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@GetMapping
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'user', 'full_access')")
	public PageResponse<AuditLogResponse> list(
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) String entityType,
			@RequestParam(required = false) Long entityId,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) int limit) {
		Page<AuditLogResponse> result = auditLogService.search(userId, entityType, entityId, page - 1, limit);
		return PageResponse.of(result.getContent(), page, limit, result.getTotalElements());
	}
}
