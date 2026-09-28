package com.fap.syllabus.controller;

import com.fap.common.api.ApiResponse;
import com.fap.syllabus.dto.CreateSyllabusDayRequest;
import com.fap.syllabus.dto.CreateSyllabusTopicRequest;
import com.fap.syllabus.dto.CreateSyllabusUnitRequest;
import com.fap.syllabus.dto.SyllabusDayResponse;
import com.fap.syllabus.dto.SyllabusTopicResponse;
import com.fap.syllabus.dto.SyllabusUnitResponse;
import com.fap.syllabus.service.SyllabusOutlineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Syllabus")
@Validated
@RestController
@RequestMapping("/api/v1/syllabuses")
public class SyllabusOutlineController {

	private final SyllabusOutlineService outlineService;

	public SyllabusOutlineController(SyllabusOutlineService outlineService) {
		this.outlineService = outlineService;
	}

	@Operation(summary = "Get outline detail")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}/outline")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'view')")
	public ApiResponse<List<SyllabusDayResponse>> getOutline(@PathVariable Long id) {
		return ApiResponse.ok(outlineService.getOutline(id));
	}

	@Operation(summary = "Create day")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/{id}/days")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusDayResponse> createDay(
			@PathVariable Long id,
			@Valid @RequestBody CreateSyllabusDayRequest request) {
		return ApiResponse.ok(outlineService.createDay(id, request));
	}

	@Operation(summary = "Update day")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/days/{dayId}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusDayResponse> updateDay(
			@PathVariable Long id,
			@PathVariable Long dayId,
			@Valid @RequestBody CreateSyllabusDayRequest request) {
		return ApiResponse.ok(outlineService.updateDay(id, dayId, request));
	}

	@Operation(summary = "Delete day")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No content"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@DeleteMapping("/{id}/days/{dayId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void deleteDay(@PathVariable Long id, @PathVariable Long dayId) {
		outlineService.deleteDay(id, dayId);
	}

	@Operation(summary = "Create unit")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/{id}/days/{dayId}/units")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusUnitResponse> createUnit(
			@PathVariable Long id,
			@PathVariable Long dayId,
			@Valid @RequestBody CreateSyllabusUnitRequest request) {
		return ApiResponse.ok(outlineService.createUnit(id, dayId, request));
	}

	@Operation(summary = "Update unit")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/units/{unitId}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusUnitResponse> updateUnit(
			@PathVariable Long id,
			@PathVariable Long unitId,
			@Valid @RequestBody CreateSyllabusUnitRequest request) {
		return ApiResponse.ok(outlineService.updateUnit(id, unitId, request));
	}

	@Operation(summary = "Delete unit")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No content"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@DeleteMapping("/{id}/units/{unitId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void deleteUnit(@PathVariable Long id, @PathVariable Long unitId) {
		outlineService.deleteUnit(id, unitId);
	}

	@Operation(summary = "Create topic")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PostMapping("/{id}/units/{unitId}/topics")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusTopicResponse> createTopic(
			@PathVariable Long id,
			@PathVariable Long unitId,
			@Valid @RequestBody CreateSyllabusTopicRequest request) {
		return ApiResponse.ok(outlineService.createTopic(id, unitId, request));
	}

	@Operation(summary = "Update topic")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/topics/{topicId}")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public ApiResponse<SyllabusTopicResponse> updateTopic(
			@PathVariable Long id,
			@PathVariable Long topicId,
			@Valid @RequestBody CreateSyllabusTopicRequest request) {
		return ApiResponse.ok(outlineService.updateTopic(id, topicId, request));
	}

	@Operation(summary = "Delete topic")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No content"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@DeleteMapping("/{id}/topics/{topicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'syllabus', 'modify')")
	public void deleteTopic(@PathVariable Long id, @PathVariable Long topicId) {
		outlineService.deleteTopic(id, topicId);
	}
}
