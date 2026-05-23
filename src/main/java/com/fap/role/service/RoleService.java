package com.fap.role.service;

import com.fap.common.exception.NotFoundException;
import com.fap.role.dto.PermissionResponse;
import com.fap.role.dto.RoleResponse;
import com.fap.role.dto.UpdatePermissionMatrixRequest;
import com.fap.role.entity.Permission;
import com.fap.role.entity.Role;
import com.fap.role.mapper.RoleMapper;
import com.fap.role.repository.PermissionRepository;
import com.fap.role.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final RoleMapper roleMapper;

	public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository, RoleMapper roleMapper) {
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
		this.roleMapper = roleMapper;
	}

	@Transactional(readOnly = true)
	public List<RoleResponse> listRoles() {
		return roleRepository.findAll().stream()
				.map(roleMapper::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<PermissionResponse> permissionMatrix() {
		return permissionRepository.findAll().stream()
				.map(roleMapper::toResponse)
				.toList();
	}

	@Transactional
	public List<PermissionResponse> updatePermissionMatrix(UpdatePermissionMatrixRequest request) {
		request.permissions().forEach(item -> {
			Role role = roleRepository.findById(item.roleId())
					.orElseThrow(() -> new NotFoundException("Role not found"));
			Permission permission = permissionRepository.findByRoleIdAndResource(item.roleId(), item.resource())
					.orElseGet(() -> {
						Permission created = new Permission();
						created.setRole(role);
						created.setResource(item.resource());
						return created;
					});
			permission.setPermissionLevel(item.permissionLevel());
			permissionRepository.save(permission);
		});
		return permissionMatrix();
	}
}
