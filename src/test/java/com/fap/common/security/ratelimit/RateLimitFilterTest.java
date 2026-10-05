package com.fap.common.security.ratelimit;

import com.fap.common.i18n.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

	private static final String LOGIN_PATH = "/api/v1/auth/login";

	private MessageService messageService;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		messageService = mock(MessageService.class);
		when(messageService.get(anyString(), any())).thenReturn("Too many requests");
		objectMapper = new ObjectMapper();
	}

	private RateLimitFilter filterAllowing(int capacity, Duration period, boolean trustForwardHeaders) {
		RateLimitProperties properties = new RateLimitProperties(
				true,
				1_000,
				Duration.ofMinutes(30),
				trustForwardHeaders,
				List.of(new RateLimitProperties.Rule(LOGIN_PATH, "POST", capacity, period)));
		return new RateLimitFilter(properties, messageService, objectMapper);
	}

	/**
	 * A fresh request and chain per call: {@code OncePerRequestFilter} marks the request as filtered,
	 * and {@code MockFilterChain} refuses a second invocation.
	 */
	private MockHttpServletResponse call(RateLimitFilter filter, String method, String path, String remoteAddr)
			throws Exception {
		return call(filter, method, path, remoteAddr, null);
	}

	private MockHttpServletResponse call(
			RateLimitFilter filter,
			String method,
			String path,
			String remoteAddr,
			String forwardedFor) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setRemoteAddr(remoteAddr);
		if (forwardedFor != null) {
			request.addHeader("X-Forwarded-For", forwardedFor);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response;
	}

	@Test
	@DisplayName("requests within capacity pass through and report remaining tokens")
	void allowsRequestsWithinCapacity() throws Exception {
		RateLimitFilter filter = filterAllowing(3, Duration.ofMinutes(1), false);

		MockHttpServletResponse first = call(filter, "POST", LOGIN_PATH, "10.0.0.1");
		MockHttpServletResponse second = call(filter, "POST", LOGIN_PATH, "10.0.0.1");

		assertThat(first.getStatus()).isEqualTo(HttpStatus.OK.value());
		assertThat(first.getHeader("X-RateLimit-Limit")).isEqualTo("3");
		assertThat(first.getHeader("X-RateLimit-Remaining")).isEqualTo("2");
		assertThat(second.getHeader("X-RateLimit-Remaining")).isEqualTo("1");
	}

	@Test
	@DisplayName("request beyond capacity is rejected with 429, Retry-After and the error envelope")
	void rejectsRequestBeyondCapacity() throws Exception {
		RateLimitFilter filter = filterAllowing(2, Duration.ofMinutes(1), false);

		call(filter, "POST", LOGIN_PATH, "10.0.0.1");
		call(filter, "POST", LOGIN_PATH, "10.0.0.1");
		MockHttpServletResponse blocked = call(filter, "POST", LOGIN_PATH, "10.0.0.1");

		assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
		assertThat(blocked.getHeader(HttpHeaders.RETRY_AFTER)).isNotNull();
		assertThat(Long.parseLong(blocked.getHeader(HttpHeaders.RETRY_AFTER))).isPositive();
		assertThat(blocked.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
		assertThat(blocked.getContentAsString())
				.contains("\"success\":false")
				.contains("RATE_LIMIT_EXCEEDED");
	}

	@Test
	@DisplayName("buckets are per client address so one caller cannot exhaust another's allowance")
	void isolatesBucketsByClientAddress() throws Exception {
		RateLimitFilter filter = filterAllowing(1, Duration.ofMinutes(1), false);

		call(filter, "POST", LOGIN_PATH, "10.0.0.1");
		MockHttpServletResponse sameClient = call(filter, "POST", LOGIN_PATH, "10.0.0.1");
		MockHttpServletResponse otherClient = call(filter, "POST", LOGIN_PATH, "10.0.0.2");

		assertThat(sameClient.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
		assertThat(otherClient.getStatus()).isEqualTo(HttpStatus.OK.value());
	}

	@Test
	@DisplayName("paths with no matching rule are never limited")
	void ignoresUnmatchedPaths() throws Exception {
		RateLimitFilter filter = filterAllowing(1, Duration.ofMinutes(1), false);

		call(filter, "POST", "/api/v1/users", "10.0.0.1");
		MockHttpServletResponse second = call(filter, "POST", "/api/v1/users", "10.0.0.1");

		assertThat(second.getStatus()).isEqualTo(HttpStatus.OK.value());
		assertThat(second.getHeader("X-RateLimit-Limit")).isNull();
	}

	@Test
	@DisplayName("a rule bound to POST does not limit other methods on the same path")
	void respectsMethodOnRule() throws Exception {
		RateLimitFilter filter = filterAllowing(1, Duration.ofMinutes(1), false);

		call(filter, "POST", LOGIN_PATH, "10.0.0.1");
		MockHttpServletResponse get = call(filter, "GET", LOGIN_PATH, "10.0.0.1");

		assertThat(get.getStatus()).isEqualTo(HttpStatus.OK.value());
	}

	@Test
	@DisplayName("X-Forwarded-For is ignored unless trust-forward-headers is enabled")
	void ignoresForwardedHeaderWhenUntrusted() throws Exception {
		RateLimitFilter filter = filterAllowing(1, Duration.ofMinutes(1), false);

		call(filter, "POST", LOGIN_PATH, "10.0.0.1", "203.0.113.1");
		MockHttpServletResponse rotatedHeader = call(filter, "POST", LOGIN_PATH, "10.0.0.1", "203.0.113.2");

		// Same socket address, so rotating the client-supplied header must not buy a fresh bucket.
		assertThat(rotatedHeader.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
	}

	@Test
	@DisplayName("X-Forwarded-For keys the bucket when trust-forward-headers is enabled")
	void usesForwardedHeaderWhenTrusted() throws Exception {
		RateLimitFilter filter = filterAllowing(1, Duration.ofMinutes(1), true);

		call(filter, "POST", LOGIN_PATH, "10.0.0.1", "203.0.113.1, 10.0.0.9");
		MockHttpServletResponse sameForwarded =
				call(filter, "POST", LOGIN_PATH, "10.0.0.1", "203.0.113.1, 10.0.0.9");
		MockHttpServletResponse otherForwarded =
				call(filter, "POST", LOGIN_PATH, "10.0.0.1", "203.0.113.2, 10.0.0.9");

		assertThat(sameForwarded.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
		assertThat(otherForwarded.getStatus()).isEqualTo(HttpStatus.OK.value());
	}

	@Test
	@DisplayName("disabling the limiter short-circuits the filter entirely")
	void skipsFilteringWhenDisabled() throws Exception {
		RateLimitProperties properties = new RateLimitProperties(
				false,
				1_000,
				Duration.ofMinutes(30),
				false,
				List.of(new RateLimitProperties.Rule(LOGIN_PATH, "POST", 1, Duration.ofMinutes(1))));
		RateLimitFilter filter = new RateLimitFilter(properties, messageService, objectMapper);

		call(filter, "POST", LOGIN_PATH, "10.0.0.1");
		MockHttpServletResponse second = call(filter, "POST", LOGIN_PATH, "10.0.0.1");

		assertThat(second.getStatus()).isEqualTo(HttpStatus.OK.value());
	}

	@Test
	@DisplayName("rule validation rejects a non-positive capacity")
	void rejectsInvalidCapacity() {
		assertThat(catchThrowable(() -> new RateLimitProperties.Rule(LOGIN_PATH, "POST", 0, Duration.ofMinutes(1))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("capacity");
	}

	@Test
	@DisplayName("rule validation rejects a blank path and a non-positive refill period")
	void rejectsInvalidPathAndPeriod() {
		assertThat(catchThrowable(() -> new RateLimitProperties.Rule(" ", "POST", 1, Duration.ofMinutes(1))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("path");
		assertThat(catchThrowable(() -> new RateLimitProperties.Rule(LOGIN_PATH, "POST", 1, Duration.ZERO)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("refill-period");
	}

	@Test
	@DisplayName("properties fall back to safe defaults for unset bounds")
	void appliesDefaultsForUnsetBounds() {
		RateLimitProperties properties = new RateLimitProperties(true, 0, null, false, null);

		assertThat(properties.maxTrackedKeys()).isPositive();
		assertThat(properties.keyIdleTtl()).isNotNull();
		assertThat(properties.rules()).isEmpty();
	}

	private static Throwable catchThrowable(Runnable runnable) {
		try {
			runnable.run();
			return null;
		} catch (Throwable throwable) {
			return throwable;
		}
	}
}
