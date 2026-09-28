package com.fap.common.config;

import com.fap.support.AbstractOracleIT;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Spring context only starts if Flyway migrated the schema and Hibernate's
 * {@code ddl-auto=validate} accepted every entity mapping, so reaching these tests already proves
 * both. The assertions pin the parts that a successful boot does not imply.
 */
class DatabaseSchemaIT extends AbstractOracleIT {

	private static final Pattern VERSION = Pattern.compile("^V(\\d+)__.*\\.sql$");

	@Autowired
	private Flyway flyway;

	@Test
	void everyMigrationIsApplied() {
		assertThat(flyway.info().pending()).isEmpty();
		assertThat(Arrays.stream(flyway.info().all()).filter(info -> info.getState().isFailed()))
				.isEmpty();
	}

	@Test
	void schemaIsAtLatestMigrationVersion() throws IOException {
		MigrationInfo current = flyway.info().current();

		assertThat(current).isNotNull();
		assertThat(current.getVersion().getVersion()).isEqualTo(String.valueOf(highestMigrationVersion()));
	}

	private static int highestMigrationVersion() throws IOException {
		Resource[] migrations = new PathMatchingResourcePatternResolver()
				.getResources("classpath:db/migration/V*__*.sql");
		return Arrays.stream(migrations)
				.map(Resource::getFilename)
				.map(VERSION::matcher)
				.filter(Matcher::matches)
				.mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
				.max()
				.orElseThrow();
	}
}
