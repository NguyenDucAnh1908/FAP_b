package com.fap.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

	@Test
	void corsExposesHeadersNeededByBrowserDownloads() {
		CorsConfigurationSource source = new CorsConfig()
				.corsConfigurationSource("http://localhost:5173");
		MockHttpServletRequest request = new MockHttpServletRequest(
				"GET",
				"/api/v1/materials/1/download");

		CorsConfiguration configuration = source.getCorsConfiguration(request);

		assertThat(configuration).isNotNull();
		assertThat(configuration.getExposedHeaders())
				.contains("Content-Disposition", "Content-Length", "X-Request-Id");
	}
}
