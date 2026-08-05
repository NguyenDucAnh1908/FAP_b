package com.fap.training.controller;

import com.fap.common.api.PageResponse;
import com.fap.common.api.ApiResponse;
import com.fap.common.security.FapUserPrincipal;
import com.fap.training.dto.MyAttendanceResponse;
import com.fap.training.dto.MyClassAdminDashboardResponse;
import com.fap.training.dto.MyTrainerDashboardResponse;
import com.fap.training.dto.MyTrainingDashboardResponse;
import com.fap.training.dto.MyTrainingRegistrationResponse;
import com.fap.training.dto.MyTrainingSessionResponse;
import com.fap.training.enums.AttendanceStatus;
import com.fap.training.enums.TrainingRegistrationStatus;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.service.MyTrainingService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "My Training")
@Validated
@RestController
@RequestMapping("/api/v1/me")
public class MyTrainingController {

	private final MyTrainingService myTrainingService;

	public MyTrainingController(MyTrainingService myTrainingService) {
		this.myTrainingService = myTrainingService;
	}

	@Operation(summary = "Get current trainee learning dashboard")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/training-dashboard")
	public ApiResponse<MyTrainingDashboardResponse> dashboard(
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(myTrainingService.dashboard(principal.id()));
	}

	@Operation(summary = "Get current trainer teaching dashboard")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/trainer-dashboard")
	public ApiResponse<MyTrainerDashboardResponse> trainerDashboard(
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(myTrainingService.trainerDashboard(principal.id()));
	}

	@Operation(summary = "Get current class admin dashboard")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/class-admin-dashboard")
	public ApiResponse<MyClassAdminDashboardResponse> classAdminDashboard(
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(myTrainingService.classAdminDashboard(principal.id()));
	}

	@Operation(summary = "Registrations")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/training-registrations")
	public PageResponse<MyTrainingRegistrationResponse> registrations(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) TrainingRegistrationStatus registrationStatus,
			@RequestParam(required = false) TrainingSessionStatus sessionStatus,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String order) {
		Page<MyTrainingRegistrationResponse> registrations = myTrainingService.registrations(
				principal.id(),
				registrationStatus,
				sessionStatus,
				fromDate,
				toDate,
				keyword,
				page - 1,
				limit,
				sortBy,
				order);
		return PageResponse.of(registrations.getContent(), page, limit, registrations.getTotalElements());
	}

	@Operation(summary = "Sessions")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/training-sessions")
	public PageResponse<MyTrainingSessionResponse> sessions(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) TrainingRegistrationStatus registrationStatus,
			@RequestParam(required = false) TrainingSessionStatus sessionStatus,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String order) {
		Page<MyTrainingSessionResponse> sessions = myTrainingService.sessions(
				principal.id(),
				registrationStatus,
				sessionStatus,
				fromDate,
				toDate,
				keyword,
				page - 1,
				limit,
				sortBy,
				order);
		return PageResponse.of(sessions.getContent(), page, limit, sessions.getTotalElements());
	}

	@Operation(summary = "Attendance")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/attendance")
	public PageResponse<MyAttendanceResponse> attendance(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) AttendanceStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String order) {
		Page<MyAttendanceResponse> attendance = myTrainingService.attendance(
				principal.id(),
				status,
				fromDate,
				toDate,
				keyword,
				page - 1,
				limit,
				sortBy,
				order);
		return PageResponse.of(attendance.getContent(), page, limit, attendance.getTotalElements());
	}
}
