package com.fap.common.util;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class ClockProvider {

	private final Clock clock = Clock.systemUTC();

	public Instant now() {
		return Instant.now(clock);
	}
}
