package com.fap.syllabus.dto;

import java.io.InputStream;

/**
 * A material's content plus the metadata the controller needs for download headers. Not a JSON
 * response body. {@code content} reads a spooled temporary copy that is deleted when the stream is
 * closed, which Spring does after writing the response.
 */
public record MaterialFileDownload(
		String fileName,
		String contentType,
		long contentLength,
		InputStream content
) {
}
