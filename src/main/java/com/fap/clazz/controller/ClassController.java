package com.fap.clazz.controller;

import com.fap.clazz.dto.ClassAdminResponse;
import com.fap.clazz.dto.ClassResponse;
import com.fap.clazz.dto.ClassTrainerResponse;
import com.fap.clazz.dto.CreateClassRequest;
import com.fap.clazz.dto.UpdateClassAdminsRequest;
import com.fap.clazz.dto.UpdateClassTrainersRequest;
import com.fap.clazz.dto.UpdateClassRequest;
import com.fap.clazz.dto.UpdateClassStatusRequest;
import com.fap.clazz.enums.ClassStatus;
import com.fap.clazz.service.ClassAccessService;
import com.fap.clazz.service.ClassAdminService;
import com.fap.clazz.service.ClassService;
import com.fap.clazz.service.ClassTrainerService;
import com.fap.common.api.ApiResponse;
import com.fap.common.api.PageResponse;
import com.fap.common.i18n.MessageService;
import com.fap.common.security.FapUserPrincipal;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Classes")
@Validated
@RestController
@RequestMapping("/api/v1/classes")
public class ClassController {

	private final ClassService classService;
	private final ClassAccessService classAccessService;
	private final ClassAdminService classAdminService;
	private final ClassTrainerService classTrainerService;
	private final MessageService messageService;

	public ClassController(
			ClassService classService,
			ClassAccessService classAccessService,
			ClassAdminService classAdminService,
			ClassTrainerService classTrainerService,
			MessageService messageService) {
		this.classService = classService;
		this.classAccessService = classAccessService;
		this.classAdminService = classAdminService;
		this.classTrainerService = classTrainerService;
		this.messageService = messageService;
	}

	@Operation(summary = "List classes")
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
	public PageResponse<ClassResponse> list(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@RequestParam(required = false) ClassStatus status,
			@RequestParam(required = false) Long trainingProgramId,
			@RequestParam(required = false) String keyword,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
		Page<ClassResponse> classes = classService.listScoped(principal, status, trainingProgramId, keyword, page - 1, limit);
		return PageResponse.of(classes.getContent(), page, limit, classes.getTotalElements());
	}

	@Operation(summary = "Create classes")
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
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'create')")
	public ApiResponse<ClassResponse> create(
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody CreateClassRequest request) {
		return ApiResponse.ok(
				classService.create(request, principal.id()),
				messageService.get("success.class.created"));
	}

	@Operation(summary = "Get classes detail")
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
	public ApiResponse<ClassResponse> get(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanViewClass(principal, id);
		return ApiResponse.ok(classService.get(id));
	}

	@Operation(summary = "Update classes")
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
	public ApiResponse<ClassResponse> update(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateClassRequest request) {
		classAccessService.assertCanManageClass(principal, id);
		return ApiResponse.ok(classService.update(id, request, principal.id()));
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
	public ApiResponse<ClassResponse> updateStatus(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateClassStatusRequest request) {
		classAccessService.assertCanManageClass(principal, id);
		return ApiResponse.ok(classService.updateStatus(id, request.status(), principal.id()));
	}

	@Operation(summary = "Delete classes")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No content"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public void delete(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanManageClass(principal, id);
		classService.delete(id, principal.id());
	}

	@Operation(summary = "List trainers")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}/trainers")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<List<ClassTrainerResponse>> listTrainers(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanViewClass(principal, id);
		return ApiResponse.ok(classTrainerService.list(id));
	}

	@Operation(summary = "Replace trainers")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/trainers")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public ApiResponse<List<ClassTrainerResponse>> replaceTrainers(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateClassTrainersRequest request) {
		classAccessService.assertCanManageClass(principal, id);
		return ApiResponse.ok(classTrainerService.replace(id, request));
	}

	@Operation(summary = "List admins")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@GetMapping("/{id}/admins")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'view')")
	public ApiResponse<List<ClassAdminResponse>> listAdmins(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal) {
		classAccessService.assertCanViewClass(principal, id);
		return ApiResponse.ok(classAdminService.list(id));
	}

	@Operation(summary = "Replace admins")
	@ApiResponses(value = {
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Business conflict")
	})
	@PutMapping("/{id}/admins")
	@PreAuthorize("@permissionEvaluator.hasPermission(authentication, 'class', 'modify')")
	public ApiResponse<List<ClassAdminResponse>> replaceAdmins(
			@PathVariable Long id,
			@AuthenticationPrincipal FapUserPrincipal principal,
			@Valid @RequestBody UpdateClassAdminsRequest request) {
		classAccessService.assertCanManageClass(principal, id);
		return ApiResponse.ok(classAdminService.replace(id, request));
	}
}
