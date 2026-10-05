package com.fap.common.i18n;

import com.fap.result.enums.CourseResultStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalExceptionHandler silently falls back to the exception's English message when a key is
 * missing, so a forgotten key only shows up as English text for a Vietnamese client. This test reads
 * the source rather than running it, so it also covers throw sites no other test reaches.
 */
class ErrorMessageKeysTest {

	private static final Path MAIN_JAVA = Path.of("src", "main", "java");
	private static final Path RESOURCES = Path.of("src", "main", "resources");

	/**
	 * A code literal followed by its English message literal (optionally with a message key in
	 * between). This catches constructors as well as helpers that forward the code to one, such as
	 * {@code StatusTransitions.requireAllowed}.
	 */
	private static final Pattern CODE_AND_MESSAGE = Pattern.compile(
			"\"([A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+)\"\\s*,\\s*(?:\"error\\.[^\"]*\"\\s*,\\s*)?\"[A-Z][a-z]");
	private static final Pattern CODE_CONSTRUCTOR = Pattern.compile(
			"new\\s+(?:BadRequest|Conflict|Business)Exception\\s*\\(\\s*\"([A-Z][A-Z0-9_]*)\"\\s*,");
	private static final Pattern FORBIDDEN_CONSTRUCTOR = Pattern.compile("new\\s+ForbiddenException\\s*\\(");
	private static final Pattern UNAUTHORIZED_CONSTRUCTOR = Pattern.compile("new\\s+UnauthorizedException\\s*\\(");
	private static final Pattern MESSAGE_ONLY_CONFLICT = Pattern.compile(
			"new\\s+ConflictException\\s*\\(\\s*\"(?:[^\"\\\\]|\\\\.)*\"\\s*\\)");
	private static final Pattern NOT_FOUND_CONSTRUCTOR = Pattern.compile("new\\s+NotFoundException\\s*\\(");
	private static final Pattern NOT_FOUND_RESOURCE = Pattern.compile("\\s*\"([a-z][a-z0-9_]*)\"\\s*,");
	/** Literal keys, e.g. in {@code withMessageKey("error.X.variant")} or {@code messageService.get("error.X")}. */
	private static final Pattern ERROR_KEY_LITERAL = Pattern.compile("\"(error\\.[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*)\"");
	/** Literal notification and mail keys, e.g. in {@code notificationService.create(userId, "notification.x.title", ...)}. */
	private static final Pattern MESSAGE_KEY_LITERAL = Pattern.compile(
			"\"((?:notification|mail)\\.[a-z0-9_]+(?:\\.[a-z0-9_]+)*)\"");
	/** {@code "course_result.status." + status.name().toLowerCase()}: one key per {@link CourseResultStatus}. */
	private static final Pattern STATUS_KEY_PREFIX = Pattern.compile("\"course_result\\.status\\.\"\\s*\\+");
	private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\d+}");

	private static Map<String, String> englishMessages;
	private static Map<String, String> vietnameseMessages;
	private static Map<String, String> sources;

	@BeforeAll
	static void load() throws IOException {
		englishMessages = readMessages(RESOURCES.resolve("messages.properties"));
		vietnameseMessages = readMessages(RESOURCES.resolve("messages_vi.properties"));
		sources = readSources();
	}

	@Test
	void englishAndVietnameseFilesDefineTheSameKeys() {
		assertThat(vietnameseMessages.keySet()).containsExactlyInAnyOrderElementsOf(englishMessages.keySet());
	}

	@Test
	void everyErrorKeyReferencedInMainCodeExistsInBothFiles() {
		Set<String> referencedKeys = referencedErrorKeys();

		// Guards against the scan itself breaking (wrong directory, pattern drift) and passing vacuously.
		assertThat(referencedKeys).hasSizeGreaterThan(100);
		assertThat(englishMessages.keySet()).containsAll(referencedKeys);
		assertThat(vietnameseMessages.keySet()).containsAll(referencedKeys);
	}

	/**
	 * Notifications and the password-reset mail fall back just as silently (to the stored English
	 * text, or to the key itself), so their keys are held to the same rule as error keys.
	 */
	@Test
	void everyNotificationAndMailKeyReferencedInMainCodeExistsInBothFiles() {
		Set<String> referencedKeys = referencedMessageKeys();

		assertThat(referencedKeys).hasSizeGreaterThan(30);
		assertThat(englishMessages.keySet()).containsAll(referencedKeys);
		assertThat(vietnameseMessages.keySet()).containsAll(referencedKeys);
	}

	@Test
	void everyNotFoundExceptionNamesALiteralResource() {
		List<String> withoutResource = new ArrayList<>();
		sources.forEach((file, source) -> {
			Matcher constructor = NOT_FOUND_CONSTRUCTOR.matcher(source);
			while (constructor.find()) {
				Matcher resource = NOT_FOUND_RESOURCE.matcher(source).region(constructor.end(), source.length());
				if (!resource.lookingAt()) {
					withoutResource.add(file + ":" + lineOf(source, constructor.start()));
				}
			}
		});

		assertThat(withoutResource)
				.as("new NotFoundException(resource, message) keeps the message localizable per resource")
				.isEmpty();
	}

	@Test
	void messagesWithPlaceholdersEscapeApostrophes() {
		List<String> unescaped = new ArrayList<>();
		for (Map<String, String> messages : List.of(englishMessages, vietnameseMessages)) {
			messages.forEach((key, message) -> {
				// MessageFormat only runs when args are passed, and then a lone ' starts a quoted section.
				if (PLACEHOLDER.matcher(message).find() && message.replace("''", "").contains("'")) {
					unescaped.add(key);
				}
			});
		}

		assertThat(unescaped).isEmpty();
	}

	private static Set<String> referencedErrorKeys() {
		Set<String> keys = new TreeSet<>();
		sources.values().forEach(source -> {
			collect(CODE_AND_MESSAGE, source, code -> keys.add("error." + code));
			collect(CODE_CONSTRUCTOR, source, code -> keys.add("error." + code));
			collect(ERROR_KEY_LITERAL, source, keys::add);
			if (FORBIDDEN_CONSTRUCTOR.matcher(source).find()) {
				keys.add("error.ACCESS_DENIED");
			}
			if (UNAUTHORIZED_CONSTRUCTOR.matcher(source).find()) {
				keys.add("error.UNAUTHORIZED");
			}
			if (MESSAGE_ONLY_CONFLICT.matcher(source).find()) {
				keys.add("error.BUSINESS_CONFLICT");
			}
			Matcher constructor = NOT_FOUND_CONSTRUCTOR.matcher(source);
			while (constructor.find()) {
				keys.add("error.RESOURCE_NOT_FOUND");
				Matcher resource = NOT_FOUND_RESOURCE.matcher(source).region(constructor.end(), source.length());
				if (resource.lookingAt()) {
					keys.add("error.RESOURCE_NOT_FOUND." + resource.group(1));
				}
			}
		});
		return keys;
	}

	private static Set<String> referencedMessageKeys() {
		Set<String> keys = new TreeSet<>();
		sources.values().forEach(source -> {
			collect(MESSAGE_KEY_LITERAL, source, keys::add);
			if (STATUS_KEY_PREFIX.matcher(source).find()) {
				for (CourseResultStatus status : CourseResultStatus.values()) {
					keys.add("course_result.status." + status.name().toLowerCase());
				}
			}
		});
		return keys;
	}

	private static void collect(Pattern pattern, String source, Consumer<String> sink) {
		Matcher matcher = pattern.matcher(source);
		while (matcher.find()) {
			sink.accept(matcher.group(1));
		}
	}

	private static int lineOf(String source, int offset) {
		return (int) source.substring(0, offset).chars().filter(character -> character == '\n').count() + 1;
	}

	private static Map<String, String> readSources() throws IOException {
		Map<String, String> result = new LinkedHashMap<>();
		try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
			for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
				result.put(MAIN_JAVA.relativize(file).toString(), Files.readString(file, StandardCharsets.UTF_8));
			}
		}
		return result;
	}

	/**
	 * Parsed by hand instead of with {@link java.util.Properties} so a duplicated key, which Properties
	 * would silently collapse, fails the test.
	 */
	private static Map<String, String> readMessages(Path file) throws IOException {
		Map<String, String> messages = new LinkedHashMap<>();
		List<String> duplicates = new ArrayList<>();
		for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
			String trimmed = line.strip();
			if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
				continue;
			}
			int separator = trimmed.indexOf('=');
			String key = trimmed.substring(0, separator).strip();
			if (messages.put(key, trimmed.substring(separator + 1)) != null) {
				duplicates.add(key);
			}
		}
		assertThat(duplicates).as("duplicate keys in %s", file).isEmpty();
		return messages;
	}
}
