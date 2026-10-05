package com.fap.common.security.ratelimit;

import com.fap.common.api.ErrorResponse;
import com.fap.common.i18n.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Token-bucket rate limiter for the authentication endpoints.
 *
 * <p>Runs before Spring Security so a rejected request never reaches
 * {@code DaoAuthenticationProvider} and therefore never costs a BCrypt verification. That matters:
 * BCrypt is deliberately slow, so an unthrottled login endpoint is both a credential-guessing
 * surface and a cheap way to saturate the CPU.
 * </p>
 *
 * <p>Buckets live in a Caffeine cache keyed by client address plus rule path, bounded by
 * {@code maxTrackedKeys} and expired after {@code keyIdleTtl}. State is per-instance; with more
 * than one replica the effective limit multiplies by the replica count. That is an accepted
 * tradeoff for now — a shared store would mean adding Redis, which {@code tech-stack.md} defers
 * until a module needs it.
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

	private final RateLimitProperties properties;
	private final MessageService messageService;
	private final ObjectMapper objectMapper;
	private final Cache<String, Bucket> buckets;

	public RateLimitFilter(
			RateLimitProperties properties,
			MessageService messageService,
			ObjectMapper objectMapper) {
		this.properties = properties;
		this.messageService = messageService;
		this.objectMapper = objectMapper;
		this.buckets = Caffeine.newBuilder()
				.maximumSize(properties.maxTrackedKeys())
				.expireAfterAccess(properties.keyIdleTtl().toMillis(), TimeUnit.MILLISECONDS)
				.build();
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !properties.enabled();
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		RateLimitProperties.Rule rule = properties.findRule(request.getRequestURI(), request.getMethod());
		if (rule == null) {
			filterChain.doFilter(request, response);
			return;
		}

		String key = clientKey(request) + "|" + rule.path();
		Bucket bucket = buckets.get(key, ignored -> newBucket(rule));
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

		if (probe.isConsumed()) {
			response.setHeader("X-RateLimit-Limit", String.valueOf(rule.capacity()));
			response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
			filterChain.doFilter(request, response);
			return;
		}

		long retryAfterSeconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
		// Path and method only. The key contains a client address, and the body may contain
		// credentials, so neither belongs in a log line (security.md).
		log.warn("Rate limit exceeded for {} {}, retry after {}s",
				request.getMethod(), request.getRequestURI(), retryAfterSeconds);
		writeTooManyRequests(response, rule, retryAfterSeconds);
	}

	private Bucket newBucket(RateLimitProperties.Rule rule) {
		// refillIntervally, not refillGreedy: the whole allowance returns at the end of the window
		// instead of trickling back token by token, which is what a "N attempts per minute" limit
		// means to an operator reading the config.
		return Bucket.builder()
				.addLimit(limit -> limit
						.capacity(rule.capacity())
						.refillIntervally(rule.capacity(), rule.refillPeriod()))
				.build();
	}

	private void writeTooManyRequests(
			HttpServletResponse response,
			RateLimitProperties.Rule rule,
			long retryAfterSeconds) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
		response.setHeader("X-RateLimit-Limit", String.valueOf(rule.capacity()));
		response.setHeader("X-RateLimit-Remaining", "0");

		ErrorResponse body = ErrorResponse.of(
				"RATE_LIMIT_EXCEEDED",
				messageService.get("error.RATE_LIMIT_EXCEEDED", retryAfterSeconds));
		objectMapper.writeValue(response.getOutputStream(), body);
	}

	/**
	 * Identifies the caller for bucket lookup.
	 *
	 * <p>{@code X-Forwarded-For} is only consulted when {@code trustForwardHeaders} is enabled,
	 * because any client can set it. Reading it unconditionally would let an attacker send a fresh
	 * value per request and never hit a limit.
	 * </p>
	 */
	private String clientKey(HttpServletRequest request) {
		if (properties.trustForwardHeaders()) {
			String forwarded = request.getHeader("X-Forwarded-For");
			if (forwarded != null && !forwarded.isBlank()) {
				// Left-most entry is the original client; the rest are proxy hops.
				return forwarded.split(",")[0].trim();
			}
		}
		String remoteAddr = request.getRemoteAddr();
		return remoteAddr != null ? remoteAddr : "unknown";
	}
}
