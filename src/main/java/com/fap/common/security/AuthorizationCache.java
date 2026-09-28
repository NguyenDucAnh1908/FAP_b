package com.fap.common.security;

import com.fap.role.entity.Permission;
import com.fap.role.enums.PermissionLevel;
import com.fap.common.util.AfterCommit;
import com.fap.role.repository.PermissionRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Per-instance cache for what every authenticated request used to read from the database: the
 * token owner's principal (user + roles) and each role's permission levels.
 *
 * <p>Writers invalidate after their transaction commits, so a concurrent request cannot re-cache
 * the old rows between the invalidation and the commit. The TTL bounds staleness for anything
 * that changes without going through those writers (manual SQL, another instance).
 */
@Component
public class AuthorizationCache {

	private static final int MAX_PRINCIPALS = 10_000;
	private static final int MAX_ROLES = 1_000;

	private final PermissionRepository permissionRepository;
	private final Cache<String, FapUserPrincipal> principals;
	private final Cache<Long, Map<String, PermissionLevel>> rolePermissions;

	public AuthorizationCache(
			PermissionRepository permissionRepository,
			@Value("${app.security.authorization-cache-ttl:60s}") Duration ttl) {
		this.permissionRepository = permissionRepository;
		this.principals = Caffeine.newBuilder()
				.maximumSize(MAX_PRINCIPALS)
				.expireAfterWrite(ttl)
				.build();
		this.rolePermissions = Caffeine.newBuilder()
				.maximumSize(MAX_ROLES)
				.expireAfterWrite(ttl)
				.build();
	}

	/**
	 * Returns the cached principal for {@code email}, loading it on a miss. A loader exception (for
	 * example an unknown user) propagates and nothing is cached.
	 */
	public FapUserPrincipal principal(String email, Function<String, FapUserPrincipal> loader) {
		return principals.get(email.toLowerCase(Locale.ROOT), loader);
	}

	/** Permission level per resource for each role; roles without permissions map to an empty map. */
	public Map<Long, Map<String, PermissionLevel>> permissionsByRole(Collection<Long> roleIds) {
		return rolePermissions.getAll(roleIds, this::loadPermissions);
	}

	public void invalidatePrincipalsAfterCommit() {
		AfterCommit.run(principals::invalidateAll);
	}

	public void invalidatePermissionsAfterCommit() {
		AfterCommit.run(rolePermissions::invalidateAll);
	}

	private Map<Long, Map<String, PermissionLevel>> loadPermissions(Set<? extends Long> roleIds) {
		Map<Long, Map<String, PermissionLevel>> byRole = new HashMap<>();
		roleIds.forEach(roleId -> byRole.put(roleId, new HashMap<>()));
		for (Permission permission : permissionRepository.findByRoleIdIn(new ArrayList<>(roleIds))) {
			byRole.computeIfAbsent(permission.getRole().getId(), id -> new HashMap<>())
					.put(permission.getResource(), permission.getPermissionLevel());
		}
		byRole.replaceAll((roleId, levels) -> Map.copyOf(levels));
		return byRole;
	}
}
