package com.fap.settings.controller;

import com.fap.common.api.ApiResponse;
import com.fap.settings.dto.SettingsResponse;
import com.fap.settings.dto.UpdateSettingsRequest;
import com.fap.settings.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Settings")
@Validated
@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

	private final SettingsService settingsService;

	public SettingsController(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@Operation(summary = "Get settings detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'user', 'view')")
	public ApiResponse<SettingsResponse> get(@RequestParam(required = false) String category) {
		if (category != null && !category.isBlank()) {
			return ApiResponse.ok(settingsService.getByCategory(category));
		}
		return ApiResponse.ok(settingsService.getAll());
	}

	@Operation(summary = "Update settings")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'user', 'full_access')")
	public ApiResponse<SettingsResponse> update(@Valid @RequestBody UpdateSettingsRequest request) {
		return ApiResponse.ok(settingsService.update(request));
	}
}
