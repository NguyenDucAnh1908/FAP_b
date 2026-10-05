package com.fap.notification.mapper;

import com.fap.common.i18n.MessageService;
import com.fap.notification.dto.NotificationResponse;
import com.fap.notification.entity.Notification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Renders a notification in the reader's language. A notification stores message keys and their
 * arguments rather than text, because the recipient's language is only known when they read it,
 * not when an admin's action created it.
 */
@Component
public class NotificationMapper {

	private final MessageService messageService;
	private final ObjectMapper objectMapper;

	public NotificationMapper(MessageService messageService, ObjectMapper objectMapper) {
		this.messageService = messageService;
		this.objectMapper = objectMapper;
	}

	public NotificationResponse toResponse(Notification notification) {
		Locale locale = LocaleContextHolder.getLocale();
		return new NotificationResponse(
				notification.getId(),
				localize(locale, notification.getTitleKey(), notification.getTitle(), List.of()),
				localize(locale, notification.getMessageKey(), notification.getMessage(), readArgs(notification.getMessageArgs())),
				notification.isRead(),
				notification.getCreatedAt());
	}

	/**
	 * Resolves {@code key} in {@code locale}, falling back to {@code fallbackText} when the key is
	 * unknown. Shared by the read path and by the English text stored at write time, so the two
	 * never disagree on how arguments are applied.
	 */
	public String render(Locale locale, String key, String fallbackText, List<String> args) {
		// An argument may itself be a message key (the course-result status), so each one is looked
		// up with the literal as its own fallback; class names and session titles never match a
		// bundle key and pass through unchanged.
		Object[] localizedArgs = args.stream()
				.map(arg -> messageService.getOrDefault(locale, arg, arg))
				.toArray();
		return messageService.getOrDefault(locale, key, fallbackText, localizedArgs);
	}

	/** The stored form of the arguments: a JSON array of strings, so the column's IS JSON check holds. */
	public String writeArgs(List<String> args) {
		try {
			return objectMapper.writeValueAsString(args);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Notification arguments could not be serialized", exception);
		}
	}

	public List<String> readArgs(String json) {
		if (json == null) {
			return List.of();
		}
		try {
			return List.of(objectMapper.readValue(json, String[].class));
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Stored notification arguments are invalid", exception);
		}
	}

	private String localize(Locale locale, String key, String storedText, List<String> args) {
		// Rows written before V34 carry no key: their stored English text is all there is.
		if (key == null) {
			return storedText;
		}
		return render(locale, key, storedText, args);
	}
}
