package com.fap.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
		boolean success,
		ErrorBody error
) {

	public static ErrorResponse of(String code, String message) {
		return new ErrorResponse(false, new ErrorBody(code, message, null));
	}

	public static ErrorResponse of(String code, String message, List<FieldError> details) {
		return new ErrorResponse(false, new ErrorBody(code, message, details));
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ErrorBody(
			String code,
			String message,
			List<FieldError> details
	) {
	}

	public record FieldError(
			String field,
			String message
	) {
	}
}
