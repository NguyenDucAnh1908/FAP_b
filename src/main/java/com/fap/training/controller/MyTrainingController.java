package com.fap.training.controller;

import com.fap.common.api.PageResponse;
import com.fap.common.security.FapUserPrincipal;
import com.fap.training.dto.MyAttendanceResponse;
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

@Validated
@RestController
@RequestMapping("/api/v1/me")
public class MyTrainingController {

	private final MyTrainingService myTrainingService;

	public MyTrainingController(MyTrainingService myTrainingService) {
		this.myTrainingService = myTrainingService;
	}

	@GetMapping("/training-registrations")
	public PageResponse<MyTrainingRegistrationResponse> registrations(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) TrainingRegistrationStatus registrationStatus,
			@RequestParam(required = false) TrainingSessionStatus sessionStatus,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<MyTrainingRegistrationResponse> registrations = myTrainingService.registrations(
				principal.id(),
				registrationStatus,
				sessionStatus,
				fromDate,
				toDate,
				keyword,
				page - 1,
				limit);
		return PageResponse.of(registrations.getContent(), page, limit, registrations.getTotalElements());
	}

	@GetMapping("/training-sessions")
	public PageResponse<MyTrainingSessionResponse> sessions(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) TrainingRegistrationStatus registrationStatus,
			@RequestParam(required = false) TrainingSessionStatus sessionStatus,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<MyTrainingSessionResponse> sessions = myTrainingService.sessions(
				principal.id(),
				registrationStatus,
				sessionStatus,
				fromDate,
				toDate,
				keyword,
				page - 1,
				limit);
		return PageResponse.of(sessions.getContent(), page, limit, sessions.getTotalElements());
	}

	@GetMapping("/attendance")
	public PageResponse<MyAttendanceResponse> attendance(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) AttendanceStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<MyAttendanceResponse> attendance = myTrainingService.attendance(
				principal.id(),
				status,
				fromDate,
				toDate,
				keyword,
				page - 1,
				limit);
		return PageResponse.of(attendance.getContent(), page, limit, attendance.getTotalElements());
	}
}
