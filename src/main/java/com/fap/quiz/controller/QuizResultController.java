package com.fap.quiz.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.security.FapUserPrincipal;
import com.fap.quiz.dto.QuizAttemptResultResponse;
import com.fap.quiz.dto.QuizAttemptReviewResponse;
import com.fap.quiz.dto.QuizAttemptSummaryResponse;
import com.fap.quiz.enums.QuizAttemptStatus;
import com.fap.quiz.service.QuizResultService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Quiz Results")
@Validated
@RestController
@RequestMapping("/api/v1/quizzes/{quizId}")
public class QuizResultController {

	private final QuizResultService quizResultService;

	public QuizResultController(QuizResultService quizResultService) {
		this.quizResultService = quizResultService;
	}

	@Operation(summary = "List attempts")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/attempts")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'view')")
	public PageResponse<QuizAttemptResultResponse> listAttempts(
			@PathVariable Long quizId,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) QuizAttemptStatus status,
			@RequestParam(required = false) Boolean passed,
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) Long classId,
			@RequestParam(required = false) Long trainingSessionId,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<QuizAttemptResultResponse> attempts = quizResultService.listAttempts(
				quizId,
				status,
				passed,
				userId,
				classId,
				trainingSessionId,
				principal,
				page - 1,
				limit);
		return PageResponse.of(attempts.getContent(), page, limit, attempts.getTotalElements());
	}

	@Operation(summary = "Get attempt detail detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/attempts/{attemptId}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'view')")
	public ApiResponse<QuizAttemptReviewResponse> getAttemptDetail(
			@PathVariable Long quizId,
			@PathVariable Long attemptId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(quizResultService.getAttemptDetail(quizId, attemptId, principal));
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
	@GetMapping("/attempt-summary")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'view')")
	public ApiResponse<QuizAttemptSummaryResponse> summary(
			@PathVariable Long quizId,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) Long classId,
			@RequestParam(required = false) Long trainingSessionId) {
		return ApiResponse.ok(quizResultService.summary(quizId, classId, trainingSessionId, principal));
	}
}
