package com.fap.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/**
 * Base class for tests that need a real Oracle: Flyway migrations, {@code ddl-auto=validate},
 * JPQL/native queries and statement counts. Every subclass shares one Spring context and one
 * database (see {@link OracleItDatabase}). Each test runs in a transaction that is rolled back.
 *
 * <p>Name subclasses {@code *IT} so Failsafe runs them in {@code verify}, not Surefire in {@code test}.
 */
@SpringBootTest
@ActiveProfiles("it")
@Transactional
@EnabledIf(
		value = "com.fap.support.OracleItDatabase#isAvailable",
		disabledReason = "No Oracle for IT: set FAP_IT_DB_URL or start Docker")
public abstract class AbstractOracleIT {

	@PersistenceContext
	protected EntityManager entityManager;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@DynamicPropertySource
	static void oracleProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> OracleItDatabase.connection().url());
		registry.add("spring.datasource.username", () -> OracleItDatabase.connection().username());
		registry.add("spring.datasource.password", () -> OracleItDatabase.connection().password());
	}

	/** Writes pending fixture changes and empties the persistence context so reads hit the database. */
	protected void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}

	/**
	 * Runs {@code action} against a cleared persistence context and returns how many JDBC statements
	 * it prepared (database round trips: N+1 regressions show up here) and how many entities it
	 * loaded (in-memory pagination and over-fetching show up here).
	 */
	protected <T> Measured<T> measure(Supplier<T> action) {
		flushAndClear();
		Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		statistics.clear();
		T result = action.get();
		return new Measured<>(result, statistics.getPrepareStatementCount(), statistics.getEntityLoadCount());
	}

	protected record Measured<T>(T result, long statements, long entitiesLoaded) {
	}
}
