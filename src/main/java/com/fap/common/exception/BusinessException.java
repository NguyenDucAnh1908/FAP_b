package com.fap.common.exception;

/**
 * A domain failure with a stable API {@code code}. The HTTP message is localized by
 * {@link GlobalExceptionHandler}: {@link #getMessageKey()} first (when set), then {@code error.<code>},
 * then {@link #getMessage()}, which therefore stays the English fallback.
 */
public class BusinessException extends RuntimeException {

	private final String code;
	private String messageKey;
	private Object[] messageArgs = new Object[0];

	public BusinessException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	/** A message key more specific than {@code error.<code>}, or {@code null}. */
	public String getMessageKey() {
		return messageKey;
	}

	public Object[] getMessageArgs() {
		return messageArgs.clone();
	}

	/**
	 * Localizes this failure with a key more specific than {@code error.<code>} without changing the
	 * API code, for example when one code is thrown with different wordings, or the message carries
	 * values ({@code {0}} placeholders filled from {@code args}).
	 */
	public final BusinessException withMessageKey(String messageKey, Object... args) {
		this.messageKey = messageKey;
		this.messageArgs = args == null ? new Object[0] : args.clone();
		return this;
	}

	/** Fills {@code {0}}-style placeholders of {@code error.<code>} without a dedicated key. */
	public final BusinessException withMessageArgs(Object... args) {
		this.messageArgs = args == null ? new Object[0] : args.clone();
		return this;
	}
}
