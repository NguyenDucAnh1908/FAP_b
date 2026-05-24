# FAP Backend

Spring Boot backend scaffold for the FAP v1 scope.

## Stack

- Java 21
- Spring Boot 3.x
- Maven
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Oracle JDBC
- Flyway
- Springdoc OpenAPI

## Local Setup

1. Install JDK 21 and set `JAVA_HOME`.
2. Start an Oracle 19c+ compatible database. Local Oracle XE defaults to `XEPDB1`.
3. Create a local `.env` from `.env.example` or export the same environment variables.
4. Run:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

## Important URLs

- Health: `GET /actuator/health`
- Swagger UI: `/swagger-ui/index.html`

## Local Logs

When running with the `local` profile, application logs are written to:

```text
logs/fap-backend-local.log
```

Useful overrides:

```bash
LOG_LEVEL=DEBUG APP_LOG_LEVEL=DEBUG ./mvnw spring-boot:run
LOG_FILE=logs/custom.log ./mvnw spring-boot:run
```

## Documentation

- Frozen backend scope: `docs/07_scope_freeze.md`
- Business rules: `docs/06_business_logic_review.md`
- Spring Boot blueprint: `docs/08_backend_spring_boot_project_blueprint.md`
- Scaffold checklist: `docs/09_backend_scaffold_checklist.md`
- Database artifacts: `docs/database/README.md`
