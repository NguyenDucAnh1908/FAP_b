package com.fap.common.security;

import com.fap.role.entity.Permission;
import com.fap.role.enums.PermissionLevel;
import com.fap.role.repository.PermissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PermissionEvaluatorTest {

	private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
	private final PermissionEvaluator evaluator = new PermissionEvaluator(permissionRepository);

	@Test
	void superAdminCanPerformAnyAction() {
		FapUserPrincipal principal = new FapUserPrincipal(
				1L,
				"admin@example.com",
				"hash",
				Set.of("Super Admin"),
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
		Permission permission = new Permission();
		permission.setResource("user");
		permission.setPermissionLevel(PermissionLevel.modify);
		when(permissionRepository.findByRoleIdIn(anySet())).thenReturn(List.of(permission));
		FapUserPrincipal principal = new FapUserPrincipal(
				2L,
				"manager@example.com",
				"hash",
				Set.of("Class Admin"),
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
		Permission permission = new Permission();
		permission.setResource("user");
		permission.setPermissionLevel(PermissionLevel.modify);
		when(permissionRepository.findByRoleIdIn(anySet())).thenReturn(List.of(permission));
		FapUserPrincipal principal = new FapUserPrincipal(
				2L,
				"manager@example.com",
				"hash",
				Set.of("Class Admin"),
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
		when(permissionRepository.findByRoleIdIn(anySet())).thenReturn(List.of());
		FapUserPrincipal principal = new FapUserPrincipal(
				2L,
				"trainer@example.com",
				"hash",
				Set.of("Trainer"),
				true,
				List.of(new SimpleGrantedAuthority("ROLE_ID_11")));
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.getAuthorities());

		assertThat(evaluator.hasAction(authentication, "class", "read")).isFalse();
	}
}
