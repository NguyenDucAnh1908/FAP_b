package com.fap.syllabus.controller;

import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.i18n.MessageService;
import com.fap.common.security.FapUserPrincipal;
import com.fap.syllabus.dto.CreateMaterialFileRequest;
import com.fap.syllabus.dto.CreateSyllabusRequest;
import com.fap.syllabus.dto.CreateSyllabusDayRequest;
import com.fap.syllabus.dto.CreateSyllabusTopicRequest;
import com.fap.syllabus.dto.CreateSyllabusUnitRequest;
import com.fap.syllabus.dto.MaterialFileResponse;
import com.fap.syllabus.dto.SyllabusDayResponse;
import com.fap.syllabus.dto.SyllabusResponse;
import com.fap.syllabus.dto.SyllabusTopicResponse;
import com.fap.syllabus.dto.SyllabusUnitResponse;
import com.fap.syllabus.dto.UpdateSyllabusOutputStandardsRequest;
import com.fap.syllabus.dto.UpdateSyllabusRequest;
import com.fap.syllabus.dto.UpdateSyllabusStatusRequest;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.service.MaterialFileService;
import com.fap.syllabus.service.SyllabusOutlineService;
import com.fap.syllabus.service.SyllabusOutputStandardService;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/syllabuses")
public class SyllabusController {

	private final SyllabusService syllabusService;
	private final SyllabusOutlineService outlineService;
	private final SyllabusOutputStandardService outputStandardService;
	private final MaterialFileService materialFileService;
	private final MessageService messageService;

	public SyllabusController(
			SyllabusService syllabusService,
			SyllabusOutlineService outlineService,
			SyllabusOutputStandardService outputStandardService,
			MaterialFileService materialFileService,
			MessageService messageService) {
		this.syllabusService = syllabusService;
		this.outlineService = outlineService;
		this.outputStandardService = outputStandardService;
		this.materialFileService = materialFileService;
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

	@GetMapping("/{id}/outline")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'view')")
	public ApiResponse<List<SyllabusDayResponse>> getOutline(@PathVariable Long id) {
		return ApiResponse.ok(outlineService.getOutline(id));
	}

	@GetMapping("/{id}/output-standards")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'view')")
	public ApiResponse<List<String>> getOutputStandards(@PathVariable Long id) {
		return ApiResponse.ok(outputStandardService.list(id));
	}

	@PutMapping("/{id}/output-standards")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<List<String>> replaceOutputStandards(
			@PathVariable Long id,
			@Valid @RequestBody UpdateSyllabusOutputStandardsRequest request) {
		return ApiResponse.ok(outputStandardService.replace(id, request));
	}

	@PostMapping("/{id}/days")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusDayResponse> createDay(
			@PathVariable Long id,
			@Valid @RequestBody CreateSyllabusDayRequest request) {
		return ApiResponse.ok(outlineService.createDay(id, request));
	}

	@PutMapping("/{id}/days/{dayId}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusDayResponse> updateDay(
			@PathVariable Long id,
			@PathVariable Long dayId,
			@Valid @RequestBody CreateSyllabusDayRequest request) {
		return ApiResponse.ok(outlineService.updateDay(id, dayId, request));
	}

	@DeleteMapping("/{id}/days/{dayId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void deleteDay(@PathVariable Long id, @PathVariable Long dayId) {
		outlineService.deleteDay(id, dayId);
	}

	@PostMapping("/{id}/days/{dayId}/units")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusUnitResponse> createUnit(
			@PathVariable Long id,
			@PathVariable Long dayId,
			@Valid @RequestBody CreateSyllabusUnitRequest request) {
		return ApiResponse.ok(outlineService.createUnit(id, dayId, request));
	}

	@PutMapping("/{id}/units/{unitId}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusUnitResponse> updateUnit(
			@PathVariable Long id,
			@PathVariable Long unitId,
			@Valid @RequestBody CreateSyllabusUnitRequest request) {
		return ApiResponse.ok(outlineService.updateUnit(id, unitId, request));
	}

	@DeleteMapping("/{id}/units/{unitId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void deleteUnit(@PathVariable Long id, @PathVariable Long unitId) {
		outlineService.deleteUnit(id, unitId);
	}

	@PostMapping("/{id}/units/{unitId}/topics")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusTopicResponse> createTopic(
			@PathVariable Long id,
			@PathVariable Long unitId,
			@Valid @RequestBody CreateSyllabusTopicRequest request) {
		return ApiResponse.ok(outlineService.createTopic(id, unitId, request));
	}

	@PutMapping("/{id}/topics/{topicId}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusTopicResponse> updateTopic(
			@PathVariable Long id,
			@PathVariable Long topicId,
			@Valid @RequestBody CreateSyllabusTopicRequest request) {
		return ApiResponse.ok(outlineService.updateTopic(id, topicId, request));
	}

	@DeleteMapping("/{id}/topics/{topicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void deleteTopic(@PathVariable Long id, @PathVariable Long topicId) {
		outlineService.deleteTopic(id, topicId);
	}

	@GetMapping("/{id}/topics/{topicId}/materials")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'view')")
	public ApiResponse<List<MaterialFileResponse>> listMaterials(
			@PathVariable Long id,
			@PathVariable Long topicId) {
		return ApiResponse.ok(materialFileService.list(id, topicId));
	}

	@PostMapping("/{id}/topics/{topicId}/materials")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<MaterialFileResponse> createMaterial(
			@PathVariable Long id,
			@PathVariable Long topicId,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateMaterialFileRequest request) {
		return ApiResponse.ok(materialFileService.create(id, topicId, request, principal.id()));
	}

	@DeleteMapping("/{id}/topics/{topicId}/materials/{materialId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void deleteMaterial(
			@PathVariable Long id,
			@PathVariable Long topicId,
			@PathVariable Long materialId) {
		materialFileService.delete(id, topicId, materialId);
	}
}
