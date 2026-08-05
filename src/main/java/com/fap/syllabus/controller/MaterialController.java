package com.fap.syllabus.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.security.FapUserPrincipal;
import com.fap.common.security.PermissionEvaluator;
import com.fap.syllabus.dto.AssignedMaterialFileResponse;
import com.fap.syllabus.dto.CreateMaterialRequest;
import com.fap.syllabus.dto.MaterialFileDownload;
import com.fap.syllabus.dto.MaterialFileResponse;
import com.fap.syllabus.dto.UpdateMaterialFileRequest;
import com.fap.syllabus.service.MaterialFileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Materials")
@Validated
@RestController
@RequestMapping("/api/v1")
public class MaterialController {

	private final MaterialFileService materialFileService;
	private final PermissionEvaluator permissionEvaluator;

	public MaterialController(MaterialFileService materialFileService, PermissionEvaluator permissionEvaluator) {
		this.materialFileService = materialFileService;
		this.permissionEvaluator = permissionEvaluator;
	}

	@Operation(summary = "List materials")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/materials")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'view')")
	public PageResponse<MaterialFileResponse> list(
			@RequestParam(required = false) Long syllabusId,
			@RequestParam(required = false) Long topicId,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String order) {
		Page<MaterialFileResponse> materials = materialFileService.listLibrary(
				syllabusId,
				topicId,
				keyword,
				page - 1,
				limit,
				sortBy,
				order);
		return PageResponse.of(materials.getContent(), page, limit, materials.getTotalElements());
	}

	@Operation(summary = "Create materials")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/materials")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'modify')")
	public ApiResponse<MaterialFileResponse> create(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateMaterialRequest request) {
		return ApiResponse.ok(materialFileService.create(request, principal.id()));
	}

	@Operation(summary = "Get materials detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/materials/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'view')")
	public ApiResponse<MaterialFileResponse> get(@PathVariable Long id) {
		return ApiResponse.ok(materialFileService.get(id));
	}

	@Operation(summary = "Update materials")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/materials/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'modify')")
	public ApiResponse<MaterialFileResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody UpdateMaterialFileRequest request) {
		return ApiResponse.ok(materialFileService.update(id, request));
	}

	@Operation(summary = "Delete materials")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No content"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@DeleteMapping("/materials/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'modify')")
	public void delete(@PathVariable Long id) {
		materialFileService.delete(id);
	}

	@Operation(summary = "Upload a material file")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping(value = "/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'modify')")
	public ApiResponse<MaterialFileResponse> upload(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam Long syllabusId,
			@RequestParam Long topicId,
			@RequestParam("file") MultipartFile file) {
		return ApiResponse.ok(materialFileService.upload(syllabusId, topicId, file, principal.id()));
	}

	/**
	 * Streams an uploaded material's bytes. The method-level check is deliberately only the coarse
	 * {@code view} gate — trainees hold it globally, so the per-material ownership decision is made
	 * in the service from the {@code modify} action check passed below.
	 */
	@Operation(summary = "Download a material file")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
	})
	@GetMapping("/materials/{id}/download")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'view')")
	public ResponseEntity<ByteArrayResource> download(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			Authentication authentication) {
		boolean canManageMaterials = permissionEvaluator.hasAction(authentication, "learning_material", "update");
		MaterialFileDownload download = materialFileService.download(id, principal.id(), canManageMaterials);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(download.contentType()))
				.header(
						HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(download.fileName()).build().toString())
				.contentLength(download.data().length)
				.body(new ByteArrayResource(download.data()));
	}

	@Operation(summary = "List current user assigned materials")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/me/materials")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'learning_material', 'view')")
	public PageResponse<AssignedMaterialFileResponse> assignedToMe(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String order) {
		Page<AssignedMaterialFileResponse> materials = materialFileService.assignedToUser(
				principal.id(),
				keyword,
				page - 1,
				limit,
				sortBy,
				order);
		return PageResponse.of(materials.getContent(), page, limit, materials.getTotalElements());
	}
}
