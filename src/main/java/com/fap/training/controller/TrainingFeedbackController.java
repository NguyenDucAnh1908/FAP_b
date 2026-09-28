package com.fap.training.controller;

import com.fap.clazz.service.ClassAccessService;
import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.security.FapUserPrincipal;
import com.fap.training.dto.CreateTrainingFeedbackRequest;
import com.fap.training.dto.TrainingFeedbackResponse;
import com.fap.training.dto.TrainingFeedbackSummaryResponse;
import com.fap.training.service.TrainingFeedbackService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Training Feedback")
@Validated
@RestController
@RequestMapping("/api/v1")
public class TrainingFeedbackController {

	private final TrainingFeedbackService trainingFeedbackService;
	private final ClassAccessService classAccessService;

	public TrainingFeedbackController(
			TrainingFeedbackService trainingFeedbackService,
			ClassAccessService classAccessService) {
		this.trainingFeedbackService = trainingFeedbackService;
		this.classAccessService = classAccessService;
	}

	@Operation(summary = "Submit training feedback")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/training-sessions/{id}/feedback")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<TrainingFeedbackResponse> submit(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateTrainingFeedbackRequest request) {
		return ApiResponse.ok(trainingFeedbackService.submit(id, principal.id(), request));
	}

	@Operation(summary = "Get summary")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/training-sessions/{id}/feedback-summary")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<TrainingFeedbackSummaryResponse> summary(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanViewSession(principal, id);
		return ApiResponse.ok(trainingFeedbackService.summary(id));
	}

	@Operation(summary = "List current user training feedback")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/me/feedback")
	public PageResponse<TrainingFeedbackResponse> myFeedback(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String order) {
		Page<TrainingFeedbackResponse> feedback = trainingFeedbackService.listMine(principal.id(), page - 1, limit, sortBy, order);
		return PageResponse.of(feedback.getContent(), page, limit, feedback.getTotalElements());
	}
}
