package com.fap.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.fap.common.metrics.DomainMetrics.RegistrationOutcome.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the meter names, tag cardinality, and counter semantics required by monitoring.md.
 * Uses SimpleMeterRegistry to avoid any Prometheus/JMX dependency.
 */
class DomainMetricsTest {

	private SimpleMeterRegistry registry;
	private DomainMetrics metrics;

	@BeforeEach
	void setUp() {
		registry = new SimpleMeterRegistry();
		metrics = new DomainMetrics(registry);
	}

	// ── quiz submit timer ────────────────────────────────────────────────────

	@Test
	void recordQuizSubmit_timesSupplierAndReturnsValue() {
		String result = metrics.recordQuizSubmit(() -> "graded");

		assertThat(result).isEqualTo("graded");
		Timer timer = registry.find("fap.quiz.attempt.submit").timer();
		assertThat(timer).isNotNull();
		assertThat(timer.count()).isEqualTo(1);
	}

	@Test
	void recordQuizSubmit_stillCountsWhenSupplierThrows() {
		assertThatThrownBy(() -> metrics.recordQuizSubmit(() -> {
			throw new RuntimeException("grading failure");
		})).isInstanceOf(RuntimeException.class);

		Timer timer = registry.find("fap.quiz.attempt.submit").timer();
		assertThat(timer).isNotNull();
		assertThat(timer.count()).isEqualTo(1);
	}

	// ── auth login failure counter ───────────────────────────────────────────

	@Test
	void recordLoginFailure_incrementsCounter() {
		metrics.recordLoginFailure();
		metrics.recordLoginFailure();

		Counter counter = registry.find("fap.auth.login.failures").counter();
		assertThat(counter).isNotNull();
		assertThat(counter.count()).isEqualTo(2.0);
	}

	// ── training registration outcomes ──────────────────────────────────────

	@Test
	void recordRegistrationOutcome_eachOutcomeHasOwnTaggedSeries() {
		metrics.recordRegistrationOutcome(REGISTERED);
		metrics.recordRegistrationOutcome(REGISTERED);
		metrics.recordRegistrationOutcome(WAITLISTED);
		metrics.recordRegistrationOutcome(CONFLICT);
		metrics.recordRegistrationOutcome(PROMOTED);

		assertThat(counterValue("fap.training.registration", "outcome", "registered")).isEqualTo(2.0);
		assertThat(counterValue("fap.training.registration", "outcome", "waitlisted")).isEqualTo(1.0);
		assertThat(counterValue("fap.training.registration", "outcome", "conflict")).isEqualTo(1.0);
		assertThat(counterValue("fap.training.registration", "outcome", "promoted")).isEqualTo(1.0);
	}

	// ── file upload outcomes ─────────────────────────────────────────────────

	@Test
	void recordUpload_successAndFailureAreDistinctSeries() {
		metrics.recordUpload(true);
		metrics.recordUpload(true);
		metrics.recordUpload(false);

		assertThat(counterValue("file.upload", "result", "success")).isEqualTo(2.0);
		assertThat(counterValue("file.upload", "result", "failure")).isEqualTo(1.0);
	}

	@Test
	void recordUpload_onlyOneSeriesIncrementedPerCall() {
		metrics.recordUpload(true);

		assertThat(counterValue("file.upload", "result", "success")).isEqualTo(1.0);
		// failure series should not exist yet
		assertThat(registry.find("file.upload").tag("result", "failure").counter()).isNull();
	}

	// ── try/finally double-count protection ──────────────────────────────────

	@Test
	void uploadCountedFlag_preventsDoubleCountOnException() {
		// Simulate the try/finally pattern used by MaterialFileService and UserAvatarService:
		// IOException path counts failure, then finally-block must NOT count again.
		AtomicBoolean counted = new AtomicBoolean(false);
		try {
			// simulate IOException path
			metrics.recordUpload(false);
			counted.set(true);
			throw new RuntimeException("FILE_UNREADABLE");
		} catch (RuntimeException ignored) {
			// swallowed for test purposes
		} finally {
			if (!counted.get()) {
				metrics.recordUpload(false);
			}
		}

		assertThat(counterValue("file.upload", "result", "failure")).isEqualTo(1.0);
	}

	// ── helpers ──────────────────────────────────────────────────────────────

	private double counterValue(String name, String tagKey, String tagValue) {
		Counter counter = registry.find(name).tag(tagKey, tagValue).counter();
		return counter == null ? 0.0 : counter.count();
	}
}
