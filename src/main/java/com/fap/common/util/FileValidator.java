package com.fap.common.util;

import com.fap.common.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates uploaded files before their bytes are persisted.
 *
 * <p>Size is checked here as well as by {@code spring.servlet.multipart.max-file-size} because the
 * container limit produces a generic multipart error, while this produces a documented error code.
 */
@Component
public class FileValidator {

	/** Matches the {@code file_name VARCHAR2(255)} column on {@code material_files}. */
	public static final int MAX_FILE_NAME_LENGTH = 255;

	private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;

	/** Control characters plus the characters Windows and Oracle both dislike in a file name. */
	private static final Pattern ILLEGAL_FILE_NAME_CHARS = Pattern.compile("[\\p{Cntrl}<>:\"|?*/\\\\]");

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
			"application/pdf",
			"application/msword",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.ms-powerpoint",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation",
			"application/vnd.ms-excel",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"application/zip",
			"text/plain",
			"text/csv",
			"image/png",
			"image/jpeg");

	public boolean isPresent(MultipartFile file) {
		return file != null && !file.isEmpty();
	}

	/**
	 * Rejects anything that must not reach storage. Callers should treat a successful return as
	 * permission to read {@code file.getBytes()}.
	 */
	public void validateUpload(MultipartFile file) {
		if (!isPresent(file)) {
			throw new BadRequestException("FILE_REQUIRED", "A non-empty file is required");
		}
		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new BadRequestException(
					"FILE_TOO_LARGE",
					"File exceeds the maximum allowed size of " + (MAX_FILE_SIZE_BYTES / (1024 * 1024)) + "MB");
		}
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
			throw new BadRequestException("FILE_TYPE_NOT_ALLOWED", "File type is not allowed");
		}
	}

	/**
	 * Reduces a client-supplied filename to a bare, length-bounded name.
	 *
	 * <p>Directory components are dropped rather than escaped, and both separators are handled
	 * regardless of host platform so a name crafted on Linux cannot smuggle a path through a Windows
	 * server. Leading dots go too, so the result can never be a relative reference like {@code ..}.
	 * Characters are filtered with an explicit pattern rather than via {@code Paths.get}, which would
	 * throw {@code InvalidPathException} on Windows for a client-supplied name containing {@code :}.
	 */
	public String sanitizeFileName(String originalFileName) {
		if (originalFileName == null || originalFileName.isBlank()) {
			throw new BadRequestException("FILE_NAME_REQUIRED", "File name is required");
		}
		String candidate = originalFileName.replace('\\', '/');
		candidate = candidate.substring(candidate.lastIndexOf('/') + 1);
		candidate = ILLEGAL_FILE_NAME_CHARS.matcher(candidate).replaceAll("_").trim();
		while (candidate.startsWith(".")) {
			candidate = candidate.substring(1).trim();
		}
		if (candidate.isBlank()) {
			throw new BadRequestException("FILE_NAME_INVALID", "File name is invalid");
		}
		return candidate.length() > MAX_FILE_NAME_LENGTH
				? candidate.substring(0, MAX_FILE_NAME_LENGTH)
				: candidate;
	}
}
