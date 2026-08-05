package com.fap.training.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.security.FapUserPrincipal;
import com.fap.clazz.service.ClassAccessService;
import com.fap.training.dto.AttendanceRecordResponse;
import com.fap.training.dto.CreateTrainingSessionRequest;
import com.fap.training.dto.TrainingParticipantsResponse;
import com.fap.training.dto.TrainingRegistrationResponse;
import com.fap.training.dto.TrainingSessionResponse;
import com.fap.training.dto.UpdateAttendanceRequest;
import com.fap.training.dto.UpdateTrainingSessionRequest;
import com.fap.training.dto.UpdateTrainingSessionStatusRequest;
import com.fap.training.enums.TrainingSessionStatus;
import com.fap.training.service.AttendanceService;
import com.fap.training.service.TrainingRegistrationService;
import com.fap.training.service.TrainingSessionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Training Sessions")
@Validated
@RestController
@RequestMapping("/api/v1/training-sessions")
public class TrainingSessionController {

	private final TrainingSessionService trainingSessionService;
	private final TrainingRegistrationService trainingRegistrationService;
	private final AttendanceService attendanceService;
	private final ClassAccessService classAccessService;

	public TrainingSessionController(
			TrainingSessionService trainingSessionService,
			TrainingRegistrationService trainingRegistrationService,
			AttendanceService attendanceService,
			ClassAccessService classAccessService) {
		this.trainingSessionService = trainingSessionService;
		this.trainingRegistrationService = trainingRegistrationService;
		this.attendanceService = attendanceService;
		this.classAccessService = classAccessService;
	}

	@Operation(summary = "List training sessions")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public PageResponse<TrainingSessionResponse> list(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) TrainingSessionStatus status,
			@RequestParam(required = false) Long classId,
			@RequestParam(required = false) Long trainerId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
			@RequestParam(required = false) String sortBy,
			@RequestParam(required = false) String order) {
		Page<TrainingSessionResponse> sessions = trainingSessionService.listScoped(
				principal,
				status,
				classId,
				trainerId,
				fromDate,
				toDate,
				keyword,
				page - 1,
				limit,
				sortBy,
				order);
		return PageResponse.of(sessions.getContent(), page, limit, sessions.getTotalElements());
	}

	@Operation(summary = "Create training sessions")
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
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public ApiResponse<TrainingSessionResponse> create(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateTrainingSessionRequest request) {
		classAccessService.assertCanManageClass(principal, request.classId());
		return ApiResponse.ok(trainingSessionService.create(request, principal.id()));
	}

	@Operation(summary = "Get training sessions detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<TrainingSessionResponse> get(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanViewSession(principal, id);
		return ApiResponse.ok(trainingSessionService.get(id));
	}

	@Operation(summary = "Update training sessions")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public ApiResponse<TrainingSessionResponse> update(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateTrainingSessionRequest request) {
		classAccessService.assertCanManageSession(principal, id);
		return ApiResponse.ok(trainingSessionService.update(id, request, principal.id()));
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
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public ApiResponse<TrainingSessionResponse> updateStatus(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateTrainingSessionStatusRequest request) {
		classAccessService.assertCanManageSession(principal, id);
		return ApiResponse.ok(trainingSessionService.updateStatus(id, request.status(), principal.id()));
	}

	@Operation(summary = "Register")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/{id}/registrations")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<TrainingRegistrationResponse> register(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(trainingRegistrationService.register(id, principal.id()));
	}

	@Operation(summary = "Cancel My Registration")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@DeleteMapping("/{id}/registrations/me")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<TrainingRegistrationResponse> cancelMyRegistration(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		return ApiResponse.ok(trainingRegistrationService.cancelSelf(id, principal.id()));
	}

	@Operation(summary = "Participants")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}/participants")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<TrainingParticipantsResponse> participants(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanViewSession(principal, id);
		return ApiResponse.ok(trainingRegistrationService.participants(id));
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
	@GetMapping("/{id}/attendance")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<List<AttendanceRecordResponse>> attendance(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanViewSession(principal, id);
		return ApiResponse.ok(attendanceService.list(id));
	}

	@Operation(summary = "Upsert Attendance")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/attendance")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public ApiResponse<List<AttendanceRecordResponse>> upsertAttendance(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateAttendanceRequest request) {
		classAccessService.assertCanManageSession(principal, id);
		return ApiResponse.ok(attendanceService.upsert(id, request, principal.id()));
	}
}
