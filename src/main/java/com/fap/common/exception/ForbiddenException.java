package com.fap.common.exception;

public class ForbiddenException extends BusinessException {

	public ForbiddenException(String message) {
		super("ACCESS_DENIED", message);
	}
}
