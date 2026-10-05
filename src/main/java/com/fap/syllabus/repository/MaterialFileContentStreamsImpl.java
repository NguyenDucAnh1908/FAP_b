package com.fap.syllabus.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Plain JDBC on the session's connection: a {@code @Lob byte[]} mapping always materializes the
 * whole value, while {@code setBinaryStream} / {@code getBinaryStream} move it in driver-sized chunks.
 * Running inside the caller's transaction keeps it atomic with the {@code material_files} change.
 */
class MaterialFileContentStreamsImpl implements MaterialFileContentStreams {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public void insertContent(Long materialFileId, InputStream data, long length) {
		entityManager.unwrap(Session.class).doWork(connection -> {
			try (PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO material_file_contents (material_file_id, file_data) VALUES (?, ?)")) {
				statement.setLong(1, materialFileId);
				statement.setBinaryStream(2, data, length);
				statement.executeUpdate();
			}
		});
	}

	@Override
	public boolean copyContent(Long materialFileId, OutputStream target) {
		return entityManager.unwrap(Session.class).doReturningWork(connection -> {
			try (PreparedStatement statement = connection.prepareStatement(
					"SELECT file_data FROM material_file_contents WHERE material_file_id = ?")) {
				statement.setLong(1, materialFileId);
				try (ResultSet resultSet = statement.executeQuery()) {
					if (!resultSet.next()) {
						return false;
					}
					try (InputStream content = resultSet.getBinaryStream(1)) {
						content.transferTo(target);
					}
					return true;
				}
			} catch (IOException exception) {
				throw new UncheckedIOException("Could not stream material content", exception);
			}
		});
	}
}
