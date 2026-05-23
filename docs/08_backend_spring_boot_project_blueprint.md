# 8. Backend Spring Boot Project Blueprint

This document adapts the `.claude` rules to the FAP backend implementation using Java Spring Boot. The `.claude` folder was written mostly for Node.js/Express, so this blueprint keeps the engineering rules and maps the technology choices to Java equivalents.

---

## 8.1 What to Apply from `.claude`

| `.claude` Source | Apply to Spring Backend | Decision |
|---|---|---|
| `CLAUDE.md` workflow | `/spec -> /plan -> /build -> /test -> /review` | Keep |
| `agents/backend.md` layered API design | Controller -> Service -> Repository -> Database | Keep, adapt to Spring |
| `rules/project-structure.md` | Layered architecture and folder discipline | Keep, adapt package names |
| `rules/api-conventions.md` | REST, `/api/v1`, status codes, response envelope | Keep |
| `rules/database.md` | Migrations, transactions, no SQL in business logic, indexes | Keep, adapt Prisma to JPA/Flyway |
| `rules/security.md` | No secrets, JWT, bcrypt, rate limiting, authorization/ownership | Keep |
| `rules/error-handling.md` | Centralized error handling and consistent error response | Keep |
| `rules/testing.md` | Unit/integration tests, 80% target, regression tests | Keep, adapt to JUnit/Testcontainers |
| `rules/monitoring.md` | Structured logs, metrics, tracing, alerts | Keep, adapt to Actuator/Micrometer/OpenTelemetry |
| `rules/naming-conventions.md` | snake_case DB, versioned APIs, env naming | Keep |
| `rules/tech-stack.md` | Approved stack is Node-specific | Replace with Java Spring stack below |

---

## 8.2 Java Spring Backend Stack

| Layer | Decision |
|---|---|
| Runtime | Java 21 LTS |
| Framework | Spring Boot 3.x |
| Build Tool | Maven |
| API | Spring Web MVC REST |
| Validation | Jakarta Validation |
| ORM | Spring Data JPA + Hibernate |
| Database | Oracle 19c+ |
| Migration | Flyway primary; Liquibase also generated for teams that prefer it |
| ID Generation | Explicit Oracle sequences with JPA `GenerationType.SEQUENCE` |
| Security | Spring Security, JWT access token, refresh token table, BCrypt |
| API Docs | springdoc-openapi / Swagger UI |
| Mapping | MapStruct |
| Boilerplate | Lombok optional; prefer records for request/response DTOs where practical |
| Logging | Logback structured JSON |
| Metrics | Spring Actuator + Micrometer + Prometheus |
| Tracing | OpenTelemetry |
| Tests | JUnit 5, Mockito, Spring Boot Test, Testcontainers Oracle-compatible DB where available |
| Cache | Redis via Spring Data Redis, only after service layer is stable |
| Queue/Async | Spring `@Async` for simple async; RabbitMQ only if v1 needs durable async workflows |
| File Storage | S3-compatible storage abstraction |

---

## 8.3 Project Root Layout

Recommended backend root:

```text
fap-backend/
├── .claude/
│   └── copied or referenced project AI rules
├── docs/
│   ├── architecture/
│   ├── api/
│   └── database/
├── src/
│   ├── main/
│   │   ├── java/com/fap/
│   │   │   ├── FapApplication.java
│   │   │   ├── common/
│   │   │   ├── auth/
│   │   │   ├── user/
│   │   │   ├── role/
│   │   │   ├── syllabus/
│   │   │   ├── program/
│   │   │   ├── clazz/
│   │   │   ├── quiz/
│   │   │   ├── calendar/
│   │   │   ├── notification/
│   │   │   ├── settings/
│   │   │   └── storage/
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── application-local.yaml
│   │       ├── application-test.yaml
│   │       ├── db/migration/
│   │       │   └── V1__create_fap_schema.sql
│   │       └── logback-spring.xml
│   └── test/
│       └── java/com/fap/
├── .env.example
├── .gitignore
├── docker-compose.yml
├── pom.xml
└── README.md
```

Use `clazz` as package name because `class` is a Java keyword.

---

## 8.4 Package Structure

Use feature-first packages with common infrastructure separated.

```text
com.fap/
├── common/
│   ├── api/
│   │   ├── ApiResponse.java
│   │   ├── PageResponse.java
│   │   └── ErrorResponse.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── CorsConfig.java
│   │   ├── OpenApiConfig.java
│   │   └── AsyncConfig.java
│   ├── exception/
│   │   ├── BusinessException.java
│   │   ├── ConflictException.java
│   │   ├── ForbiddenException.java
│   │   ├── NotFoundException.java
│   │   └── GlobalExceptionHandler.java
│   ├── security/
│   │   ├── JwtService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── PermissionEvaluator.java
│   │   └── CurrentUser.java
│   ├── audit/
│   │   ├── AuditAspect.java
│   │   └── Auditable.java
│   └── util/
│       ├── ClockProvider.java
│       └── FileValidator.java
├── auth/
│   ├── controller/
│   ├── dto/
│   └── service/
├── user/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── mapper/
│   ├── repository/
│   └── service/
└── ...same pattern for role, syllabus, program, clazz, quiz, calendar
```

Per feature:
- `controller`: HTTP only, no business rule.
- `dto`: request/response objects.
- `entity`: JPA entities only.
- `repository`: Spring Data repositories and query specs.
- `service`: transaction boundary and business logic.
- `mapper`: MapStruct mapping.
- `enums`: status/permission enums when useful.

---

## 8.5 Dependency Rules

Allowed direction:

```text
controller -> service -> repository -> database
             service -> mapper
all layers -> common
```

Forbidden:
- Controller calling repository directly.
- Repository containing business status transition rules.
- Entity exposed directly as API response.
- Cross-feature writes without going through the owning service.
- Raw SQL in service classes.

Raw SQL is allowed only in:
- Flyway/Liquibase migrations.
- Repository-level custom queries when JPA cannot express the query cleanly.
- Performance-critical queries after review.

---

## 8.6 Database Setup

Use generated artifacts:

| Purpose | File |
|---|---|
| Canonical Oracle DDL | `docs/database/oracle/schema.sql` |
| Flyway migration | `docs/database/flyway/V1__create_fap_schema.sql` |
| Liquibase changelog | `docs/database/liquibase/db.changelog-master.xml` |
| Index/constraint inventory | `docs/database/indexes_and_constraints.md` |

Spring backend should copy:

```text
docs/database/flyway/V1__create_fap_schema.sql
-> src/main/resources/db/migration/V1__create_fap_schema.sql
docs/database/flyway/V2__create_sequences_and_seed_initial_roles_and_admin.sql
-> src/main/resources/db/migration/V2__create_sequences_and_seed_initial_roles_and_admin.sql
```

Recommended Flyway config:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: "0.1"
  jpa:
    hibernate:
      ddl-auto: validate
```

Important:
- Do not use `ddl-auto: update` for this project.
- Schema changes must be migration files.
- `baseline-version: "0.1"` is used for local Oracle schemas that are not empty yet still need V1 to run before V2.
- Keep Oracle DDL as source of truth unless the team intentionally switches to Liquibase.

---

## 8.7 API Conventions for Spring

Base path:

```text
/api/v1
```

Response envelope:

```json
{
  "success": true,
  "data": {},
  "message": "Optional"
}
```

Error envelope:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": []
  }
}
```

Pagination:

```json
{
  "success": true,
  "data": [],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 100,
    "totalPages": 5
  }
}
```

Spring implementation:
- Use `@RestController`.
- Use `@RequestMapping("/api/v1/...")`.
- Use `@Valid` on request DTOs.
- Use `Pageable` internally, but expose `page`, `limit`, `sortBy`, `order` consistently.
- Use `@Operation` and `@ApiResponses` for OpenAPI.

---

## 8.8 Security Rules for Spring

Must implement:
- Spring Security filter chain.
- JWT access token with short TTL.
- Refresh token rotation using `refresh_tokens`.
- BCrypt password hashing.
- CORS whitelist from environment.
- Rate limiting for auth endpoints.
- Resource permission check by action, not ordinal level.
- Ownership checks after permission check.

Permission model:

| Stored level | Allowed actions |
|---|---|
| `access_denied` | none |
| `view` | `read` |
| `create` | `read`, `create` |
| `modify` | `read`, `update`, `transition` |
| `full_access` | `read`, `create`, `update`, `transition`, `delete`, `admin` |

Security annotations should express action:

```java
@PreAuthorize("@permissionEvaluator.hasAction(authentication, 'syllabus', 'create')")
```

Ownership examples:
- Trainer can update attendance only for assigned sessions.
- Class Admin can manage only administered classes.
- Trainee can access only own profile, registrations, attempts, and assigned quizzes.
- Super Admin bypasses ownership checks.

---

## 8.9 Error Handling

Use centralized exception handling:

```text
GlobalExceptionHandler
├── MethodArgumentNotValidException -> 422 VALIDATION_ERROR
├── ConstraintViolationException -> 422 VALIDATION_ERROR
├── NotFoundException -> 404 RESOURCE_NOT_FOUND
├── ForbiddenException -> 403 ACCESS_DENIED
├── ConflictException -> 409 BUSINESS_CONFLICT
├── BadCredentialsException -> 401 UNAUTHORIZED
└── Exception -> 500 INTERNAL_ERROR
```

Rules:
- Do not expose stack traces to clients.
- Log unexpected errors with request ID.
- Use domain-specific error codes.
- Business rule violation should usually be `409 Conflict`.
- Validation failure should be `422 Unprocessable Entity`.

---

## 8.10 Testing Strategy

Minimum test set before feature is considered done:

| Test Type | Tool | Scope |
|---|---|---|
| Unit | JUnit 5 + Mockito | Services, permission evaluator, state machines |
| Repository | `@DataJpaTest` | JPA mappings, custom queries |
| API integration | `@SpringBootTest` + MockMvc | Controllers, security, validation |
| Migration validation | Flyway + test DB | Schema boots and JPA validates |
| Security regression | MockMvc | Forbidden/ownership cases |

Coverage target:
- 80% minimum for service/domain logic.
- Every bug fix must include regression test.
- Every status transition must have allowed and rejected tests.
- Every permission level must have action mapping tests.

High-priority test suites:
- `PermissionEvaluatorTest`
- `SyllabusStateMachineTest`
- `TrainingProgramStateMachineTest`
- `ClassStateMachineTest`
- `QuizStateMachineTest`
- `RegistrationConcurrencyTest`
- `QuizAttemptEligibilityTest`
- `AttendanceOwnershipTest`

---

## 8.11 Monitoring and Logging

Use:
- Spring Boot Actuator.
- Micrometer Prometheus registry.
- Structured JSON logs.
- Request ID correlation.
- OpenTelemetry tracing when infrastructure is ready.

Required metrics:
- HTTP request rate, errors, duration.
- DB connection pool usage.
- Flyway migration status at startup.
- Auth failure count.
- Quiz attempt submission latency.
- Training registration conflict/waitlist count.
- File upload success/failure count.

Do not log:
- Passwords.
- JWT/refresh tokens.
- Authorization headers.
- Raw uploaded file content.
- Sensitive PII beyond required audit metadata.

---

## 8.12 Environment Variables

Use upper snake case.

```bash
APP_NAME=fap-backend
APP_ENV=local
SERVER_PORT=8080

DB_HOST=localhost
DB_PORT=1521
DB_SERVICE=XEPDB1
DB_USERNAME=fap
DB_PASSWORD=change_me
DB_POOL_MAX=20

JWT_SECRET=change_me
JWT_ACCESS_TTL_MINUTES=15
JWT_REFRESH_TTL_DAYS=7
BCRYPT_STRENGTH=12

CORS_ALLOWED_ORIGINS=http://localhost:5173

REDIS_HOST=localhost
REDIS_PORT=6379

S3_BUCKET=fap-materials
S3_REGION=ap-southeast-1
S3_ACCESS_KEY=change_me
S3_SECRET_KEY=change_me

LOG_LEVEL=INFO
```

Do not commit real `.env` files.

---

## 8.13 Maven Dependencies

Initial dependencies:

```text
spring-boot-starter-web
spring-boot-starter-validation
spring-boot-starter-security
spring-boot-starter-data-jpa
spring-boot-starter-actuator
flyway-core
flyway-database-oracle
oracle jdbc driver
springdoc-openapi-starter-webmvc-ui
mapstruct
lombok
jjwt-api / jjwt-impl / jjwt-jackson
spring-boot-starter-test
spring-security-test
testcontainers
```

Add later only when needed:
- `spring-boot-starter-data-redis`
- AWS S3 SDK
- OpenTelemetry exporter
- RabbitMQ starter

---

## 8.14 Build Order

Implement in this order:

1. Spring Boot skeleton, Maven, profiles, health endpoint.
2. Flyway migration copied from generated DB artifact.
3. JPA base entity/audit fields and enum mappings.
4. Common API envelope and exception handler.
5. Auth and security foundation.
6. User/role/permission module.
7. Syllabus module with state machine.
8. Training program module.
9. Class module.
10. Quiz/question/assignment/attempt module.
11. Training calendar registration/attendance module.
12. Notification/settings/audit module.
13. File storage module.
14. Monitoring, OpenAPI polish, test hardening.

Rationale: permissions and identity must exist before protected domain modules.

---

## 8.15 Definition of Done

A backend feature is done only when:
- API contract matches `07_scope_freeze.md`.
- Business rule matches `06_business_logic_review.md`.
- Database mapping matches generated Oracle schema.
- Controller has validation and OpenAPI annotations.
- Service has transaction boundary where needed.
- Permission and ownership checks are implemented.
- Unit and integration tests pass.
- Error response follows the standard envelope.
- Logs do not expose sensitive data.
