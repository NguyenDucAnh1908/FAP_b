package com.fap.common.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.logback.LogbackLoggingSystem;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads logback-spring.xml the way a deployed (non-local, non-test) profile does. Unit tests run
 * with the {@code test} profile, so without this the JSON/masking branch is never parsed.
 */
@ExtendWith(OutputCaptureExtension.class)
class LogbackConfigurationTest {

	private final LogbackLoggingSystem loggingSystem = new LogbackLoggingSystem(getClass().getClassLoader());

	@AfterEach
	void restoreTestLogging() {
		initialize("test");
	}

	@Test
	@DisplayName("production profile config loads and masks secrets in JSON log lines")
	void productionProfileMasksSecrets(CapturedOutput output) {
		initialize("prod");

		LoggerFactory.getLogger(LogbackConfigurationTest.class)
				.warn("login failed password=hunter2, token: abc.def next=kept Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2ln");

		assertThat(output.getOut())
				.contains("\"level\":\"WARN\"")
				.contains("password=****")
				.contains("token: ****")
				.contains("next=kept")
				.contains("Bearer ****")
				.doesNotContain("hunter2")
				.doesNotContain("eyJhbGci");
	}

	private void initialize(String profile) {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles(profile);
		loggingSystem.cleanUp();
		loggingSystem.beforeInitialize();
		loggingSystem.initialize(new LoggingInitializationContext(environment), "classpath:logback-spring.xml", null);
	}
}
