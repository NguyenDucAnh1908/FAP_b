package com.fap.common.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Local-only Flyway migration strategy that runs {@code repair()} before {@code migrate()}.
 *
 * <p>Spring Boot 3.4.x does not expose {@code spring.flyway.repair-on-migrate} in its
 * auto-configuration properties. Without this bean a FAILED row left in
 * {@code flyway_schema_history} by a previous aborted migration would block every
 * subsequent startup with a {@link org.flywaydb.core.api.exception.FlywayValidateException}.
 * </p>
 *
 * <p>Restricted to the {@code local} profile on purpose. {@code repair()} rewrites the schema
 * history to match whatever is currently on the classpath: it realigns checksums of edited
 * migrations and marks missing ones as DELETED. That turns a deploy of the wrong artifact into a
 * silent success instead of a loud {@code FlywayValidateException}, so outside development the
 * default strategy (plain {@code migrate()} with validation) must stay in place.
 * </p>
 */
@Configuration
@Profile("local")
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();   // removes FAILED rows & realigns checksums
            flyway.migrate();
        };
    }
}
