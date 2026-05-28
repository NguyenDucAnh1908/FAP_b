package com.fap.notification.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.security.FapUserPrincipal;
import com.fap.notification.dto.NotificationResponse;
import com.fap.notification.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public PageResponse<NotificationResponse> list(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<NotificationResponse> notifications = notificationService.listMyNotifications(principal.id(), page - 1, limit);
		return PageResponse.of(notifications.getContent(), page, limit, notifications.getTotalElements());
	}

	@PatchMapping("/{id}/read")
	public ApiResponse<NotificationResponse> markRead(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(notificationService.markRead(id, principal.id()));
	}
}
