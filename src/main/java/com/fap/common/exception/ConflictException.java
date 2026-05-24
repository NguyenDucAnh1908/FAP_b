package com.fap.common.exception;

public class ConflictException extends BusinessException {

	public ConflictException(String message) {
		super("BUSINESS_CONFLICT", message);
	}

	public ConflictException(String code, String message) {
		super(code, message);
	}
}
