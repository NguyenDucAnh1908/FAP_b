# 9. Backend Scaffold Checklist

Use this checklist when creating the Java Spring backend project.

---

## 9.1 Required Inputs

- `07_scope_freeze.md` for frozen modules, entities, APIs, roles, workflows.
- `06_business_logic_review.md` for corrected business rules.
- `08_backend_spring_boot_project_blueprint.md` for Java Spring project conventions.
- `database/flyway/V1__create_fap_schema.sql` for first migration.

---

## 9.2 Scaffold Commands

Use Spring Initializr or equivalent with:

```text
Project: Maven
Language: Java
Spring Boot: 3.x
Java: 21
Packaging: Jar
Group: com.fap
Artifact: fap-backend
Name: fap-backend
Package: com.fap
```

Initial dependencies:

```text
Spring Web
Spring Validation
Spring Security
Spring Data JPA
Spring Boot Actuator
Flyway Migration
Oracle Driver
Springdoc OpenAPI
Lombok
MapStruct
```

Test dependencies:

```text
Spring Boot Test
Spring Security Test
Mockito
Testcontainers
```

---

## 9.3 Files to Copy from Docs

```text
docs/database/flyway/V1__create_fap_schema.sql
-> fap-backend/src/main/resources/db/migration/V1__create_fap_schema.sql
```

Optional DBA/reference files:

```text
docs/database/oracle/schema.sql
docs/database/oracle/indexes.sql
docs/database/oracle/constraints_validation.sql
docs/database/liquibase/db.changelog-master.xml
```

---

## 9.4 First Commit Scope

First backend commit should include only:

- Maven project skeleton.
- `application.yaml`, `application-local.yaml`, `application-test.yaml`.
- Flyway migration V1.
- Common response/error envelope.
- Global exception handler.
- Security placeholder config that protects all `/api/v1/**`.
- Actuator health endpoint.
- OpenAPI config.
- Empty package skeleton for frozen modules.

Do not implement domain APIs in the first scaffold commit.

---

## 9.5 Validation Before Continuing

Run:

```bash
mvn clean test
mvn spring-boot:run
```

Check:

- App starts.
- Flyway applies V1 migration.
- JPA `ddl-auto=validate` does not fail.
- `/actuator/health` returns UP.
- `/swagger-ui/index.html` loads.
- Protected API returns `401` without token.

---

## 9.6 Non-Negotiable Rules

- Do not use `spring.jpa.hibernate.ddl-auto=update`.
- Do not expose JPA entities directly in controller responses.
- Do not put business rules in controllers.
- Do not compare permission levels ordinally.
- Do not skip ownership checks.
- Do not add endpoints outside `07_scope_freeze.md` without updating scope docs first.
- Do not manually edit DB schema outside migrations.
