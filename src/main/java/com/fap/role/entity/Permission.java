package com.fap.role.entity;

import com.fap.role.enums.PermissionLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
		name = "permissions",
		uniqueConstraints = @UniqueConstraint(name = "uk_permissions_role_res", columnNames = {"role_id", "resource_name"}))
public class Permission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	@Column(name = "resource_name", nullable = false, length = 50)
	private String resource;

	@Enumerated(EnumType.STRING)
	@Column(name = "permission_level", nullable = false, length = 30)
	private PermissionLevel permissionLevel;
}
