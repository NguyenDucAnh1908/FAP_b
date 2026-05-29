package com.fap.syllabus.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.security.FapUserPrincipal;
import com.fap.syllabus.dto.AssignedMaterialFileResponse;
import com.fap.syllabus.dto.CreateMaterialRequest;
import com.fap.syllabus.dto.MaterialFileResponse;
import com.fap.syllabus.dto.UpdateMaterialFileRequest;
import com.fap.syllabus.service.MaterialFileService;
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
@RequestMapping("/api/v1")
public class MaterialController {

	private final MaterialFileService materialFileService;

	public MaterialController(MaterialFileService materialFileService) {
		this.materialFileService = materialFileService;
	}

	@GetMapping("/materials")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'view')")
	public PageResponse<MaterialFileResponse> list(
			@RequestParam(required = false) Long syllabusId,
			@RequestParam(required = false) Long topicId,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<MaterialFileResponse> materials = materialFileService.listLibrary(
				syllabusId,
				topicId,
				keyword,
				page - 1,
				limit);
		return PageResponse.of(materials.getContent(), page, limit, materials.getTotalElements());
	}

	@PostMapping("/materials")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'modify')")
	public ApiResponse<MaterialFileResponse> create(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateMaterialRequest request) {
		return ApiResponse.ok(materialFileService.create(request, principal.id()));
	}

	@GetMapping("/materials/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'view')")
	public ApiResponse<MaterialFileResponse> get(@PathVariable Long id) {
		return ApiResponse.ok(materialFileService.get(id));
	}

	@PutMapping("/materials/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'modify')")
	public ApiResponse<MaterialFileResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody UpdateMaterialFileRequest request) {
		return ApiResponse.ok(materialFileService.update(id, request));
	}

	@DeleteMapping("/materials/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'modify')")
	public void delete(@PathVariable Long id) {
		materialFileService.delete(id);
	}

	@GetMapping("/me/materials")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'view')")
	public PageResponse<AssignedMaterialFileResponse> assignedToMe(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<AssignedMaterialFileResponse> materials = materialFileService.assignedToUser(
				principal.id(),
				keyword,
				page - 1,
				limit);
		return PageResponse.of(materials.getContent(), page, limit, materials.getTotalElements());
	}
}
