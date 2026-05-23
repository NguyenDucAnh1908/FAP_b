# Technology Stack

## Approved Backend Stack

| Layer | Primary Choice |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.x |
| Build Tool | Maven wrapper |
| API | Spring Web MVC REST |
| Validation | Jakarta Bean Validation |
| Security | Spring Security |
| Auth | JWT access token + refresh token table |
| Password Hashing | BCrypt |
| ORM | Spring Data JPA + Hibernate |
| Database | Oracle 19c+ |
| Migrations | Flyway |
| API Docs | Springdoc OpenAPI / Swagger UI |
| Mapping | MapStruct |
| Boilerplate | Lombok optional; prefer records for DTOs |
| Logging | Logback structured JSON when configured |
| Metrics | Spring Boot Actuator + Micrometer |
| Tracing | OpenTelemetry when infrastructure is ready |
| Tests | JUnit 5, Mockito, Spring Boot Test, MockMvc, Testcontainers |

## Dependency Rules

- Add dependencies only when they solve a current requirement.
- Prefer Spring Boot starters and maintained Spring integrations.
- Do not add a second ORM or migration tool for runtime use.
- Flyway is the primary migration mechanism. Liquibase files in docs are DBA/reference artifacts unless the team explicitly switches.
- Redis, S3 SDK, RabbitMQ, and OpenTelemetry exporters are added only when their modules are implemented.

## Required Runtime Endpoints

- Health: `/actuator/health`
- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`

## Environment

Use upper snake case environment variables. See `.env.example`.

Do not commit real secrets or local `.env` files.
