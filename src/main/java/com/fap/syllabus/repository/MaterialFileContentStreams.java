package com.fap.syllabus.repository;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Streams material bytes between the BLOB column and a stream, so an upload or download never holds
 * a whole file (up to the multipart limit) on the heap.
 */
public interface MaterialFileContentStreams {

	/**
	 * Inserts the content row for an already flushed {@code material_files} row.
	 *
	 * @param length exact number of bytes {@code data} provides
	 */
	void insertContent(Long materialFileId, InputStream data, long length);

	/**
	 * Copies the stored bytes to {@code target}.
	 *
	 * @return {@code false} when the material has no stored content
	 */
	boolean copyContent(Long materialFileId, OutputStream target);
}
