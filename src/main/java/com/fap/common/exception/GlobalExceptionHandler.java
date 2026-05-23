package com.fap.common.exception;

import com.fap.common.api.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		List<ErrorResponse.FieldError> details = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();

		return ResponseEntity.unprocessableEntity()
				.body(ErrorResponse.of("VALIDATION_ERROR", "Validation failed", details));
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
				.body(ErrorResponse.of("VALIDATION_ERROR", "Validation failed", details));
	}

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErrorResponse.of(exception.getCode(), exception.getMessage()));
	}

	@ExceptionHandler(ForbiddenException.class)
	ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ErrorResponse.of(exception.getCode(), exception.getMessage()));
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<ErrorResponse> handleConflict(ConflictException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of(exception.getCode(), exception.getMessage()));
	}

	@ExceptionHandler(BadCredentialsException.class)
	ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ErrorResponse.of("UNAUTHORIZED", "Invalid credentials"));
	}

	@ExceptionHandler(UnauthorizedException.class)
	ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(ErrorResponse.of(exception.getCode(), exception.getMessage()));
	}

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of(exception.getCode(), exception.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of("INTERNAL_ERROR", "Unexpected server error"));
	}
}
