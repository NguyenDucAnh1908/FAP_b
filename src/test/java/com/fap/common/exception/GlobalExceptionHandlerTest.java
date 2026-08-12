package com.fap.common.exception;

import com.fap.common.api.ErrorResponse;
import com.fap.common.i18n.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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
