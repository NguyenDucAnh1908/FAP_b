package com.fap.role.service;

import com.fap.common.exception.BadRequestException;
import com.fap.common.audit.AuditLogService;
import com.fap.role.dto.PermissionResponse;
import com.fap.role.dto.UpdatePermissionMatrixRequest;
import com.fap.role.dto.UpdatePermissionRequest;
import com.fap.role.entity.Permission;
import com.fap.role.entity.Role;
import com.fap.role.enums.PermissionLevel;
import com.fap.role.mapper.RoleMapper;
import com.fap.role.repository.PermissionRepository;
import com.fap.role.repository.RoleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleServiceTest {

	private final RoleRepository roleRepository = mock(RoleRepository.class);
	private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
	private final RoleMapper roleMapper = mock(RoleMapper.class);
	private final AuditLogService auditLogService = mock(AuditLogService.class);
	private final RoleService roleService = new RoleService(
			roleRepository,
			permissionRepository,
			roleMapper,
			auditLogService);

	@Test
	void permissionMatrixAlwaysReportsFullAccessForSuperAdmin() {
		Permission permission = new Permission();
		when(permissionRepository.findAll()).thenReturn(List.of(permission));
		when(roleMapper.toResponse(permission)).thenReturn(new PermissionResponse(
				1L,
				"Super Admin",
				"user",
				PermissionLevel.access_denied));

		List<PermissionResponse> result = roleService.permissionMatrix();

		assertThat(result).singleElement()
				.extracting(PermissionResponse::permissionLevel)
				.isEqualTo(PermissionLevel.full_access);
	}

	@Test
	void updatePermissionMatrixCannotReduceSuperAdminAccess() {
		Role role = new Role();
		role.setId(1L);
		role.setName("Super Admin");
		when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
		when(permissionRepository.findByRoleIdAndResource(1L, "user")).thenReturn(Optional.empty());
		when(permissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(permissionRepository.findAll()).thenReturn(List.of());

		roleService.updatePermissionMatrix(new UpdatePermissionMatrixRequest(List.of(
				new UpdatePermissionRequest(1L, "user", PermissionLevel.access_denied))));

		verify(permissionRepository).save(argThat(permission ->
				permission.getPermissionLevel() == PermissionLevel.full_access));
	}

	@Test
	void updatePermissionMatrixRejectsUnsupportedResource() {
		UpdatePermissionMatrixRequest request = new UpdatePermissionMatrixRequest(List.of(
				new UpdatePermissionRequest(1L, "unknown", PermissionLevel.view)));

		assertThatThrownBy(() -> roleService.updatePermissionMatrix(request))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Unsupported permission resource");
	}

	@Test
	void updatePermissionMatrixRejectsDuplicateRoleResourcePair() {
		UpdatePermissionMatrixRequest request = new UpdatePermissionMatrixRequest(List.of(
				new UpdatePermissionRequest(1L, "user", PermissionLevel.view),
				new UpdatePermissionRequest(1L, " USER ", PermissionLevel.modify)));

		assertThatThrownBy(() -> roleService.updatePermissionMatrix(request))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Duplicate permission entry");
	}

	@Test
	void updatePermissionMatrixNormalizesResourceBeforeSaving() {
		Role role = new Role();
		role.setId(1L);
		when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
		when(permissionRepository.findByRoleIdAndResource(1L, "user")).thenReturn(Optional.empty());
		when(permissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(permissionRepository.findAll()).thenReturn(List.of());

		roleService.updatePermissionMatrix(new UpdatePermissionMatrixRequest(List.of(
				new UpdatePermissionRequest(1L, " USER ", PermissionLevel.modify))));

		verify(permissionRepository).save(any(Permission.class));
		verify(permissionRepository).findByRoleIdAndResource(1L, "user");
		verify(auditLogService).record("UPDATE_PERMISSION_MATRIX", "permission", null);
	}
}
