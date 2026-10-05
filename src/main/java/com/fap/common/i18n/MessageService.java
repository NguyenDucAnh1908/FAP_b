package com.fap.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class MessageService {

	private final MessageSource messageSource;

	public MessageService(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String get(String code, Object... args) {
		return get(LocaleContextHolder.getLocale(), code, args);
	}

	public String getOrDefault(String code, String defaultMessage, Object... args) {
		return getOrDefault(LocaleContextHolder.getLocale(), code, defaultMessage, args);
	}

	/**
	 * Explicit-locale variants for text that is not produced on the request thread (asynchronous
	 * mail) or deliberately in one language (the English fallback stored with a notification).
	 */
	public String get(Locale locale, String code, Object... args) {
		return messageSource.getMessage(code, args, code, locale);
	}

	public String getOrDefault(Locale locale, String code, String defaultMessage, Object... args) {
		return messageSource.getMessage(code, args, defaultMessage, locale);
	}
}
