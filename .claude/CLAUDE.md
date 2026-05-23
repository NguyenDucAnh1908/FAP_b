# FAP Backend AI Agent Configuration

## Project Context

This repository is the FAP backend service.

- Runtime: Java 21
- Framework: Spring Boot 3.x
- Build: Maven wrapper
- Package root: `com.fap`
- Database: Oracle 19c+
- ORM: Spring Data JPA + Hibernate
- Migration: Flyway
- API docs: Springdoc OpenAPI / Swagger UI
- Main source: `src/main/java/com/fap`
- Tests: `src/test/java/com/fap`

The backend contract is defined by:

- `docs/07_scope_freeze.md`
- `docs/06_business_logic_review.md`
- `docs/08_backend_spring_boot_project_blueprint.md`
- `docs/database/README.md`

## Development Workflow

Use this workflow for feature work:

```text
/spec -> /plan -> /build -> /test -> /review
```

## Mandatory Rules

Follow all rules in `.claude/rules/`.

Core backend rules:

- Controllers only handle HTTP concerns.
- Services own business rules and transaction boundaries.
- Repositories own persistence access.
- JPA entities are never returned directly from controllers.
- All schema changes go through Flyway migrations.
- `spring.jpa.hibernate.ddl-auto=update` is forbidden.
- API paths are versioned under `/api/v1`.
- Permission checks are action-based, not ordinal comparisons.
- Ownership checks run after permission checks.
- Super Admin bypasses ownership checks only where explicitly allowed by scope docs.
- Do not add endpoints, entities, or workflows outside `docs/07_scope_freeze.md` without updating docs first.

## Build And Verification

Use Maven wrapper commands:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

The local machine must have JDK 21 installed and `JAVA_HOME` configured.

## Available Agents

Use these agents for backend work:

- `backend.md`: Spring Boot API, service, repository, database integration.
- `systems-architect.md`: architecture decisions and ADRs.
- `code-reviewer.md`: correctness, maintainability, security, performance review.
- `security-auditor.md`: authentication, authorization, secret handling, threat review.
- `test-engineer.md`: JUnit, Mockito, Spring Boot Test, Testcontainers strategy.
- `qa.md`: test scenarios and acceptance verification.
- `project-manager.md`: scope, slices, acceptance criteria.

## Agent Behavior

1. Read the scope and business-rule docs before implementing domain behavior.
2. Prefer existing project conventions over new abstractions.
3. Keep changes small and buildable.
4. Add focused tests for each behavior change.
5. Explain important tradeoffs before large structural edits.
6. Never hide failed verification. State exactly what command failed and why.
