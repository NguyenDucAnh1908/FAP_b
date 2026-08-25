package com.fap.result.controller;

import com.fap.clazz.service.ClassAccessService;
import com.fap.common.api.ApiResponse;
import com.fap.common.security.FapUserPrincipal;
import com.fap.result.dto.ClassCourseResultsResponse;
import com.fap.result.dto.CompletionPolicyResponse;
import com.fap.result.dto.CourseResultResponse;
import com.fap.result.dto.UpdateCompletionPolicyRequest;
import com.fap.result.dto.UpdateCourseResultRequest;
import com.fap.result.service.CourseResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Course Results")
@RestController
@RequestMapping("/api/v1")
public class CourseResultController {
	private final CourseResultService courseResultService;
	private final ClassAccessService classAccessService;

	public CourseResultController(CourseResultService courseResultService, ClassAccessService classAccessService) {
		this.courseResultService = courseResultService;
		this.classAccessService = classAccessService;
	}

	@Operation(summary = "Get class completion policy")
	@GetMapping("/classes/{classId}/completion-policy")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<CompletionPolicyResponse> getPolicy(
			@PathVariable Long classId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanViewEnrollmentRoster(principal, classId);
		return ApiResponse.ok(courseResultService.getPolicy(classId));
	}

	@Operation(summary = "Update class completion policy")
	@PutMapping("/classes/{classId}/completion-policy")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public ApiResponse<CompletionPolicyResponse> updatePolicy(
			@PathVariable Long classId,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateCompletionPolicyRequest request) {
		classAccessService.assertCanManageClass(principal, classId);
		return ApiResponse.ok(courseResultService.updatePolicy(classId, request, principal.id()));
	}

	@Operation(summary = "List class course results")
	@GetMapping("/classes/{classId}/results")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<ClassCourseResultsResponse> list(
			@PathVariable Long classId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanViewEnrollmentRoster(principal, classId);
		return ApiResponse.ok(courseResultService.list(classId));
	}

	@Operation(summary = "Get trainee course result")
	@GetMapping("/classes/{classId}/results/{userId}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<CourseResultResponse> get(
			@PathVariable Long classId,
			@PathVariable Long userId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanViewEnrollmentRoster(principal, classId);
		return ApiResponse.ok(courseResultService.get(classId, userId));
	}

	@Operation(summary = "Calculate class course results")
	@PostMapping("/classes/{classId}/results/calculate")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public ApiResponse<ClassCourseResultsResponse> calculate(
			@PathVariable Long classId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanManageClass(principal, classId);
		return ApiResponse.ok(courseResultService.calculate(classId, principal.id()));
	}

	@Operation(summary = "Adjust trainee course result")
	@PatchMapping("/classes/{classId}/results/{userId}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public ApiResponse<CourseResultResponse> adjust(
			@PathVariable Long classId,
			@PathVariable Long userId,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateCourseResultRequest request) {
		classAccessService.assertCanManageClass(principal, classId);
		return ApiResponse.ok(courseResultService.adjust(classId, userId, request, principal.id()));
	}

	@Operation(summary = "Publish class course results")
	@PostMapping("/classes/{classId}/results/publish")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public ApiResponse<ClassCourseResultsResponse> publish(
			@PathVariable Long classId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanManageClass(principal, classId);
		return ApiResponse.ok(courseResultService.publish(classId, principal.id()));
	}

	@Operation(summary = "Get current trainee published course result")
	@GetMapping("/me/classes/{classId}/result")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<CourseResultResponse> getMine(
			@PathVariable Long classId,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(courseResultService.getMine(classId, principal.id()));
	}
}
