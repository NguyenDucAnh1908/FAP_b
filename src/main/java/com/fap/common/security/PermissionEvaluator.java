package com.fap.common.security;

import com.fap.role.entity.Permission;
import com.fap.role.repository.PermissionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component("permissionEvaluator")
public class PermissionEvaluator {

	private final PermissionRepository permissionRepository;

	public PermissionEvaluator(PermissionRepository permissionRepository) {
		this.permissionRepository = permissionRepository;
	}

	public boolean hasAction(Authentication authentication, String resource, String action) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return false;
		}
		if (!(authentication.getPrincipal() instanceof FapUserPrincipal principal)) {
			return false;
		}
		if (principal.roles().contains("Super Admin")) {
			return true;
		}

		Set<Long> roleIds = principal.authorities().stream()
				.map(authority -> authority.getAuthority())
				.filter(authority -> authority.startsWith("ROLE_ID_"))
				.map(authority -> Long.parseLong(authority.substring("ROLE_ID_".length())))
				.collect(Collectors.toSet());
		if (roleIds.isEmpty()) {
			return false;
		}

		return permissionRepository.findByRoleIdIn(roleIds).stream()
				.filter(permission -> permission.getResource().equals(resource))
				.map(Permission::getPermissionLevel)
				.anyMatch(level -> level.allows(action));
	}
}
