package com.fap.role.controller;

import com.fap.common.api.ApiResponse;
import com.fap.role.dto.PermissionResponse;
import com.fap.role.dto.RoleResponse;
import com.fap.role.dto.UpdatePermissionMatrixRequest;
import com.fap.role.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

	private final RoleService roleService;

	public RoleController(RoleService roleService) {
		this.roleService = roleService;
	}

	@GetMapping
	@PreAuthorize("@permissionEvaluator.hasAction(authentication, 'user', 'read')")
	public ApiResponse<List<RoleResponse>> listRoles() {
		return ApiResponse.ok(roleService.listRoles());
	}

	@GetMapping("/permissions")
	@PreAuthorize("@permissionEvaluator.hasAction(authentication, 'user', 'read')")
	public ApiResponse<List<PermissionResponse>> permissionMatrix() {
		return ApiResponse.ok(roleService.permissionMatrix());
	}

	@PutMapping("/permissions")
	@PreAuthorize("@permissionEvaluator.hasAction(authentication, 'user', 'admin')")
	public ApiResponse<List<PermissionResponse>> updatePermissionMatrix(
			@Valid @RequestBody UpdatePermissionMatrixRequest request) {
		return ApiResponse.ok(roleService.updatePermissionMatrix(request), "Permission matrix updated");
	}
}
