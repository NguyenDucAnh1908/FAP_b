package com.fap.auth.controller;

import com.fap.auth.dto.AuthResponse;
import com.fap.auth.dto.ChangePasswordRequest;
import com.fap.auth.dto.ForgotPasswordRequest;
import com.fap.auth.dto.GoogleLoginRequest;
import com.fap.auth.dto.LoginRequest;
import com.fap.auth.dto.LogoutRequest;
import com.fap.auth.dto.RefreshTokenRequest;
import com.fap.auth.dto.ResetPasswordRequest;
import com.fap.auth.service.AuthService;
import com.fap.common.api.ApiResponse;
import com.fap.common.i18n.MessageService;
import com.fap.common.security.FapUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final MessageService messageService;

	public AuthController(AuthService authService, MessageService messageService) {
		this.authService = authService;
		this.messageService = messageService;
	}

	@Operation(summary = "Authenticate with email and password")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok(authService.login(request));
	}

	@Operation(summary = "Authenticate with Google OAuth token")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/google")
	public ApiResponse<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
		return ApiResponse.ok(authService.googleLogin(request.idToken()));
	}

	@Operation(summary = "Refresh access token")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/refresh")
	public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.ok(authService.refresh(request.refreshToken()));
	}

	@Operation(summary = "Request password reset OTP")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/forgot-password")
	public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.forgotPassword(request);
		return ApiResponse.ok(null, messageService.get("success.auth.forgot_password_requested"));
	}

	@Operation(summary = "Reset password using OTP")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/reset-password")
	public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request);
		return ApiResponse.ok(null, messageService.get("success.auth.password_reset"));
	}

	@Operation(summary = "Logout and revoke refresh token")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
		authService.logout(request.refreshToken());
		return ResponseEntity.ok(ApiResponse.ok(null, messageService.get("success.auth.logged_out")));
	}

	@Operation(summary = "Change current user password")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PatchMapping("/change-password")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<Void> changePassword(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(principal.id(), request);
		return ApiResponse.ok(null, messageService.get("success.auth.password_changed"));
	}
}
