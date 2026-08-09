package com.fap.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Single home for the domain-specific meters {@code monitoring.md} requires: quiz submission
 * latency, authentication failures, training registration conflicts / waitlisting, and file upload
 * outcomes. HTTP request rate/error/duration and HikariCP pool usage are supplied by Spring Boot
 * auto-configuration once a {@code MeterRegistry} is on the classpath, so they are deliberately
 * not restated here. Flyway migration status is reported in the startup logs as required by
 * {@code monitoring.md}; Spring Boot does not auto-configure a Flyway Prometheus meter.
 *
 * <p>Centralising the meter names (rather than scattering string literals through the services)
 * keeps them consistent, keeps tag cardinality low and reviewable in one place, and lets each
 * service depend on one small collaborator that is trivial to build from a {@code SimpleMeterRegistry}
 * in a unit test.
 */
@Component
public class DomainMetrics {

	private final Timer quizSubmitTimer;
	private final Counter authLoginFailureCounter;
	private final MeterRegistry registry;

	public DomainMetrics(MeterRegistry registry) {
		this.registry = registry;
		this.quizSubmitTimer = Timer.builder("fap.quiz.attempt.submit")
				.description("Latency of a quiz attempt submission, including grading")
				.register(registry);
		this.authLoginFailureCounter = Counter.builder("fap.auth.login.failures")
				.description("Rejected username/password login attempts")
				.register(registry);
	}

	/**
	 * Times a quiz submission. The whole grading path is measured, since latency the trainee feels
	 * is what matters, not just the persistence call.
	 */
	public <T> T recordQuizSubmit(Supplier<T> submission) {
		return quizSubmitTimer.record(submission);
	}

	/** One rejected login (bad credentials). Not incremented for a successful login. */
	public void recordLoginFailure() {
		authLoginFailureCounter.increment();
	}

	/**
	 * Outcome of a training registration attempt. {@code outcome} is a closed set
	 * (registered | waitlisted | conflict | promoted) so the series stays low-cardinality.
	 */
	public void recordRegistrationOutcome(RegistrationOutcome outcome) {
		Counter.builder("fap.training.registration")
				.description("Training registration attempts by outcome")
				.tag("outcome", outcome.tag())
				.register(registry)
				.increment();
	}

	/**
	 * Outcome of a file upload. One series per result value, so Prometheus shows the required
	 * {@code file_upload_total{result="success"}} / {@code {result="failure"}} counters.
	 */
	public void recordUpload(boolean success) {
		Counter.builder("file.upload")
				.description("File upload attempts by result")
				.tag("result", success ? "success" : "failure")
				.register(registry)
				.increment();
	}

	/** Closed set of training-registration outcomes tracked as a metric tag. */
	public enum RegistrationOutcome {
		REGISTERED("registered"),
		WAITLISTED("waitlisted"),
		CONFLICT("conflict"),
		PROMOTED("promoted");

		private final String tag;

		RegistrationOutcome(String tag) {
			this.tag = tag;
		}

		String tag() {
			return tag;
		}
	}

	/** Closed set of upload paths tracked as a metric tag. */
	public enum UploadType {
		MATERIAL("material"),
		AVATAR("avatar");

		private final String tag;

		UploadType(String tag) {
			this.tag = tag;
		}

		String tag() {
			return tag;
		}
	}
}
