package com.fap.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.LifecycleProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class DeploymentReadinessIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ServerProperties serverProperties;

	@Autowired
	private LifecycleProperties lifecycleProperties;

	@Autowired
	private DataSource dataSource;

	@Test
	void shutdown_waitsForInFlightRequests() {
		assertThat(serverProperties.getShutdown()).isEqualTo(Shutdown.GRACEFUL);
		assertThat(lifecycleProperties.getTimeoutPerShutdownPhase()).isEqualTo(Duration.ofSeconds(30));
	}

	@Test
	void hikariPool_hasBoundedSizeAndExplicitTimeouts() {
		assertThat(dataSource).isInstanceOf(HikariDataSource.class);
		HikariDataSource hikari = (HikariDataSource) dataSource;

		assertThat(hikari.getMinimumIdle()).isEqualTo(5);
		assertThat(hikari.getMaximumPoolSize()).isEqualTo(20);
		assertThat(hikari.getConnectionTimeout()).isEqualTo(30_000L);
		assertThat(hikari.getValidationTimeout()).isEqualTo(5_000L);
		assertThat(hikari.getIdleTimeout()).isEqualTo(600_000L);
		assertThat(hikari.getMaxLifetime()).isEqualTo(1_800_000L);
		assertThat(hikari.getKeepaliveTime()).isZero();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"/actuator/health/liveness",
			"/actuator/health/readiness"
	})
	void healthProbe_isPublicAndUp(String path) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}
}
