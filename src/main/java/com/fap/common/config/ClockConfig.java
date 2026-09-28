package com.fap.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Time source for services whose rules depend on "now" (deadlines, windows, expiry), so their unit
 * tests can use a fixed clock. Uses the JVM default zone, like the {@code LocalDateTime.now()} calls
 * it replaces, so stored timestamps do not shift.
 */
@Configuration
public class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.systemDefaultZone();
	}
}
