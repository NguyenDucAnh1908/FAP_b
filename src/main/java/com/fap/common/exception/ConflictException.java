package com.fap.common.exception;

public class ConflictException extends BusinessException {

	public ConflictException(String message) {
		super("BUSINESS_CONFLICT", message);
	}
}
