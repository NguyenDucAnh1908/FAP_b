package com.fap.common.util;

import com.fap.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {

	private final FileValidator fileValidator = new FileValidator();

	private static MultipartFile file(String contentType, byte[] content) {
		return new MockMultipartFile("file", "week-1.pdf", contentType, content);
	}

	private static byte[] bytes(int length) {
		return new byte[length];
	}

	private void assertRejectedWithCode(MultipartFile file, String expectedCode) {
		assertThatThrownBy(() -> fileValidator.validateUpload(file))
				.isInstanceOf(BadRequestException.class)
				.extracting(exception -> ((BadRequestException) exception).getCode())
				.isEqualTo(expectedCode);
	}

	@Test
	void acceptsAnAllowedContentTypeWithContent() {
		assertThatCode(() -> fileValidator.validateUpload(
				file("application/pdf", "slides".getBytes(StandardCharsets.UTF_8))))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsANullFile() {
		assertRejectedWithCode(null, "FILE_REQUIRED");
	}

	@Test
	void rejectsAnEmptyFile() {
		assertRejectedWithCode(file("application/pdf", new byte[0]), "FILE_REQUIRED");
	}

	@Test
	void rejectsAFileOverTheSizeLimit() {
		assertRejectedWithCode(file("application/pdf", bytes(20 * 1024 * 1024 + 1)), "FILE_TOO_LARGE");
	}

	@Test
	void acceptsAFileExactlyAtTheSizeLimit() {
		assertThatCode(() -> fileValidator.validateUpload(file("application/pdf", bytes(20 * 1024 * 1024))))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"application/x-msdownload",
		"application/x-sh",
		"application/octet-stream",
		"text/html"
	})
	void rejectsADisallowedContentType(String contentType) {
		assertRejectedWithCode(file(contentType, "payload".getBytes(StandardCharsets.UTF_8)), "FILE_TYPE_NOT_ALLOWED");
	}

	@Test
	void rejectsAMissingContentType() {
		assertRejectedWithCode(file(null, "payload".getBytes(StandardCharsets.UTF_8)), "FILE_TYPE_NOT_ALLOWED");
	}

	@Test
	void acceptsAContentTypeRegardlessOfCase() {
		assertThatCode(() -> fileValidator.validateUpload(
				file("APPLICATION/PDF", "slides".getBytes(StandardCharsets.UTF_8))))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@CsvSource({
		"week-1.pdf,                        week-1.pdf",
		"C:\\Users\\admin\\week-1.pdf,      week-1.pdf",
		"/var/www/uploads/week-1.pdf,       week-1.pdf",
		"../../../etc/passwd,               passwd",
		"..\\..\\windows\\system32\\a.pdf,  a.pdf",
		"my<script>.pdf,                    my_script_.pdf",
		"report:final?.pdf,                 report_final_.pdf",
		// Every character illegal is still a usable name once replaced — safety, not aesthetics.
		"<>,                                __"
	})
	void sanitizeStripsDirectoriesAndIllegalCharacters(String original, String expected) {
		assertThat(fileValidator.sanitizeFileName(original)).isEqualTo(expected);
	}

	@Test
	void sanitizeTruncatesToTheColumnLength() {
		String longName = "a".repeat(400) + ".pdf";

		String sanitized = fileValidator.sanitizeFileName(longName);

		assertThat(sanitized).hasSize(FileValidator.MAX_FILE_NAME_LENGTH);
	}

	@ParameterizedTest
	@ValueSource(strings = {"..", "...", "/", "\\", "   ", "///"})
	void sanitizeRejectsNamesThatReduceToNothingUseful(String original) {
		assertThatThrownBy(() -> fileValidator.sanitizeFileName(original))
				.isInstanceOf(BadRequestException.class);
	}

	@Test
	void sanitizeRejectsANullName() {
		assertThatThrownBy(() -> fileValidator.sanitizeFileName(null))
				.isInstanceOf(BadRequestException.class)
				.extracting(exception -> ((BadRequestException) exception).getCode())
				.isEqualTo("FILE_NAME_REQUIRED");
	}

	@Test
	void isPresentRejectsNullAndEmpty() {
		assertThat(fileValidator.isPresent(null)).isFalse();
		assertThat(fileValidator.isPresent(file("application/pdf", new byte[0]))).isFalse();
		assertThat(fileValidator.isPresent(file("application/pdf", bytes(1)))).isTrue();
	}
}
