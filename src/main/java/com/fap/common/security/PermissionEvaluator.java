package com.fap.common.security;

import com.fap.role.entity.Permission;
import com.fap.role.enums.PermissionLevel;
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
		return hasAllowedPermission(authentication, resource, level -> level.allows(action));
	}

	public boolean hasPermission(Authentication authentication, String resource, String requiredLevel) {
		PermissionLevel permissionLevel;
		try {
			permissionLevel = PermissionLevel.valueOf(requiredLevel);
		} catch (IllegalArgumentException exception) {
			return false;
		}
		return hasAllowedPermission(authentication, resource, level -> level.allows(permissionLevel));
	}

	private boolean hasAllowedPermission(
			Authentication authentication,
			String resource,
			java.util.function.Predicate<PermissionLevel> permissionMatcher) {
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
				.anyMatch(permissionMatcher);
	}
}
