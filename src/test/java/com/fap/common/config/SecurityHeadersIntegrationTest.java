package com.fap.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.MOCK,
		properties = {
				"management.health.db.enabled=false",
				"management.metrics.tags.env=test"
		})
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
class SecurityHeadersIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void httpResponse_hasExplicitSafeHeadersWithoutHsts() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
				.andExpect(header().doesNotExist("Strict-Transport-Security"));
	}

	@Test
	void httpsResponse_hasLongLivedHsts() throws Exception {
		mockMvc.perform(get("/actuator/health").secure(true))
				.andExpect(status().isOk())
				.andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")))
				.andExpect(header().string("Strict-Transport-Security", not(containsString("includeSubDomains"))))
				.andExpect(header().string("Strict-Transport-Security", not(containsString("preload"))));
	}

	@Test
	void swaggerUi_remainsPublicAndReceivesSecurityHeaders() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html").secure(true))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/swagger-ui/swagger-ui/index.html"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));

		mockMvc.perform(get("/swagger-ui/swagger-ui/index.html").secure(true))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
	}
}
