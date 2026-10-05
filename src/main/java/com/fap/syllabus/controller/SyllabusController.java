package com.fap.syllabus.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.i18n.MessageService;
import com.fap.common.security.FapUserPrincipal;
import com.fap.syllabus.dto.CreateSyllabusRequest;
import com.fap.syllabus.dto.QuickCreateSyllabusRequest;
import com.fap.syllabus.dto.SyllabusImportResponse;
import com.fap.syllabus.dto.SyllabusResponse;
import com.fap.syllabus.dto.UpdateSyllabusRequest;
import com.fap.syllabus.dto.UpdateSyllabusStatusRequest;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.service.SyllabusImportService;
import com.fap.syllabus.service.SyllabusService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Syllabus header CRUD, status and CSV import. The other endpoints under this path are split by
 * concern into {@link SyllabusFullController}, {@link SyllabusOutlineController},
 * {@link SyllabusOutputStandardController} and {@link SyllabusMaterialController}.
 */
@Tag(name = "Syllabus")
@Validated
@RestController
@RequestMapping("/api/v1/syllabuses")
public class SyllabusController {

	private final SyllabusService syllabusService;
	private final SyllabusImportService syllabusImportService;
	private final MessageService messageService;

	public SyllabusController(
			SyllabusService syllabusService,
			SyllabusImportService syllabusImportService,
			MessageService messageService) {
		this.syllabusService = syllabusService;
		this.syllabusImportService = syllabusImportService;
		this.messageService = messageService;
	}

	@Operation(summary = "List syllabus")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'view')")
	public PageResponse<SyllabusResponse> list(
			@RequestParam(required = false) SyllabusStatus status,
			@RequestParam(required = false) String levelName,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String order) {
		Page<SyllabusResponse> syllabuses = syllabusService.list(status, levelName, keyword, page - 1, limit, sortBy, order);
		return PageResponse.of(syllabuses.getContent(), page, limit, syllabuses.getTotalElements());
	}

	@Operation(summary = "Create syllabus")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'create')")
	public ApiResponse<SyllabusResponse> create(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateSyllabusRequest request) {
		return ApiResponse.ok(
				syllabusService.create(request, principal.id()),
				messageService.get("success.syllabus.created"));
	}

	@Operation(summary = "Quick create syllabus draft")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/quick-create")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'create')")
	public ApiResponse<SyllabusResponse> quickCreate(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody QuickCreateSyllabusRequest request) {
		return ApiResponse.ok(
				syllabusService.quickCreate(request, principal.id()),
				messageService.get("success.syllabus.created"));
	}

	@Operation(summary = "Import syllabuses from CSV file")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'create')")
	public ApiResponse<SyllabusImportResponse> importCsv(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam("file") MultipartFile file) {
		return ApiResponse.ok(
				syllabusImportService.importCsv(file, principal.id()),
				messageService.get("success.syllabus.imported"));
	}

	@Operation(summary = "Get syllabus detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'view')")
	public ApiResponse<SyllabusResponse> get(@PathVariable Long id) {
		return ApiResponse.ok(syllabusService.get(id));
	}

	@Operation(summary = "Update syllabus")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusResponse> update(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateSyllabusRequest request) {
		return ApiResponse.ok(syllabusService.update(id, request, principal.id()));
	}

	@Operation(summary = "Update status")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PatchMapping("/{id}/status")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusResponse> updateStatus(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateSyllabusStatusRequest request) {
		return ApiResponse.ok(syllabusService.updateStatus(id, request.status(), principal.id()));
	}

	@Operation(summary = "Delete syllabus")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No content"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void delete(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		syllabusService.delete(id, principal.id());
	}
}
