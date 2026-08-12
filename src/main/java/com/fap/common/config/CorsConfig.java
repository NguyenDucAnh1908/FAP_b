package com.fap.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(split(allowedOrigins));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		// Browser JS cannot read a response header unless it is explicitly exposed, so without this
		// the correlation id set by RequestIdFilter would be invisible to the SPA and a user could
		// not quote it in a bug report. Rate limit headers are exposed for the same reason: the
		// client needs Retry-After to back off correctly. Download metadata is exposed so the
		// SPA can preserve the server-provided file name and size.
		configuration.setExposedHeaders(List.of(
				"Content-Disposition",
				"Content-Length",
				"X-Request-Id",
				"Retry-After",
				"X-RateLimit-Limit",
				"X-RateLimit-Remaining"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private List<String> split(String value) {
		return Arrays.stream(value.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isBlank())
				.toList();
	}
}
