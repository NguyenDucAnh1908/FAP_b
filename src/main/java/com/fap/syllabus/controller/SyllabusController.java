package com.fap.syllabus.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.i18n.MessageService;
import com.fap.common.security.FapUserPrincipal;
import com.fap.syllabus.dto.CreateSyllabusRequest;
import com.fap.syllabus.dto.SyllabusResponse;
import com.fap.syllabus.dto.UpdateSyllabusRequest;
import com.fap.syllabus.dto.UpdateSyllabusStatusRequest;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.service.SyllabusService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
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

@Validated
@RestController
@RequestMapping("/api/v1/syllabuses")
public class SyllabusController {

	private final SyllabusService syllabusService;
	private final MessageService messageService;

	public SyllabusController(SyllabusService syllabusService, MessageService messageService) {
		this.syllabusService = syllabusService;
		this.messageService = messageService;
	}

	@GetMapping
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'view')")
	public PageResponse<SyllabusResponse> list(
			@RequestParam(required = false) SyllabusStatus status,
			@RequestParam(required = false) String levelName,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<SyllabusResponse> syllabuses = syllabusService.list(status, levelName, keyword, page - 1, limit);
		return PageResponse.of(syllabuses.getContent(), page, limit, syllabuses.getTotalElements());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'create')")
	public ApiResponse<SyllabusResponse> create(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateSyllabusRequest request) {
		return ApiResponse.ok(
				syllabusService.create(request, principal.id()),
				messageService.get("success.syllabus.created"));
	}

	@GetMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'view')")
	public ApiResponse<SyllabusResponse> get(@PathVariable Long id) {
		return ApiResponse.ok(syllabusService.get(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusResponse> update(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateSyllabusRequest request) {
		return ApiResponse.ok(syllabusService.update(id, request, principal.id()));
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusResponse> updateStatus(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateSyllabusStatusRequest request) {
		return ApiResponse.ok(syllabusService.updateStatus(id, request.status(), principal.id()));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void delete(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		syllabusService.delete(id, principal.id());
	}
}
