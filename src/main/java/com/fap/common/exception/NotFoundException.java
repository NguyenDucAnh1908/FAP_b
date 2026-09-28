package com.fap.common.exception;

/**
 * Always carries the API code {@code RESOURCE_NOT_FOUND} (the documented contract). The resource
 * only selects a more specific localized message, {@code error.RESOURCE_NOT_FOUND.<resource>}.
 */
public class NotFoundException extends BusinessException {

	public static final String CODE = "RESOURCE_NOT_FOUND";

	public NotFoundException(String message) {
		super(CODE, message);
	}

	/**
	 * @param resource stable lowercase key of the missing resource, for example {@code class} or
	 *                 {@code quiz_attempt}
	 */
	public NotFoundException(String resource, String message) {
		super(CODE, message);
		withMessageKey("error." + CODE + "." + resource);
	}
}
