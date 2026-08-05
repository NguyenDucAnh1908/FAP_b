package com.fap.syllabus.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.i18n.MessageService;
import com.fap.common.security.FapUserPrincipal;
import com.fap.syllabus.dto.CreateMaterialFileRequest;
import com.fap.syllabus.dto.CreateFullSyllabusRequest;
import com.fap.syllabus.dto.CreateSyllabusRequest;
import com.fap.syllabus.dto.CreateSyllabusDayRequest;
import com.fap.syllabus.dto.CreateSyllabusTopicRequest;
import com.fap.syllabus.dto.CreateSyllabusUnitRequest;
import com.fap.syllabus.dto.MaterialFileResponse;
import com.fap.syllabus.dto.QuickCreateSyllabusRequest;
import com.fap.syllabus.dto.FullSyllabusResponse;
import com.fap.syllabus.dto.SyllabusDayResponse;
import com.fap.syllabus.dto.SyllabusImportResponse;
import com.fap.syllabus.dto.SyllabusResponse;
import com.fap.syllabus.dto.SyllabusTopicResponse;
import com.fap.syllabus.dto.SyllabusUnitResponse;
import com.fap.syllabus.dto.UpdateSyllabusOutputStandardsRequest;
import com.fap.syllabus.dto.UpdateSyllabusRequest;
import com.fap.syllabus.dto.UpdateSyllabusStatusRequest;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.service.MaterialFileService;
import com.fap.syllabus.service.SyllabusImportService;
import com.fap.syllabus.service.SyllabusOutlineService;
import com.fap.syllabus.service.SyllabusOutputStandardService;
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

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Syllabus")
@Validated
@RestController
@RequestMapping("/api/v1/syllabuses")
public class SyllabusController {

	private final SyllabusService syllabusService;
	private final SyllabusOutlineService outlineService;
	private final SyllabusOutputStandardService outputStandardService;
	private final SyllabusImportService syllabusImportService;
	private final MaterialFileService materialFileService;
	private final MessageService messageService;

	public SyllabusController(
			SyllabusService syllabusService,
			SyllabusOutlineService outlineService,
			SyllabusOutputStandardService outputStandardService,
			SyllabusImportService syllabusImportService,
			MaterialFileService materialFileService,
			MessageService messageService) {
		this.syllabusService = syllabusService;
		this.outlineService = outlineService;
		this.outputStandardService = outputStandardService;
		this.syllabusImportService = syllabusImportService;
		this.materialFileService = materialFileService;
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

	@Operation(summary = "Create full syllabus with outline")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/full")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'create')")
	public ApiResponse<FullSyllabusResponse> createFull(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateFullSyllabusRequest request) {
		return ApiResponse.ok(
				syllabusService.createFull(request, principal.id()),
				messageService.get("success.syllabus.created"));
	}

	@Operation(summary = "Update full syllabus with outline")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/full")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<FullSyllabusResponse> updateFull(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateFullSyllabusRequest request) {
		return ApiResponse.ok(syllabusService.updateFull(id, request, principal.id()));
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

	@Operation(summary = "Get full syllabus detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}/full")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'view')")
	public ApiResponse<FullSyllabusResponse> getFull(@PathVariable Long id) {
		return ApiResponse.ok(syllabusService.getFull(id));
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

	@Operation(summary = "Get outline detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}/outline")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'view')")
	public ApiResponse<List<SyllabusDayResponse>> getOutline(@PathVariable Long id) {
		return ApiResponse.ok(outlineService.getOutline(id));
	}

	@Operation(summary = "Get output standards detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}/output-standards")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'view')")
	public ApiResponse<List<String>> getOutputStandards(@PathVariable Long id) {
		return ApiResponse.ok(outputStandardService.list(id));
	}

	@Operation(summary = "Replace output standards")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/output-standards")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<List<String>> replaceOutputStandards(
			@PathVariable Long id,
			@Valid @RequestBody UpdateSyllabusOutputStandardsRequest request) {
		return ApiResponse.ok(outputStandardService.replace(id, request));
	}

	@Operation(summary = "Create day")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/{id}/days")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusDayResponse> createDay(
			@PathVariable Long id,
			@Valid @RequestBody CreateSyllabusDayRequest request) {
		return ApiResponse.ok(outlineService.createDay(id, request));
	}

	@Operation(summary = "Update day")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/days/{dayId}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusDayResponse> updateDay(
			@PathVariable Long id,
			@PathVariable Long dayId,
			@Valid @RequestBody CreateSyllabusDayRequest request) {
		return ApiResponse.ok(outlineService.updateDay(id, dayId, request));
	}

	@Operation(summary = "Delete day")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No content"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@DeleteMapping("/{id}/days/{dayId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void deleteDay(@PathVariable Long id, @PathVariable Long dayId) {
		outlineService.deleteDay(id, dayId);
	}

	@Operation(summary = "Create unit")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/{id}/days/{dayId}/units")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusUnitResponse> createUnit(
			@PathVariable Long id,
			@PathVariable Long dayId,
			@Valid @RequestBody CreateSyllabusUnitRequest request) {
		return ApiResponse.ok(outlineService.createUnit(id, dayId, request));
	}

	@Operation(summary = "Update unit")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/units/{unitId}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusUnitResponse> updateUnit(
			@PathVariable Long id,
			@PathVariable Long unitId,
			@Valid @RequestBody CreateSyllabusUnitRequest request) {
		return ApiResponse.ok(outlineService.updateUnit(id, unitId, request));
	}

	@Operation(summary = "Delete unit")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No content"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@DeleteMapping("/{id}/units/{unitId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void deleteUnit(@PathVariable Long id, @PathVariable Long unitId) {
		outlineService.deleteUnit(id, unitId);
	}

	@Operation(summary = "Create topic")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/{id}/units/{unitId}/topics")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusTopicResponse> createTopic(
			@PathVariable Long id,
			@PathVariable Long unitId,
			@Valid @RequestBody CreateSyllabusTopicRequest request) {
		return ApiResponse.ok(outlineService.createTopic(id, unitId, request));
	}

	@Operation(summary = "Update topic")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/topics/{topicId}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusTopicResponse> updateTopic(
			@PathVariable Long id,
			@PathVariable Long topicId,
			@Valid @RequestBody CreateSyllabusTopicRequest request) {
		return ApiResponse.ok(outlineService.updateTopic(id, topicId, request));
	}

	@Operation(summary = "Delete topic")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No content"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@DeleteMapping("/{id}/topics/{topicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void deleteTopic(@PathVariable Long id, @PathVariable Long topicId) {
		outlineService.deleteTopic(id, topicId);
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
	@GetMapping("/{id}/topics/{topicId}/materials")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'view')")
	public ApiResponse<List<MaterialFileResponse>> listMaterials(
			@PathVariable Long id,
			@PathVariable Long topicId) {
		return ApiResponse.ok(materialFileService.list(id, topicId));
	}

	@Operation(summary = "Create material")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/{id}/topics/{topicId}/materials")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<MaterialFileResponse> createMaterial(
			@PathVariable Long id,
			@PathVariable Long topicId,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateMaterialFileRequest request) {
		return ApiResponse.ok(materialFileService.create(id, topicId, request, principal.id()));
	}

	@Operation(summary = "Delete material")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No content"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@DeleteMapping("/{id}/topics/{topicId}/materials/{materialId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void deleteMaterial(
			@PathVariable Long id,
			@PathVariable Long topicId,
			@PathVariable Long materialId) {
		materialFileService.delete(id, topicId, materialId);
	}
}
