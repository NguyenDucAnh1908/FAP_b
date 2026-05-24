package com.fap.common.audit;

import com.fap.common.security.FapUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;
	private final AuditLogMapper auditLogMapper;

	public AuditLogService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
		this.auditLogRepository = auditLogRepository;
		this.auditLogMapper = auditLogMapper;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(String action, String entityType, Long entityId) {
		AuditLog auditLog = new AuditLog();
		auditLog.setUserId(currentUserId());
		auditLog.setAction(action);
		auditLog.setEntityType(entityType);
		auditLog.setEntityId(entityId);
		auditLog.setIpAddress(currentIpAddress());
		auditLog.setCreatedAt(LocalDateTime.now());
		auditLogRepository.save(auditLog);
	}

	@Transactional(readOnly = true)
	public Page<AuditLogResponse> search(Long userId, String entityType, Long entityId, int page, int limit) {
		PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
		return auditLogRepository.search(userId, normalizeBlank(entityType), entityId, pageRequest)
				.map(auditLogMapper::toResponse);
	}

	private Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof FapUserPrincipal principal)) {
			return null;
		}
		return principal.id();
	}

	private String currentIpAddress() {
		if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
			return null;
		}
		HttpServletRequest request = attributes.getRequest();
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}

	private String normalizeBlank(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
