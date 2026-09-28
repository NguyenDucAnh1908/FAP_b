package com.fap.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Puts a correlation id on every request so log lines from one request can be grouped.
 *
 * <p>Runs first in the chain: an id assigned after authentication would be missing from exactly the
 * lines that matter most when investigating a rejected request.
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String MDC_KEY = "requestId";
	public static final String HEADER = "X-Request-Id";

	/**
	 * An inbound id is echoed into responses and log files, so it is treated as untrusted input.
	 * Restricting it to this alphabet stops a caller from injecting newlines to forge extra log
	 * entries, or CR/LF into the response header.
	 */
	private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String requestId = resolveRequestId(request);
		MDC.put(MDC_KEY, requestId);
		response.setHeader(HEADER, requestId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			// Servlet threads are pooled. Leaving the key set would stamp the previous request's id
			// onto whatever runs next on this thread.
			MDC.remove(MDC_KEY);
		}
	}

	private String resolveRequestId(HttpServletRequest request) {
		String provided = request.getHeader(HEADER);
		if (provided != null && SAFE_ID.matcher(provided).matches()) {
			return provided;
		}
		return UUID.randomUUID().toString();
	}
}
