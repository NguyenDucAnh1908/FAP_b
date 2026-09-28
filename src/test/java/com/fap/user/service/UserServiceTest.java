package com.fap.user.service;

import com.fap.common.exception.ConflictException;
import com.fap.common.exception.NotFoundException;
import com.fap.common.audit.AuditLogService;
import com.fap.common.security.AuthorizationCache;
import com.fap.common.security.RoleNames;
import com.fap.role.entity.Role;
import com.fap.role.repository.RoleRepository;
import com.fap.user.dto.CreateUserRequest;
import com.fap.user.dto.UpdateUserRequest;
import com.fap.user.dto.UserResponse;
import com.fap.user.entity.User;
import com.fap.user.enums.Gender;
import com.fap.user.enums.UserStatus;
import com.fap.user.mapper.UserMapper;
import com.fap.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final UserMapper userMapper = mock(UserMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final AuthorizationCache authorizationCache = mock(AuthorizationCache.class);
	private final UserService userService = new UserService(
			userRepository,
			roleRepository,
			passwordEncoder,
			userMapper,
			auditLogService,
			authorizationCache);

	@BeforeEach
	void useRealRepositoryLookups() {
		// The default lookup must run so the findWithRolesById stubs below keep driving behaviour.
		lenient().doCallRealMethod().when(userRepository).getWithRolesOrThrow(any());
	}

	@Test
	void listNormalizesFiltersAndDelegatesToSearchRepository() {
		when(userRepository.search(
				eq(UserStatus.Active),
				eq("admin"),
				eq("admin@example.com"),
				eq("System Admin"),
				eq(1L),
				eq("super admin"),
				any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));

		userService.list(
				UserStatus.Active,
				" admin ",
				" admin@example.com ",
				" System Admin ",
				1L,
				" Super Admin ",
				0,
				20);

		verify(userRepository).search(
				eq(UserStatus.Active),
				eq("admin"),
				eq("admin@example.com"),
				eq("System Admin"),
				eq(1L),
				eq("super admin"),
				any(Pageable.class));
	}

	@Test
	void createHashesPasswordAndMapsResponse() {
		Role role = new Role();
		role.setId(1L);
		when(userRepository.existsByEmailIgnoreCase("USER@Example.com")).thenReturn(false);
		when(roleRepository.findByIdIn(Set.of(1L))).thenReturn(Set.of(role));
		when(passwordEncoder.encode("password123")).thenReturn("hash");
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		UserResponse expected = new UserResponse(null, "User", "user@example.com", null, null, Gender.Male, null, null, null, null, null);
		when(userMapper.toResponse(any())).thenReturn(expected);

		UserResponse response = userService.create(new CreateUserRequest(
				"User",
				"USER@Example.com",
				null,
				"password123",
				null,
				Gender.Male,
				null,
				Set.of(1L)),
				Set.of(RoleNames.SUPER_ADMIN));

		assertThat(response).isEqualTo(expected);
		verify(passwordEncoder).encode("password123");
		verify(auditLogService).record("CREATE_USER", "user", null);
	}

	@Test
	void createRejectsDuplicateEmail() {
		when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

		assertThatThrownBy(() -> userService.create(new CreateUserRequest(
				"User",
				"user@example.com",
				null,
				"password123",
				null,
				Gender.Male,
				null,
				Set.of(1L)),
				Set.of(RoleNames.SUPER_ADMIN)))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void createRejectsUnknownRole() {
		when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
		when(roleRepository.findByIdIn(Set.of(99L))).thenReturn(Set.of());

		assertThatThrownBy(() -> userService.create(new CreateUserRequest(
				"User",
				"user@example.com",
				null,
				"password123",
				null,
				Gender.Male,
				null,
				Set.of(99L)),
				Set.of(RoleNames.SUPER_ADMIN)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void createRejectsSuperAdminRoleWhenActorIsNotSuperAdmin() {
		Role superAdmin = new Role();
		superAdmin.setId(1L);
		superAdmin.setName(RoleNames.SUPER_ADMIN);
		when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
		when(roleRepository.findByIdIn(Set.of(1L))).thenReturn(Set.of(superAdmin));

		assertThatThrownBy(() -> userService.create(new CreateUserRequest(
				"User",
				"user@example.com",
				null,
				"password123",
				null,
				Gender.Male,
				null,
				Set.of(1L)),
				Set.of(RoleNames.CLASS_ADMIN)))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Only Super Admin can manage Super Admin role");
	}

	@Test
	void updateRejectsRemovingLastActiveSuperAdminRole() {
		Role superAdmin = new Role();
		superAdmin.setId(1L);
		superAdmin.setName(RoleNames.SUPER_ADMIN);
		Role trainer = new Role();
		trainer.setId(2L);
		trainer.setName(RoleNames.TRAINER);
		User user = new User();
		user.setId(1000L);
		user.setEmail("admin@example.com");
		user.setStatus(UserStatus.Active);
		user.setRoles(Set.of(superAdmin));
		when(userRepository.findWithRolesById(1000L)).thenReturn(Optional.of(user));
		when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
		when(roleRepository.findByIdIn(Set.of(2L))).thenReturn(Set.of(trainer));
		when(userRepository.countByRoleNameAndStatus(RoleNames.SUPER_ADMIN, UserStatus.Active)).thenReturn(1L);

		assertThatThrownBy(() -> userService.update(1000L, new UpdateUserRequest(
				"Admin",
				"admin@example.com",
				null,
				null,
				Gender.Male,
				null,
				Set.of(2L)),
				Set.of(RoleNames.SUPER_ADMIN)))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Cannot remove Super Admin role from the last active Super Admin");
	}

	@Test
	void getRejectsUnknownUser() {
		when(userRepository.findWithRolesById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.get(404L))
				.isInstanceOf(NotFoundException.class)
				.hasMessage("User not found");
	}

	@Test
	void updateStatusRejectsSelfDeactivation() {
		User user = new User();
		user.setId(1000L);
		user.setStatus(UserStatus.Active);
		when(userRepository.findWithRolesById(1000L)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> userService.updateStatus(1000L, UserStatus.Inactive, 1000L))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Cannot deactivate your own account");
	}

	@Test
	void updateStatusRejectsLastActiveSuperAdminDeactivation() {
		Role superAdmin = new Role();
		superAdmin.setName(RoleNames.SUPER_ADMIN);
		User user = new User();
		user.setId(1000L);
		user.setStatus(UserStatus.Active);
		user.setRoles(Set.of(superAdmin));
		when(userRepository.findWithRolesById(1000L)).thenReturn(Optional.of(user));
		when(userRepository.countByRoleNameAndStatus(RoleNames.SUPER_ADMIN, UserStatus.Active)).thenReturn(1L);

		assertThatThrownBy(() -> userService.updateStatus(1000L, UserStatus.Inactive, 2000L))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Cannot deactivate the last active Super Admin");
	}

	@Test
	void updateStatusAllowsDeactivatingNonSelfWhenAnotherSuperAdminRemains() {
		Role superAdmin = new Role();
		superAdmin.setName(RoleNames.SUPER_ADMIN);
		User user = new User();
		user.setId(1000L);
		user.setStatus(UserStatus.Active);
		user.setRoles(Set.of(superAdmin));
		UserResponse expected = new UserResponse(1000L, "Admin", "admin@example.com", null, null, Gender.Male, null, UserStatus.Inactive, null, null, null);
		when(userRepository.findWithRolesById(1000L)).thenReturn(Optional.of(user));
		when(userRepository.countByRoleNameAndStatus(RoleNames.SUPER_ADMIN, UserStatus.Active)).thenReturn(2L);
		when(userMapper.toResponse(user)).thenReturn(expected);

		UserResponse response = userService.updateStatus(1000L, UserStatus.Inactive, 2000L);

		assertThat(response).isEqualTo(expected);
		assertThat(user.getStatus()).isEqualTo(UserStatus.Inactive);
		assertThat(user.getUpdatedAt()).isNotNull();
		verify(auditLogService).record("UPDATE_USER_STATUS:Inactive", "user", 1000L);
	}
}
