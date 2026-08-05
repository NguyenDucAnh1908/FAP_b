package com.fap.syllabus.dto;

/**
 * Bytes of a material plus the metadata the controller needs to build download headers. Not a JSON
 * response body — the controller streams {@code data} and puts the rest into headers.
 */
public record MaterialFileDownload(
		String fileName,
		String contentType,
		byte[] data
) {
}
