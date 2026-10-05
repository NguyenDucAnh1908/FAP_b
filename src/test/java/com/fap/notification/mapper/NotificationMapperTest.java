package com.fap.notification.mapper;

import com.fap.common.i18n.MessageService;
import com.fap.notification.dto.NotificationResponse;
import com.fap.notification.entity.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A notification is rendered when it is read, in the reader's language. Rows from before keys were
 * stored, and keys that have since left the bundles, must still come back as readable English text
 * rather than as an error or an empty string.
 */
class NotificationMapperTest {

	private static final Locale VIETNAMESE = Locale.forLanguageTag("vi");
	private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 3, 1, 9, 0);

	private final NotificationMapper mapper = new NotificationMapper(
			new MessageService(messageSource()),
			new ObjectMapper());

	@AfterEach
	void resetLocale() {
		LocaleContextHolder.resetLocaleContext();
	}

	@Test
	void rendersKeyBasedNotificationInTheRequestLocale() {
		LocaleContextHolder.setLocale(VIETNAMESE);

		NotificationResponse response = mapper.toResponse(keyed("[\"Java 101\",\"course_result.status.passed\"]"));

		assertThat(response.id()).isEqualTo(5L);
		assertThat(response.title()).isEqualTo("Kết quả cuối khóa đã được công bố");
		assertThat(response.message()).isEqualTo("Kết quả khóa học Java 101 của bạn là Đạt");
		assertThat(response.read()).isTrue();
		assertThat(response.createdAt()).isEqualTo(CREATED_AT);
	}

	/** An argument that is itself a message key (the course-result status) follows the reader's language too. */
	@Test
	void rendersEnglishForAnEnglishReader() {
		LocaleContextHolder.setLocale(Locale.ENGLISH);

		NotificationResponse response = mapper.toResponse(keyed("[\"Java 101\",\"course_result.status.passed\"]"));

		assertThat(response.title()).isEqualTo("Course result published");
		assertThat(response.message()).isEqualTo("Your result for Java 101 is Passed");
	}

	@Test
	void legacyRowWithoutKeysReturnsStoredTextUnchanged() {
		LocaleContextHolder.setLocale(VIETNAMESE);
		Notification legacy = new Notification();
		legacy.setId(5L);
		legacy.setTitle("Class opened");
		legacy.setMessage("Class JAVA-01 is now open");
		legacy.setCreatedAt(CREATED_AT);

		NotificationResponse response = mapper.toResponse(legacy);

		assertThat(response.title()).isEqualTo("Class opened");
		assertThat(response.message()).isEqualTo("Class JAVA-01 is now open");
	}

	/** A key removed from the bundles falls back to the English text stored at write time. */
	@Test
	void unknownKeyFallsBackToStoredText() {
		LocaleContextHolder.setLocale(VIETNAMESE);
		Notification notification = keyed("[\"Java 101\",\"course_result.status.unknown\"]");
		notification.setTitleKey("notification.gone.title");
		notification.setMessageKey("notification.gone.message");

		NotificationResponse response = mapper.toResponse(notification);

		assertThat(response.title()).isEqualTo("Course result published");
		assertThat(response.message()).isEqualTo("Your result for Java 101 is Passed");
	}

	@Test
	void keyWithoutStoredArgumentsRendersWithoutThem() {
		LocaleContextHolder.setLocale(Locale.ENGLISH);
		Notification notification = keyed(null);
		notification.setMessageKey("notification.course_result.title");

		assertThat(mapper.toResponse(notification).message()).isEqualTo("Course result published");
	}

	@Test
	void argumentsRoundTripAsAJsonArrayOfStrings() {
		String json = mapper.writeArgs(List.of("Java \"101\"", "Lập trình"));

		assertThat(json).isEqualTo("[\"Java \\\"101\\\"\",\"Lập trình\"]");
		assertThat(mapper.readArgs(json)).containsExactly("Java \"101\"", "Lập trình");
	}

	@Test
	void rejectsCorruptStoredArguments() {
		assertThatThrownBy(() -> mapper.toResponse(keyed("not json")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("notification arguments");
	}

	private static Notification keyed(String messageArgs) {
		Notification notification = new Notification();
		notification.setId(5L);
		notification.setTitleKey("notification.course_result.title");
		notification.setMessageKey("notification.course_result.message");
		notification.setMessageArgs(messageArgs);
		notification.setTitle("Course result published");
		notification.setMessage("Your result for Java 101 is Passed");
		notification.setRead(true);
		notification.setCreatedAt(CREATED_AT);
		return notification;
	}

	private static StaticMessageSource messageSource() {
		StaticMessageSource source = new StaticMessageSource();
		source.addMessage("notification.course_result.title", Locale.ENGLISH, "Course result published");
		source.addMessage("notification.course_result.message", Locale.ENGLISH, "Your result for {0} is {1}");
		source.addMessage("course_result.status.passed", Locale.ENGLISH, "Passed");
		source.addMessage("notification.course_result.title", VIETNAMESE, "Kết quả cuối khóa đã được công bố");
		source.addMessage("notification.course_result.message", VIETNAMESE, "Kết quả khóa học {0} của bạn là {1}");
		source.addMessage("course_result.status.passed", VIETNAMESE, "Đạt");
		return source;
	}
}
