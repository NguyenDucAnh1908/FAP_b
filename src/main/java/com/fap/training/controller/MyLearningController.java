package com.fap.training.controller;

import com.fap.clazz.dto.ClassResponse;
import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.security.FapUserPrincipal;
import com.fap.training.dto.MyClassDetailResponse;
import com.fap.training.dto.MyClassLearningContentResponse;
import com.fap.training.dto.MyClassProgressResponse;
import com.fap.training.service.MyLearningService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
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

@Tag(name = "My Learning")
@Validated
@RestController
@RequestMapping("/api/v1/me/classes")
public class MyLearningController {

	private final MyLearningService myLearningService;

	public MyLearningController(MyLearningService myLearningService) {
		this.myLearningService = myLearningService;
	}

	@Operation(summary = "Classes")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping
	public PageResponse<ClassResponse> classes(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<ClassResponse> classes = myLearningService.classes(principal.id(), keyword, page - 1, limit);
		return PageResponse.of(classes.getContent(), page, limit, classes.getTotalElements());
	}

	@Operation(summary = "Class Detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{classId}")
	public ApiResponse<MyClassDetailResponse> classDetail(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@PathVariable Long classId) {
		return ApiResponse.ok(myLearningService.classDetail(classId, principal.id()));
	}

	@Operation(summary = "Learning Content")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{classId}/learning-content")
	public ApiResponse<MyClassLearningContentResponse> learningContent(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@PathVariable Long classId,
			@RequestParam(required = false) String keyword) {
		return ApiResponse.ok(myLearningService.learningContent(classId, principal.id(), keyword));
	}

	@Operation(summary = "Progress")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{classId}/progress")
	public ApiResponse<MyClassProgressResponse> progress(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@PathVariable Long classId) {
		return ApiResponse.ok(myLearningService.progress(classId, principal.id()));
	}
}
