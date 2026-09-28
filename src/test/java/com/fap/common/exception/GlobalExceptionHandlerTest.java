package com.fap.common.exception;

import com.fap.common.api.ErrorResponse;
import com.fap.common.i18n.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

	@Test
	void accessDeniedReturnsForbiddenInsteadOfInternalServerError() {
		MessageService messageService = mock(MessageService.class);
		when(messageService.get("error.FORBIDDEN"))
				.thenReturn("You do not have permission to perform this action");
		GlobalExceptionHandler handler = new GlobalExceptionHandler(messageService);

		ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(
				new AccessDeniedException("Access Denied"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().error().code()).isEqualTo("FORBIDDEN");
	}

	@Test
	void missingMultipartFileReturnsBadRequest() {
		MessageService messageService = mock(MessageService.class);
		when(messageService.get("error.FILE_REQUIRED")).thenReturn("A non-empty file is required");
		GlobalExceptionHandler handler = new GlobalExceptionHandler(messageService);

		ResponseEntity<ErrorResponse> response = handler.handleMissingRequestPart(
				new MissingServletRequestPartException("file"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().error().code()).isEqualTo("FILE_REQUIRED");
	}

	@Test
	void oversizedMultipartFileReturnsBadRequest() {
		MessageService messageService = mock(MessageService.class);
		when(messageService.get("error.FILE_TOO_LARGE")).thenReturn("File exceeds the maximum allowed size");
		GlobalExceptionHandler handler = new GlobalExceptionHandler(messageService);

		ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSize(
				new MaxUploadSizeExceededException(20L * 1024 * 1024));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().error().code()).isEqualTo("FILE_TOO_LARGE");
	}

	@Test
	void notFoundKeepsTheApiCodeAndUsesTheResourceSpecificMessage() {
		MessageService messageService = messageSource(
				"error.RESOURCE_NOT_FOUND", "Resource not found",
				"error.RESOURCE_NOT_FOUND.class", "Class not found");
		GlobalExceptionHandler handler = new GlobalExceptionHandler(messageService);

		ResponseEntity<ErrorResponse> specific = handler.handleNotFound(new NotFoundException("class", "Class not found"));
		ResponseEntity<ErrorResponse> unknownResource = handler.handleNotFound(new NotFoundException("widget", "Widget not found"));
		ResponseEntity<ErrorResponse> legacy = handler.handleNotFound(new NotFoundException("Quiz not found"));

		assertThat(specific.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(specific.getBody().error().code()).isEqualTo("RESOURCE_NOT_FOUND");
		assertThat(specific.getBody().error().message()).isEqualTo("Class not found");
		assertThat(unknownResource.getBody().error().code()).isEqualTo("RESOURCE_NOT_FOUND");
		assertThat(unknownResource.getBody().error().message()).isEqualTo("Resource not found");
		assertThat(legacy.getBody().error().message()).isEqualTo("Resource not found");
	}

	@Test
	void messageKeyAndArgsRefineTheMessageWithoutChangingTheCode() {
		MessageService messageService = messageSource(
				"error.IMPORT_HEADER_MISSING", "Missing header {0}",
				"error.BUSINESS_CONFLICT", "Business conflict",
				"error.BUSINESS_CONFLICT.email_exists", "Email already exists");
		GlobalExceptionHandler handler = new GlobalExceptionHandler(messageService);

		ResponseEntity<ErrorResponse> withArgs = handler.handleBadRequest(
				(BadRequestException) new BadRequestException("IMPORT_HEADER_MISSING", "Missing header code")
						.withMessageArgs("code"));
		ResponseEntity<ErrorResponse> withKey = handler.handleConflict(
				(ConflictException) new ConflictException("Email already exists")
						.withMessageKey("error.BUSINESS_CONFLICT.email_exists"));
		ResponseEntity<ErrorResponse> noKey = handler.handleConflict(new ConflictException("UNKNOWN_CODE", "Raw English"));

		assertThat(withArgs.getBody().error().message()).isEqualTo("Missing header code");
		assertThat(withKey.getBody().error().code()).isEqualTo("BUSINESS_CONFLICT");
		assertThat(withKey.getBody().error().message()).isEqualTo("Email already exists");
		assertThat(noKey.getBody().error().message()).isEqualTo("Raw English");
	}

	/** A real message source, so key lookup, fallback and {0} formatting behave as in production. */
	private static MessageService messageSource(String... keysAndMessages) {
		StaticMessageSource source = new StaticMessageSource();
		for (int index = 0; index < keysAndMessages.length; index += 2) {
			source.addMessage(keysAndMessages[index], Locale.getDefault(), keysAndMessages[index + 1]);
		}
		return new MessageService(source);
	}

	@Test
	void malformedMultipartRequestReturnsBadRequest() {
		MessageService messageService = mock(MessageService.class);
		when(messageService.get("error.INVALID_MULTIPART")).thenReturn("Multipart request is invalid");
		GlobalExceptionHandler handler = new GlobalExceptionHandler(messageService);

		ResponseEntity<ErrorResponse> response = handler.handleMultipart(
				new MultipartException("Failed to parse multipart request"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().error().code()).isEqualTo("INVALID_MULTIPART");
	}
}
