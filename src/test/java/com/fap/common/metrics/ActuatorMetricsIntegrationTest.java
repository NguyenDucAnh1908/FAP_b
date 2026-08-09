package com.fap.common.metrics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.fap.common.metrics.DomainMetrics.RegistrationOutcome.WAITLISTED;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class ActuatorMetricsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DomainMetrics domainMetrics;

	@Test
	void health_isPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	@Test
	void prometheus_withoutAuthenticationIsRejected() throws Exception {
		mockMvc.perform(get("/actuator/prometheus"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void prometheus_withAuthenticationExportsDomainMetricsAndGlobalTags() throws Exception {
		domainMetrics.recordLoginFailure();
		domainMetrics.recordQuizSubmit(() -> "graded");
		domainMetrics.recordRegistrationOutcome(WAITLISTED);
		domainMetrics.recordUpload(true);

		mockMvc.perform(get("/actuator/prometheus").with(user("metrics-scraper")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("fap_auth_login_failures_total")))
				.andExpect(content().string(containsString("fap_quiz_attempt_submit_seconds_count")))
				.andExpect(content().string(containsString("fap_training_registration_total")))
				.andExpect(content().string(containsString("file_upload_total")))
				.andExpect(content().string(containsString("application=\"fap-backend\"")))
				.andExpect(content().string(containsString("env=\"test\"")));
	}
}
