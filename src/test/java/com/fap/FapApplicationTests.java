package com.fap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Context-load smoke test.
 *
 * <p>The "test" profile boots the whole application without a database, so this test builds the
 * Hibernate metamodel for every entity and instantiates every Spring Data repository. That
 * validates entity mappings, JPQL {@code @Query} strings and their paging count queries, derived
 * query method property paths, and {@code @EntityGraph} attribute paths.
 *
 * <p>It deliberately cannot catch two things, because no connection is opened: native
 * ({@code nativeQuery = true}) query syntax, and schema drift between the Flyway migrations and
 * the entities. Those still require starting the app against a real Oracle instance, where
 * {@code ddl-auto: validate} does the checking.
 *
 * <p>The annotations match the other {@code *IntegrationTest} classes exactly so Spring's test
 * context cache serves all of them from one boot instead of starting the application twice.
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.MOCK,
		properties = {
				"management.health.db.enabled=false",
				"management.metrics.tags.env=test"
		})
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
class FapApplicationTests {

	@Test
	void contextLoads() {
	}
}
