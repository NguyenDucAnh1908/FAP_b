package com.fap.quiz.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.security.FapUserPrincipal;
import com.fap.quiz.dto.CreateQuizRequest;
import com.fap.quiz.dto.CreateQuizAssignmentRequest;
import com.fap.quiz.dto.QuizAssignmentResponse;
import com.fap.quiz.dto.QuizQuestionResponse;
import com.fap.quiz.dto.QuizResponse;
import com.fap.quiz.dto.UpdateQuizQuestionsRequest;
import com.fap.quiz.dto.UpdateQuizRequest;
import com.fap.quiz.dto.UpdateQuizStatusRequest;
import com.fap.quiz.enums.QuizStatus;
import com.fap.quiz.service.QuizAssignmentService;
import com.fap.quiz.service.QuizService;
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

@Tag(name = "Quizzes")
@Validated
@RestController
@RequestMapping("/api/v1/quizzes")
public class QuizController {

	private final QuizService quizService;
	private final QuizAssignmentService quizAssignmentService;

	public QuizController(QuizService quizService, QuizAssignmentService quizAssignmentService) {
		this.quizService = quizService;
		this.quizAssignmentService = quizAssignmentService;
	}

	@Operation(summary = "List quizzes")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'view')")
	public PageResponse<QuizResponse> list(
			@RequestParam(required = false) QuizStatus status,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<QuizResponse> quizzes = quizService.list(status, category, keyword, page - 1, limit);
		return PageResponse.of(quizzes.getContent(), page, limit, quizzes.getTotalElements());
	}

	@Operation(summary = "Create quizzes")
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
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'create')")
	public ApiResponse<QuizResponse> create(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateQuizRequest request) {
		return ApiResponse.ok(quizService.create(request, principal.id()));
	}

	@Operation(summary = "Get quizzes detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'view')")
	public ApiResponse<QuizResponse> get(@PathVariable Long id) {
		return ApiResponse.ok(quizService.get(id));
	}

	@Operation(summary = "Update quizzes")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'modify')")
	public ApiResponse<QuizResponse> update(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateQuizRequest request) {
		return ApiResponse.ok(quizService.update(id, request, principal.id()));
	}

	@Operation(summary = "Delete quizzes")
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
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'modify')")
	public void delete(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		quizService.delete(id, principal.id());
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
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'modify')")
	public ApiResponse<QuizResponse> updateStatus(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateQuizStatusRequest request) {
		return ApiResponse.ok(quizService.updateStatus(id, request, principal.id()));
	}

	@Operation(summary = "List questions")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}/questions")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'view')")
	public ApiResponse<List<QuizQuestionResponse>> listQuestions(@PathVariable Long id) {
		return ApiResponse.ok(quizService.listQuestions(id));
	}

	@Operation(summary = "Replace questions")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/questions")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'modify')")
	public ApiResponse<List<QuizQuestionResponse>> replaceQuestions(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateQuizQuestionsRequest request) {
		return ApiResponse.ok(quizService.replaceQuestions(id, request, principal.id()));
	}

	@Operation(summary = "List assignments")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}/assignments")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'view')")
	public ApiResponse<List<QuizAssignmentResponse>> listAssignments(@PathVariable Long id) {
		return ApiResponse.ok(quizAssignmentService.list(id));
	}

	@Operation(summary = "Assign")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/{id}/assignments")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'modify')")
	public ApiResponse<QuizAssignmentResponse> assign(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateQuizAssignmentRequest request) {
		return ApiResponse.ok(quizAssignmentService.assign(id, request, principal.id()));
	}

	@Operation(summary = "Delete assignment")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No content"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@DeleteMapping("/{id}/assignments/{assignmentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'quiz', 'modify')")
	public void deleteAssignment(
			@PathVariable Long id,
			@PathVariable Long assignmentId) {
		quizAssignmentService.delete(id, assignmentId);
	}
}
