package com.fap.dashboard.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.exception.ForbiddenException;
import com.fap.common.security.FapUserPrincipal;
import com.fap.dashboard.dto.AdminDashboardResponse;
import com.fap.dashboard.dto.TrainingAnalyticsResponse;
import com.fap.dashboard.service.AdminDashboardService;
import com.fap.dashboard.service.TrainingAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Dashboards")
@RestController
@RequestMapping("/api/v1/me")
public class DashboardController {

	private static final String SUPER_ADMIN_ROLE = "Super Admin";
	private static final String CLASS_ADMIN_ROLE = "Class Admin";

	private final AdminDashboardService adminDashboardService;
	private final TrainingAnalyticsService trainingAnalyticsService;

	public DashboardController(
			AdminDashboardService adminDashboardService,
			TrainingAnalyticsService trainingAnalyticsService) {
		this.adminDashboardService = adminDashboardService;
		this.trainingAnalyticsService = trainingAnalyticsService;
	}

	@Operation(summary = "Get Super Admin system dashboard")
	@GetMapping("/admin-dashboard")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'user', 'full_access')")
	public ApiResponse<AdminDashboardResponse> adminDashboard(
			@AuthenticationPrincipal FapUserPrincipal principal) {
		requireRole(principal, SUPER_ADMIN_ROLE);
		return ApiResponse.ok(adminDashboardService.getDashboard());
	}

	@Operation(summary = "Get date-filtered training analytics")
	@GetMapping("/training-analytics")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<TrainingAnalyticsResponse> trainingAnalytics(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
		Long classAdminId = analyticsScope(principal);
		return ApiResponse.ok(trainingAnalyticsService.getAnalytics(classAdminId, fromDate, toDate));
	}

	private Long analyticsScope(FapUserPrincipal principal) {
		if (principal.roles().contains(SUPER_ADMIN_ROLE)) {
			return null;
		}
		if (principal.roles().contains(CLASS_ADMIN_ROLE)) {
			return principal.id();
		}
		throw new ForbiddenException("Training analytics is available to administrators only");
	}

	private void requireRole(FapUserPrincipal principal, String role) {
		if (!principal.roles().contains(role)) {
			throw new ForbiddenException("This dashboard is available to Super Admin only");
		}
	}
}
