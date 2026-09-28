package com.fap.syllabus.controller;

import com.fap.common.api.ApiResponse;
import com.fap.syllabus.dto.UpdateSyllabusOutputStandardsRequest;
import com.fap.syllabus.service.SyllabusOutputStandardService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Syllabus")
@Validated
@RestController
@RequestMapping("/api/v1/syllabuses")
public class SyllabusOutputStandardController {

	private final SyllabusOutputStandardService outputStandardService;

	public SyllabusOutputStandardController(SyllabusOutputStandardService outputStandardService) {
		this.outputStandardService = outputStandardService;
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
}
