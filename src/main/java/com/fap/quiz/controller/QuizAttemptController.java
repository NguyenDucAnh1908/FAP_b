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

@Validated
@RestController
@RequestMapping("/api/v1")
public class QuizAttemptController {

	private final QuizAttemptService quizAttemptService;

	public QuizAttemptController(QuizAttemptService quizAttemptService) {
		this.quizAttemptService = quizAttemptService;
	}

	@GetMapping("/quizzes/assigned")
	public PageResponse<AssignedQuizResponse> assigned(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<AssignedQuizResponse> quizzes = quizAttemptService.assigned(principal.id(), page - 1, limit);
		return PageResponse.of(quizzes.getContent(), page, limit, quizzes.getTotalElements());
	}

	@PostMapping("/quizzes/{quizId}/attempts")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<QuizAttemptResponse> start(
			@PathVariable Long quizId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(quizAttemptService.start(quizId, principal.id()));
	}

	@GetMapping("/quiz-attempts/{attemptId}")
	public ApiResponse<QuizAttemptResponse> get(
			@PathVariable Long attemptId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(quizAttemptService.get(attemptId, principal.id()));
	}

	@PutMapping("/quiz-attempts/{attemptId}/answers")
	public ApiResponse<QuizAttemptResponse> saveAnswers(
			@PathVariable Long attemptId,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody SaveQuizAnswersRequest request) {
		return ApiResponse.ok(quizAttemptService.saveAnswers(attemptId, request, principal.id()));
	}

	@PostMapping("/quiz-attempts/{attemptId}/submit")
	public ApiResponse<QuizAttemptResponse> submit(
			@PathVariable Long attemptId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(quizAttemptService.submit(attemptId, principal.id()));
	}

	@GetMapping("/quiz-attempts/{attemptId}/review")
	public ApiResponse<QuizAttemptReviewResponse> review(
			@PathVariable Long attemptId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(quizAttemptService.review(attemptId, principal.id()));
	}
}
