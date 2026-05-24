package com.fap.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_logs_seq")
	@SequenceGenerator(name = "audit_logs_seq", sequenceName = "audit_logs_seq", allocationSize = 1)
	private Long id;

	@Column(name = "user_id")
	private Long userId;

	@Lob
	@Column(nullable = false)
	private String action;

	@Column(name = "entity_type", length = 50)
	private String entityType;

	@Column(name = "entity_id")
	private Long entityId;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}
