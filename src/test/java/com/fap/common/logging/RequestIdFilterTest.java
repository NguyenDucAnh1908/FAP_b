package com.fap.common.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestIdFilterTest {

	private final RequestIdFilter filter = new RequestIdFilter();

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	@DisplayName("generates an id when the client sends none and echoes it in the response")
	void generatesIdWhenAbsent() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/users"), response, new MockFilterChain());

		String header = response.getHeader(RequestIdFilter.HEADER);
		assertThat(header).isNotBlank();
		assertThat(UUID.fromString(header)).isNotNull();
	}

	@Test
	@DisplayName("the id is visible in the MDC for the duration of the request")
	void exposesIdInMdcDuringRequest() throws Exception {
		AtomicReference<String> seenInsideChain = new AtomicReference<>();
		FilterChain chain = (request, response) -> seenInsideChain.set(MDC.get(RequestIdFilter.MDC_KEY));
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/users"), response, chain);

		assertThat(seenInsideChain.get()).isEqualTo(response.getHeader(RequestIdFilter.HEADER));
	}

	@Test
	@DisplayName("a well-formed client id is reused so a caller can correlate across services")
	void reusesValidClientProvidedId() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
		request.addHeader(RequestIdFilter.HEADER, "abc-123-DEF");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("abc-123-DEF");
	}

	@ParameterizedTest
	@DisplayName("a malformed or injection-shaped client id is replaced with a generated one")
	@ValueSource(strings = {
			"has space",
			"drop\nINFO forged log line",
			"crlf\r\nX-Injected: 1",
			"semi;colon",
			"under_score",
			"slash/path",
			"" // blank header
	})
	void rejectsUnsafeClientProvidedId(String provided) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
		request.addHeader(RequestIdFilter.HEADER, provided);
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		String header = response.getHeader(RequestIdFilter.HEADER);
		assertThat(header).isNotEqualTo(provided);
		assertThat(UUID.fromString(header)).isNotNull();
	}

	@Test
	@DisplayName("an over-long client id is replaced so log lines stay bounded")
	void rejectsOverlongClientProvidedId() throws Exception {
		String tooLong = "a".repeat(65);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
		request.addHeader(RequestIdFilter.HEADER, tooLong);
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getHeader(RequestIdFilter.HEADER)).isNotEqualTo(tooLong);
	}

	@Test
	@DisplayName("the MDC is cleared after a successful request so pooled threads do not leak the id")
	void clearsMdcAfterRequest() throws Exception {
		filter.doFilter(
				new MockHttpServletRequest("GET", "/api/v1/users"),
				new MockHttpServletResponse(),
				new MockFilterChain());

		assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
	}

	@Test
	@DisplayName("the MDC is cleared even when the chain throws")
	void clearsMdcWhenChainThrows() {
		FilterChain failing = (request, response) -> {
			throw new IllegalStateException("boom");
		};

		assertThatThrownBy(() -> filter.doFilter(
				new MockHttpServletRequest("GET", "/api/v1/users"),
				new MockHttpServletResponse(),
				failing))
				.isInstanceOf(IllegalStateException.class);

		// Without the finally block the next request served by this thread would log the failed
		// request's id.
		assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
	}
}
