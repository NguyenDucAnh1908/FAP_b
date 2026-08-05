package com.fap.quiz.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.security.FapUserPrincipal;
import com.fap.quiz.dto.AssignedQuizResponse;
import com.fap.quiz.dto.QuizAttemptReviewResponse;
import com.fap.quiz.dto.QuizAttemptResponse;
import com.fap.quiz.dto.SaveQuizAnswersRequest;
import com.fap.quiz.service.QuizAttemptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Quiz Attempts")
@Validated
@RestController
@RequestMapping("/api/v1")
public class QuizAttemptController {

	private final QuizAttemptService quizAttemptService;

	public QuizAttemptController(QuizAttemptService quizAttemptService) {
		this.quizAttemptService = quizAttemptService;
	}

	@Operation(summary = "List assigned quizzes for current trainee")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/quizzes/assigned")
	public PageResponse<AssignedQuizResponse> assigned(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String order) {
		Page<AssignedQuizResponse> quizzes = quizAttemptService.assigned(principal.id(), page - 1, limit, sortBy, order);
		return PageResponse.of(quizzes.getContent(), page, limit, quizzes.getTotalElements());
	}

	@Operation(summary = "Start quiz attempt")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/quizzes/{quizId}/attempts")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<QuizAttemptResponse> start(
			@PathVariable Long quizId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(quizAttemptService.start(quizId, principal.id()));
	}

	@Operation(summary = "Get quiz attempts detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/quiz-attempts/{attemptId}")
	public ApiResponse<QuizAttemptResponse> get(
			@PathVariable Long attemptId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(quizAttemptService.get(attemptId, principal.id()));
	}

	@Operation(summary = "Save quiz attempt answers")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/quiz-attempts/{attemptId}/answers")
	public ApiResponse<QuizAttemptResponse> saveAnswers(
			@PathVariable Long attemptId,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody SaveQuizAnswersRequest request) {
		return ApiResponse.ok(quizAttemptService.saveAnswers(attemptId, request, principal.id()));
	}

	@Operation(summary = "Submit quiz attempt")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/quiz-attempts/{attemptId}/submit")
	public ApiResponse<QuizAttemptResponse> submit(
			@PathVariable Long attemptId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(quizAttemptService.submit(attemptId, principal.id()));
	}

	@Operation(summary = "Review submitted quiz attempt")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/quiz-attempts/{attemptId}/review")
	public ApiResponse<QuizAttemptReviewResponse> review(
			@PathVariable Long attemptId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(quizAttemptService.review(attemptId, principal.id()));
	}
}
