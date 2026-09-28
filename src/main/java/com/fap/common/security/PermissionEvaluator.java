package com.fap.common.security;

import com.fap.role.enums.PermissionLevel;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component("permissionEvaluator")
public class PermissionEvaluator {

	private final AuthorizationCache authorizationCache;

	public PermissionEvaluator(AuthorizationCache authorizationCache) {
		this.authorizationCache = authorizationCache;
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
		if (principal.roles().contains(RoleNames.SUPER_ADMIN)) {
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

		return authorizationCache.permissionsByRole(roleIds).values().stream()
				.map(levels -> levels.get(resource))
				.filter(Objects::nonNull)
				.anyMatch(permissionMatcher);
	}
}
