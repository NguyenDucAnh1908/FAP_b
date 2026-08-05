package com.fap.common.exception;

import com.fap.common.api.ErrorResponse;
import com.fap.common.i18n.MessageService;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private final MessageService messageService;

	public GlobalExceptionHandler(MessageService messageService) {
		this.messageService = messageService;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		List<ErrorResponse.FieldError> details = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();

		return ResponseEntity.unprocessableEntity()
				.body(ErrorResponse.of("VALIDATION_ERROR", messageService.get("error.VALIDATION_ERROR"), details));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
		List<ErrorResponse.FieldError> details = exception.getConstraintViolations()
				.stream()
				.map(violation -> new ErrorResponse.FieldError(
						violation.getPropertyPath().toString(),
						violation.getMessage()))
				.toList();

		return ResponseEntity.unprocessableEntity()
				.body(ErrorResponse.of("VALIDATION_ERROR", messageService.get("error.VALIDATION_ERROR"), details));
	}

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErrorResponse.of(exception.getCode(), errorMessage(exception)));
	}

	@ExceptionHandler(ForbiddenException.class)
	ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ErrorResponse.of(exception.getCode(), errorMessage(exception)));
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<ErrorResponse> handleConflict(ConflictException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of(exception.getCode(), errorMessage(exception)));
	}

	@ExceptionHandler(BadRequestException.class)
	ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException exception) {
		return ResponseEntity.badRequest()
				.body(ErrorResponse.of(exception.getCode(), errorMessage(exception)));
	}

	@ExceptionHandler(BadCredentialsException.class)
	ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ErrorResponse.of("UNAUTHORIZED", messageService.get("error.UNAUTHORIZED")));
	}

	@ExceptionHandler(UnauthorizedException.class)
	ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ErrorResponse.of(exception.getCode(), errorMessage(exception)));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
				.body(ErrorResponse.of("METHOD_NOT_ALLOWED", messageService.get("error.METHOD_NOT_ALLOWED")));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErrorResponse.of("RESOURCE_NOT_FOUND", messageService.get("error.RESOURCE_NOT_FOUND")));
	}

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of(exception.getCode(), errorMessage(exception)));
	}

	/**
	 * Lost-update protection surfaces here. Two concurrent writers to the same optimistically
	 * locked row (for example two {@code submit} calls on one quiz attempt) mean the loser's
	 * version check fails, which is a business conflict rather than a server fault.
	 */
	@ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
	ResponseEntity<ErrorResponse> handleOptimisticLock(Exception exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of(
						"CONCURRENT_MODIFICATION",
						messageService.get("error.CONCURRENT_MODIFICATION")));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of("INTERNAL_ERROR", messageService.get("error.INTERNAL_ERROR")));
	}

	private String errorMessage(BusinessException exception) {
		return messageService.getOrDefault("error." + exception.getCode(), exception.getMessage());
	}
}
