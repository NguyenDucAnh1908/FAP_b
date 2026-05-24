package com.fap.role.service;

import com.fap.common.exception.BadRequestException;
import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.NotFoundException;
import com.fap.role.dto.PermissionResponse;
import com.fap.role.dto.RoleResponse;
import com.fap.role.dto.UpdatePermissionRequest;
import com.fap.role.dto.UpdatePermissionMatrixRequest;
import com.fap.role.entity.Permission;
import com.fap.role.entity.Role;
import com.fap.role.mapper.RoleMapper;
import com.fap.role.repository.PermissionRepository;
import com.fap.role.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RoleService {

	private static final Set<String> SUPPORTED_PERMISSION_RESOURCES = Set.of(
			"user",
			"syllabus",
			"training_program",
			"class",
			"learning_material");

	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final RoleMapper roleMapper;
	private final AuditLogService auditLogService;

	public RoleService(
			RoleRepository roleRepository,
			PermissionRepository permissionRepository,
			RoleMapper roleMapper,
			AuditLogService auditLogService) {
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
		this.roleMapper = roleMapper;
		this.auditLogService = auditLogService;
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
		validatePermissionMatrix(request);
		request.permissions().forEach(item -> {
			String resource = normalizeResource(item.resource());
			Role role = roleRepository.findById(item.roleId())
					.orElseThrow(() -> new NotFoundException("Role not found"));
			Permission permission = permissionRepository.findByRoleIdAndResource(item.roleId(), resource)
					.orElseGet(() -> {
						Permission created = new Permission();
						created.setRole(role);
						created.setResource(resource);
						return created;
					});
			permission.setPermissionLevel(item.permissionLevel());
			permissionRepository.save(permission);
		});
		auditLogService.record("UPDATE_PERMISSION_MATRIX", "permission", null);
		return permissionMatrix();
	}

	private void validatePermissionMatrix(UpdatePermissionMatrixRequest request) {
		Set<String> seen = new HashSet<>();
		for (UpdatePermissionRequest item : request.permissions()) {
			String resource = normalizeResource(item.resource());
			if (!SUPPORTED_PERMISSION_RESOURCES.contains(resource)) {
				throw new BadRequestException("INVALID_PERMISSION_RESOURCE", "Unsupported permission resource");
			}
			String permissionKey = item.roleId() + ":" + resource;
			if (!seen.add(permissionKey)) {
				throw new BadRequestException("DUPLICATE_PERMISSION_ENTRY", "Duplicate permission entry");
			}
		}
	}

	private String normalizeResource(String resource) {
		return resource.trim().toLowerCase(Locale.ROOT);
	}
}
