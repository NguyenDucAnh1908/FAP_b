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
