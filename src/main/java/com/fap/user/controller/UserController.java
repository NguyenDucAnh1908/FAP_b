package com.fap.user.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.i18n.MessageService;
import com.fap.common.security.FapUserPrincipal;
import com.fap.user.dto.CreateUserRequest;
import com.fap.user.dto.UpdateUserRequest;
import com.fap.user.dto.UpdateUserStatusRequest;
import com.fap.user.dto.UserResponse;
import com.fap.user.enums.UserStatus;
import com.fap.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;
	private final MessageService messageService;

	public UserController(UserService userService, MessageService messageService) {
		this.userService = userService;
		this.messageService = messageService;
	}

	@GetMapping
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'user', 'view')")
	public PageResponse<UserResponse> list(
			@RequestParam(required = false) UserStatus status,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String email,
			@RequestParam(required = false) String fullName,
			@RequestParam(required = false) Long roleId,
			@RequestParam(required = false) String roleName,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<UserResponse> users = userService.list(
				status,
				keyword,
				email,
				fullName,
				roleId,
				roleName,
				page - 1,
				limit);
		return PageResponse.of(users.getContent(), page, limit, users.getTotalElements());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'user', 'create')")
	public ApiResponse<UserResponse> create(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateUserRequest request) {
		return ApiResponse.ok(userService.create(request, principal.roles()), messageService.get("success.user.created"));
	}

	@GetMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'user', 'view')")
	public ApiResponse<UserResponse> get(@PathVariable Long id) {
		return ApiResponse.ok(userService.get(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'user', 'modify')")
	public ApiResponse<UserResponse> update(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateUserRequest request) {
		return ApiResponse.ok(userService.update(id, request, principal.roles()));
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'user', 'modify')")
	public ApiResponse<UserResponse> updateStatus(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateUserStatusRequest request) {
		return ApiResponse.ok(userService.updateStatus(id, request.status(), principal.id()));
	}
}
