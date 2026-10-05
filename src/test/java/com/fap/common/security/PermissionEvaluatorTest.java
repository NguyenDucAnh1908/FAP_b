package com.fap.common.security;

import com.fap.role.entity.Permission;
import com.fap.role.entity.Role;
import com.fap.role.enums.PermissionLevel;
import com.fap.role.repository.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PermissionEvaluatorTest {

	private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
	private final AuthorizationCache authorizationCache = new AuthorizationCache(permissionRepository, Duration.ofMinutes(1));
	private final PermissionEvaluator evaluator = new PermissionEvaluator(authorizationCache);

	@Test
	void superAdminCanPerformAnyAction() {
		FapUserPrincipal principal = new FapUserPrincipal(
				1L,
				"admin@example.com",
				"hash",
				Set.of(RoleNames.SUPER_ADMIN),
				true,
				List.of());
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				List.of());

		assertThat(evaluator.hasAction(authentication, "syllabus", "delete")).isTrue();
		assertThat(evaluator.hasPermission(authentication, "user", "view")).isTrue();
		assertThat(evaluator.hasPermission(authentication, "class", "create")).isTrue();
		assertThat(evaluator.hasPermission(authentication, "quiz", "modify")).isTrue();
		assertThat(evaluator.hasPermission(authentication, "learning_material", "full_access")).isTrue();
		verifyNoInteractions(permissionRepository);
	}

	@Test
	void evaluatesStoredLevelByAction() {
		when(permissionRepository.findByRoleIdIn(anyCollection()))
				.thenReturn(List.of(permission(10L, "user", PermissionLevel.modify)));
		FapUserPrincipal principal = new FapUserPrincipal(
				2L,
				"manager@example.com",
				"hash",
				Set.of(RoleNames.CLASS_ADMIN),
				true,
				List.of(new SimpleGrantedAuthority("ROLE_ID_10")));
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.getAuthorities());

		assertThat(evaluator.hasAction(authentication, "user", "update")).isTrue();
		assertThat(evaluator.hasAction(authentication, "user", "delete")).isFalse();
	}

	@Test
	void evaluatesStoredLevelByRequiredPermissionLevel() {
		when(permissionRepository.findByRoleIdIn(anyCollection()))
				.thenReturn(List.of(permission(10L, "user", PermissionLevel.modify)));
		FapUserPrincipal principal = new FapUserPrincipal(
				2L,
				"manager@example.com",
				"hash",
				Set.of(RoleNames.CLASS_ADMIN),
				true,
				List.of(new SimpleGrantedAuthority("ROLE_ID_10")));
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.getAuthorities());

		assertThat(evaluator.hasPermission(authentication, "user", "view")).isTrue();
		assertThat(evaluator.hasPermission(authentication, "user", "modify")).isTrue();
		assertThat(evaluator.hasPermission(authentication, "user", "create")).isFalse();
		assertThat(evaluator.hasPermission(authentication, "user", "full_access")).isFalse();
	}

	@Test
	void deniesWhenNoMatchingPermissionExists() {
		when(permissionRepository.findByRoleIdIn(anyCollection())).thenReturn(List.of());
		FapUserPrincipal principal = new FapUserPrincipal(
				2L,
				"trainer@example.com",
				"hash",
				Set.of(RoleNames.TRAINER),
				true,
				List.of(new SimpleGrantedAuthority("ROLE_ID_11")));
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.getAuthorities());

		assertThat(evaluator.hasAction(authentication, "class", "read")).isFalse();
	}

	@Test
	void permissionsAreCachedPerRoleUntilInvalidated() {
		when(permissionRepository.findByRoleIdIn(anyCollection()))
				.thenReturn(List.of(permission(10L, "user", PermissionLevel.view)))
				.thenReturn(List.of(permission(10L, "user", PermissionLevel.modify)));
		UsernamePasswordAuthenticationToken authentication = classAdminWithRole(10L);

		assertThat(evaluator.hasAction(authentication, "user", "update")).isFalse();
		assertThat(evaluator.hasAction(authentication, "user", "read")).isTrue();
		verify(permissionRepository, times(1)).findByRoleIdIn(anyCollection());

		authorizationCache.invalidatePermissionsAfterCommit();

		assertThat(evaluator.hasAction(authentication, "user", "update")).isTrue();
		verify(permissionRepository, times(2)).findByRoleIdIn(anyCollection());
	}

	@Test
	void roleWithoutPermissionsIsCachedAsEmpty() {
		when(permissionRepository.findByRoleIdIn(anyCollection())).thenReturn(List.of());
		UsernamePasswordAuthenticationToken authentication = classAdminWithRole(12L);

		assertThat(evaluator.hasAction(authentication, "class", "read")).isFalse();
		assertThat(evaluator.hasAction(authentication, "class", "read")).isFalse();
		verify(permissionRepository, times(1)).findByRoleIdIn(anyCollection());
	}

	private static UsernamePasswordAuthenticationToken classAdminWithRole(Long roleId) {
		FapUserPrincipal principal = new FapUserPrincipal(
				3L,
				"class-admin@example.com",
				"",
				Set.of(RoleNames.CLASS_ADMIN),
				true,
				List.of(new SimpleGrantedAuthority("ROLE_ID_" + roleId)));
		return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
	}

	private static Permission permission(Long roleId, String resource, PermissionLevel level) {
		Role role = new Role();
		role.setId(roleId);
		Permission permission = new Permission();
		permission.setRole(role);
		permission.setResource(resource);
		permission.setPermissionLevel(level);
		return permission;
	}
}
