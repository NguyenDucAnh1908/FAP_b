package com.fap.program.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.i18n.MessageService;
import com.fap.common.security.FapUserPrincipal;
import com.fap.program.dto.CreateTrainingProgramRequest;
import com.fap.program.dto.TrainingProgramResponse;
import com.fap.program.dto.TrainingProgramSyllabusResponse;
import com.fap.program.dto.UpdateTrainingProgramRequest;
import com.fap.program.dto.UpdateTrainingProgramStatusRequest;
import com.fap.program.dto.UpdateTrainingProgramSyllabusesRequest;
import com.fap.program.enums.TrainingProgramStatus;
import com.fap.program.service.TrainingProgramService;
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

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Training Programs")
@Validated
@RestController
@RequestMapping("/api/v1/training-programs")
public class TrainingProgramController {

	private final TrainingProgramService trainingProgramService;
	private final MessageService messageService;

	public TrainingProgramController(TrainingProgramService trainingProgramService, MessageService messageService) {
		this.trainingProgramService = trainingProgramService;
		this.messageService = messageService;
	}

	@Operation(summary = "List training programs")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'training_program', 'view')")
	public PageResponse<TrainingProgramResponse> list(
			@RequestParam(required = false) TrainingProgramStatus status,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String order) {
		Page<TrainingProgramResponse> programs = trainingProgramService.list(status, keyword, page - 1, limit, sortBy, order);
		return PageResponse.of(programs.getContent(), page, limit, programs.getTotalElements());
	}

	@Operation(summary = "Create training programs")
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
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'training_program', 'create')")
	public ApiResponse<TrainingProgramResponse> create(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateTrainingProgramRequest request) {
		return ApiResponse.ok(
				trainingProgramService.create(request, principal.id()),
				messageService.get("success.training_program.created"));
	}

	@Operation(summary = "Get training programs detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'training_program', 'view')")
	public ApiResponse<TrainingProgramResponse> get(@PathVariable Long id) {
		return ApiResponse.ok(trainingProgramService.get(id));
	}

	@Operation(summary = "Update training programs")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'training_program', 'modify')")
	public ApiResponse<TrainingProgramResponse> update(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateTrainingProgramRequest request) {
		return ApiResponse.ok(trainingProgramService.update(id, request, principal.id()));
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
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'training_program', 'modify')")
	public ApiResponse<TrainingProgramResponse> updateStatus(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateTrainingProgramStatusRequest request) {
		return ApiResponse.ok(trainingProgramService.updateStatus(id, request.status(), principal.id()));
	}

	@Operation(summary = "Delete training programs")
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
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'training_program', 'modify')")
	public void delete(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		trainingProgramService.delete(id, principal.id());
	}

	@Operation(summary = "List syllabuses")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}/syllabuses")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'training_program', 'view')")
	public ApiResponse<List<TrainingProgramSyllabusResponse>> listSyllabuses(@PathVariable Long id) {
		return ApiResponse.ok(trainingProgramService.listSyllabuses(id));
	}

	@Operation(summary = "Replace syllabuses")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/syllabuses")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'training_program', 'modify')")
	public ApiResponse<List<TrainingProgramSyllabusResponse>> replaceSyllabuses(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateTrainingProgramSyllabusesRequest request) {
		return ApiResponse.ok(trainingProgramService.replaceSyllabuses(id, request, principal.id()));
	}
}
