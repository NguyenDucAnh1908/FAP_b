package com.fap.clazz.controller;

import com.fap.clazz.dto.ClassResponse;
import com.fap.clazz.dto.CreateClassRequest;
import com.fap.clazz.dto.UpdateClassRequest;
import com.fap.clazz.dto.UpdateClassStatusRequest;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.service.ClassService;
import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.i18n.MessageService;
import com.fap.common.security.FapUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/classes")
public class ClassController {

	private final ClassService classService;
	private final MessageService messageService;

	public ClassController(ClassService classService, MessageService messageService) {
		this.classService = classService;
		this.messageService = messageService;
	}

	@GetMapping
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public PageResponse<ClassResponse> list(
			@RequestParam(required = false) ClassStatus status,
			@RequestParam(required = false) Long trainingProgramId,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<ClassResponse> classes = classService.list(status, trainingProgramId, keyword, page - 1, limit);
		return PageResponse.of(classes.getContent(), page, limit, classes.getTotalElements());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'create')")
	public ApiResponse<ClassResponse> create(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateClassRequest request) {
		return ApiResponse.ok(
				classService.create(request, principal.id()),
				messageService.get("success.class.created"));
	}

	@GetMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<ClassResponse> get(@PathVariable Long id) {
		return ApiResponse.ok(classService.get(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public ApiResponse<ClassResponse> update(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateClassRequest request) {
		return ApiResponse.ok(classService.update(id, request, principal.id()));
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public ApiResponse<ClassResponse> updateStatus(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateClassStatusRequest request) {
		return ApiResponse.ok(classService.updateStatus(id, request.status(), principal.id()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public void delete(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classService.delete(id, principal.id());
	}
}
