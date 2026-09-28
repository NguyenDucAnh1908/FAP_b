package com.fap.syllabus.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.i18n.MessageService;
import com.fap.common.security.FapUserPrincipal;
import com.fap.syllabus.dto.CloneSyllabusRequest;
import com.fap.syllabus.dto.CreateFullSyllabusRequest;
import com.fap.syllabus.dto.FullSyllabusResponse;
import com.fap.syllabus.service.SyllabusFullService;
import com.fap.syllabus.service.SyllabusVersionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/** A syllabus with its whole outline in one request, and cloning it as a new version. */
@Tag(name = "Syllabus")
@Validated
@RestController
@RequestMapping("/api/v1/syllabuses")
public class SyllabusFullController {

	private final SyllabusFullService syllabusFullService;
	private final SyllabusVersionService syllabusVersionService;
	private final MessageService messageService;

	public SyllabusFullController(
			SyllabusFullService syllabusFullService,
			SyllabusVersionService syllabusVersionService,
			MessageService messageService) {
		this.syllabusFullService = syllabusFullService;
		this.syllabusVersionService = syllabusVersionService;
		this.messageService = messageService;
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
				syllabusFullService.createFull(request, principal.id()),
				messageService.get("success.syllabus.created"));
	}

	@Operation(summary = "Create a new syllabus version from an active or inactive syllabus")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/{id}/clone")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'create')")
	public ApiResponse<FullSyllabusResponse> cloneVersion(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CloneSyllabusRequest request) {
		return ApiResponse.ok(
				syllabusVersionService.cloneVersion(id, request, principal.id()),
				messageService.get("success.syllabus.cloned"));
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
		return ApiResponse.ok(syllabusFullService.updateFull(id, request, principal.id()));
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
		return ApiResponse.ok(syllabusFullService.getFull(id));
	}
}
