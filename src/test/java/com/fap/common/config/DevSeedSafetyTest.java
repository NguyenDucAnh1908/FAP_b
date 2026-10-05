package com.fap.common.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DevSeedSafetyTest {

	@Test
	void seedIsLocalOnlyRepeatableAndNonDestructive() throws IOException {
		String local = read("src/main/resources/application-local.yaml");
		String prod = read("src/main/resources/application-prod.yaml");
		String seed = read("src/main/resources/db/seed/R__seed_e2e_test_data.sql").toUpperCase();

		assertThat(local).contains("classpath:db/seed");
		assertThat(prod).doesNotContain("classpath:db/seed");
		assertThat(seed)
				.contains("MERGE INTO USERS", "MERGE INTO SYSTEM_SETTINGS", "DEV_SEED")
				.doesNotContain("TRUNCATE ", "DROP TABLE", "DELETE FROM");
	}

	@Test
	void resetRequiresDatasetMarkerAndDoesNotDisableDatabaseSafety() throws IOException {
		String reset = executableSql(read("scripts/reset-dev-data.sql")).toUpperCase();

		assertThat(reset)
				.contains("DEV_SEED", "DATASET_VERSION", "RAISE_APPLICATION_ERROR")
				.doesNotContain("TRUNCATE ", "DROP TABLE", "DISABLE CONSTRAINT");
	}

	private String read(String path) throws IOException {
		return Files.readString(Path.of(path));
	}

	private String executableSql(String sql) {
		return sql.replaceAll("(?m)^\\s*--.*$", "");
	}
}
