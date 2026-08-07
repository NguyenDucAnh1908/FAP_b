package com.fap.common.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Rate limit rules for the authentication endpoints.
 *
 * <p>Rules are matched in declaration order, so put the most specific path first. A request that
 * matches no rule is not limited: this is a targeted control for credential guessing and OTP
 * brute force, not a general traffic shaper.
 * </p>
 *
 * @param enabled             master switch; the test profile turns it off so suites do not throttle
 *                            themselves
 * @param maxTrackedKeys      upper bound on distinct client keys held in memory. Without a bound, a
 *                            source rotating IPs would grow the bucket map until the heap runs out,
 *                            turning a rate limit into a denial of service.
 * @param keyIdleTtl          how long an unused key keeps its bucket. Must be at least as long as
 *                            the longest {@code refillPeriod}, otherwise a bucket is evicted while
 *                            still partially drained and the client silently gets a fresh
 *                            allowance.
 * @param trustForwardHeaders whether to read the client address from {@code X-Forwarded-For}.
 *                            Defaults to false because that header is client-supplied: when the app
 *                            is reachable directly, an attacker rotates the header value and every
 *                            request lands on a fresh bucket, which removes the limit entirely.
 *                            Enable it only behind a reverse proxy that overwrites the header.
 * @param rules               per-path limits
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
		boolean enabled,
		int maxTrackedKeys,
		Duration keyIdleTtl,
		boolean trustForwardHeaders,
		List<Rule> rules) {

	public RateLimitProperties {
		maxTrackedKeys = maxTrackedKeys > 0 ? maxTrackedKeys : 50_000;
		keyIdleTtl = keyIdleTtl != null ? keyIdleTtl : Duration.ofMinutes(30);
		rules = rules != null ? List.copyOf(rules) : List.of();
	}

	/**
	 * @param path          request path prefix, for example {@code /api/v1/auth/login}
	 * @param method        HTTP method to limit, or {@code null}/blank for every method
	 * @param capacity      requests allowed per {@code refillPeriod}
	 * @param refillPeriod  window after which the full capacity is restored
	 */
	public record Rule(
			String path,
			String method,
			int capacity,
			Duration refillPeriod) {

		public Rule {
			if (path == null || path.isBlank()) {
				throw new IllegalArgumentException("app.rate-limit.rules[].path is required");
			}
			if (capacity <= 0) {
				throw new IllegalArgumentException(
						"app.rate-limit.rules[].capacity must be positive for path " + path);
			}
			if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
				throw new IllegalArgumentException(
						"app.rate-limit.rules[].refill-period must be positive for path " + path);
			}
			method = method == null || method.isBlank() ? null : method.toUpperCase();
		}

		boolean matches(String requestPath, String requestMethod) {
			if (method != null && !method.equalsIgnoreCase(requestMethod)) {
				return false;
			}
			return requestPath.startsWith(path);
		}
	}

	Rule findRule(String requestPath, String requestMethod) {
		for (Rule rule : rules) {
			if (rule.matches(requestPath, requestMethod)) {
				return rule;
			}
		}
		return null;
	}
}
