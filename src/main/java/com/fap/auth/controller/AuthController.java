package com.fap.auth.controller;

import com.fap.auth.dto.AuthResponse;
import com.fap.auth.dto.GoogleLoginRequest;
import com.fap.auth.dto.LoginRequest;
import com.fap.auth.dto.LogoutRequest;
import com.fap.auth.dto.RefreshTokenRequest;
import com.fap.auth.service.AuthService;
import com.fap.common.api.ApiResponse;
import com.fap.common.i18n.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final MessageService messageService;

	public AuthController(AuthService authService, MessageService messageService) {
		this.authService = authService;
		this.messageService = messageService;
	}

	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok(authService.login(request));
	}

	@PostMapping("/google")
	public ApiResponse<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
		return ApiResponse.ok(authService.googleLogin(request.idToken()));
	}

	@PostMapping("/refresh")
	public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.ok(authService.refresh(request.refreshToken()));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request.refreshToken());
		return ResponseEntity.ok(ApiResponse.ok(null, messageService.get("success.auth.logged_out")));
	}
}
