package com.fap.common.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Flyway migration strategy that always runs {@code repair()} before {@code migrate()}.
 *
 * <p>Spring Boot 3.4.x does not expose {@code spring.flyway.repair-on-migrate} in its
 * auto-configuration properties. Without this bean a FAILED row left in
 * {@code flyway_schema_history} by a previous aborted migration would block every
 * subsequent startup with a {@link org.flywaydb.core.api.exception.FlywayValidateException}.
 * </p>
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();   // removes FAILED rows & realigns checksums
            flyway.migrate();
        };
    }
}
