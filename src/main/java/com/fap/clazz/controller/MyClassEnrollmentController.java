package com.fap.clazz.controller;

import com.fap.clazz.dto.ClassEnrollmentResponse;
import com.fap.clazz.dto.ClassResponse;
import com.fap.clazz.enums.ClassEnrollmentStatus;
import com.fap.clazz.service.ClassEnrollmentService;
import com.fap.common.api.PageResponse;
import com.fap.common.security.FapUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "My Class Enrollments")
@Validated
@RestController
@RequestMapping("/api/v1/me")
public class MyClassEnrollmentController {

	private final ClassEnrollmentService classEnrollmentService;

	public MyClassEnrollmentController(ClassEnrollmentService classEnrollmentService) {
		this.classEnrollmentService = classEnrollmentService;
	}

	@Operation(summary = "List classes open for self enrollment")
	@GetMapping("/available-classes")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public PageResponse<ClassResponse> availableClasses(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String order) {
		Page<ClassResponse> classes = classEnrollmentService.availableClasses(
				principal.id(), keyword, page - 1, limit, sortBy, order);
		return PageResponse.of(classes.getContent(), page, limit, classes.getTotalElements());
	}

	@Operation(summary = "List current trainee class enrollments")
	@GetMapping("/class-enrollments")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public PageResponse<ClassEnrollmentResponse> myEnrollments(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) ClassEnrollmentStatus status,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String order) {
		Page<ClassEnrollmentResponse> enrollments = classEnrollmentService.listMine(
				principal.id(), status, keyword, page - 1, limit, sortBy, order);
		return PageResponse.of(enrollments.getContent(), page, limit, enrollments.getTotalElements());
	}
}
