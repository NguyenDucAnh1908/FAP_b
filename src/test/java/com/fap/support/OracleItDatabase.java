package com.fap.support;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.OracleContainer;

import java.time.Duration;

/**
 * Resolves the Oracle database used by {@code *IT} tests, once per JVM.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>{@code FAP_IT_DB_URL} (+ {@code FAP_IT_DB_USERNAME}/{@code FAP_IT_DB_PASSWORD}): an existing
 *       Oracle, for machines without Docker. Tests roll back their own writes, but Flyway still
 *       migrates that schema, so point it at a dev schema, never a shared one.</li>
 *   <li>Docker available: a throwaway {@code gvenzl/oracle-xe} container shared by all IT classes.</li>
 *   <li>Neither: IT classes are skipped, unless {@code FAP_IT_REQUIRE_DB=true} (set in CI), in which
 *       case they run and fail loudly instead of silently passing without a database.</li>
 * </ol>
 */
public final class OracleItDatabase {

	private static final String IMAGE = "gvenzl/oracle-xe:21-slim-faststart";

	private static volatile Connection connection;

	private OracleItDatabase() {
	}

	/** Used by {@code @EnabledIf} on {@link AbstractOracleIT}. */
	public static boolean isAvailable() {
		return externalUrl() != null
				|| Boolean.parseBoolean(System.getenv("FAP_IT_REQUIRE_DB"))
				|| DockerClientFactory.instance().isDockerAvailable();
	}

	public static Connection connection() {
		Connection current = connection;
		if (current == null) {
			synchronized (OracleItDatabase.class) {
				current = connection;
				if (current == null) {
					current = resolve();
					connection = current;
				}
			}
		}
		return current;
	}

	private static Connection resolve() {
		String url = externalUrl();
		if (url != null) {
			return new Connection(
					url,
					envOrDefault("FAP_IT_DB_USERNAME", "fap"),
					envOrDefault("FAP_IT_DB_PASSWORD", ""));
		}
		OracleContainer container = new OracleContainer(IMAGE)
				.withUsername("fap")
				.withPassword("fap")
				.withStartupTimeout(Duration.ofMinutes(5));
		// Not stopped explicitly: the container is shared by every IT class in the JVM and
		// Testcontainers' Ryuk sidecar removes it when the JVM exits.
		container.start();
		return new Connection(container.getJdbcUrl(), container.getUsername(), container.getPassword());
	}

	private static String externalUrl() {
		String url = System.getenv("FAP_IT_DB_URL");
		return url == null || url.isBlank() ? null : url;
	}

	private static String envOrDefault(String name, String fallback) {
		String value = System.getenv(name);
		return value == null ? fallback : value;
	}

	public record Connection(String url, String username, String password) {
	}
}
