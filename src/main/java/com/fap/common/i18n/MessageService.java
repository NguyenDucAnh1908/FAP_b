package com.fap.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

	private final MessageSource messageSource;

	public MessageService(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String get(String code, Object... args) {
		return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale());
	}

	public String getOrDefault(String code, String defaultMessage, Object... args) {
		return messageSource.getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
	}
}
