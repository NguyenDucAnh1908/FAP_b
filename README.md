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
- Liveness probe: `GET /actuator/health/liveness`
- Readiness probe: `GET /actuator/health/readiness`
- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`
- OpenAPI YAML: `/v3/api-docs.yaml`

## Production Runtime

The application uses graceful shutdown. After receiving `SIGTERM`, it stops accepting new
requests and gives active work up to `SERVER_SHUTDOWN_TIMEOUT` (default `30s`) per shutdown
phase to finish.

Use the health probes as follows:

- Liveness tells the runtime whether the process should be restarted.
- Readiness tells the load balancer whether this instance should receive traffic.

Both probe URLs are public and return only health status. Other Actuator endpoints remain
authenticated.

JPA batching is environment-tunable: `JPA_BATCH_SIZE` (JDBC insert/update batch, default `50`),
`JPA_BATCH_FETCH_SIZE` (lazy association batch fetch, default `50`) and `JPA_FETCH_SIZE` (rows per
JDBC round trip, default `100`). JSON responses of at least 2 KB are gzip-compressed; set
`SERVER_COMPRESSION_ENABLED=false` when a reverse proxy already compresses.

HikariCP settings are environment-tunable:

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_POOL_MIN` | `5` | Idle connections kept ready per instance |
| `DB_POOL_MAX` | `20` | Maximum Oracle connections per instance |
| `DB_POOL_CONNECTION_TIMEOUT_MS` | `30000` | Maximum wait for a connection from the pool |
| `DB_POOL_VALIDATION_TIMEOUT_MS` | `5000` | Maximum time to validate a connection |
| `DB_POOL_IDLE_TIMEOUT_MS` | `600000` | Time before an excess idle connection is retired |
| `DB_POOL_MAX_LIFETIME_MS` | `1800000` | Maximum lifetime of a pooled connection |
| `DB_POOL_KEEPALIVE_MS` | `120000` | Keepalive interval for idle connections; `0` disables keepalive |

Before increasing the pool, keep this within the database connection budget:

```text
replica count * DB_POOL_MAX <= Oracle connection limit - admin/background reserve
```

If keepalive is enabled, set it to at least `30000` and below `DB_POOL_MAX_LIFETIME_MS`.
Set max lifetime below any connection lifetime imposed by Oracle or the network proxy.

## Tests

| Kind | Naming | Phase | Needs |
|------|--------|-------|-------|
| Unit | `*Test` | `./mvnw test` (Surefire) | nothing |
| Integration | `*IT` | `./mvnw verify` (Failsafe) | a real Oracle |

Integration tests extend `AbstractOracleIT`: they run Flyway, boot with `ddl-auto=validate`, execute
the real JPQL/SQL and assert statement counts, so Oracle-only errors (such as `ORA-00932` on
`DISTINCT` over CLOB columns) and N+1 regressions fail the build. They find a database in this
order:

1. `FAP_IT_DB_URL` (+ `FAP_IT_DB_USERNAME`, `FAP_IT_DB_PASSWORD`): an existing Oracle. Use a
   development schema only: Flyway migrates it, though every test rolls back its own data.
2. Docker: a throwaway `gvenzl/oracle-xe` container (what CI uses).
3. Neither: the `*IT` classes are skipped. CI sets `FAP_IT_REQUIRE_DB=true` so they fail instead.

```powershell
$env:FAP_IT_DB_URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:FAP_IT_DB_USERNAME = "fap"
$env:FAP_IT_DB_PASSWORD = "12345"
.\mvnw.cmd verify
```

`verify` also writes JaCoCo coverage (`target/site/jacoco-merged`) and the measured statement counts
of the heavy read paths (`target/query-baseline.txt`). Locally, `HIBERNATE_STATS=true` logs the
statement count of every session to help spot N+1 queries.

## Continuous Integration

`.github/workflows/backend-ci.yml` runs `clean verify` with Java 21 for every push and pull
request. Pull requests also run GitHub dependency review and fail when a newly introduced
dependency has a known vulnerability of `high` severity or above.

GitHub Actions are pinned to immutable commit SHAs. Dependabot checks Maven dependencies and
GitHub Actions weekly so updates arrive as reviewable pull requests.

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

## Password Reset OTP Email

Forgot password uses a 6-digit OTP. In local/dev, if mail is not enabled, the OTP is written to the application log.

To send OTP by SMTP, set:

```bash
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM=your_email@gmail.com
```

Mail health check is disabled by default for local development. Enable it only after SMTP is fully configured:

```bash
MAIL_HEALTH_ENABLED=true
```

Then call:

```text
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
```

## Documentation

- Local E2E data overview: `docs/TEST_DATA_OVERVIEW.md`
- Local test accounts: `docs/TEST_ACCOUNTS.md`
- Business flow: `docs/BUSINESS_FLOW.md`
- UI test flow: `docs/UI_TEST_FLOW.md`
- API Center test flow: `docs/API_CENTER_TEST_FLOW.md`
- Mock versus real API audit: `docs/MOCK_VS_REAL_API.md`
- Frozen backend scope: `docs/07_scope_freeze.md`
- Business rules: `docs/06_business_logic_review.md`
- Spring Boot blueprint: `docs/08_backend_spring_boot_project_blueprint.md`
- Scaffold checklist: `docs/09_backend_scaffold_checklist.md`
- Dashboard APIs: `docs/api/dashboard_apis.md`
- Training feedback APIs: `docs/api/training_feedback_apis.md`
- Training feedback test flow: `docs/api/training_feedback_test_flow.md`
- Question Bank APIs: `docs/api/question_bank_apis.md`
- Quiz Draft APIs: `docs/api/quiz_draft_apis.md`
- Quiz Assignment APIs: `docs/api/quiz_assignment_apis.md`
- Quiz Attempt APIs: `docs/api/quiz_attempt_apis.md`
- Quiz Result APIs: `docs/api/quiz_result_apis.md`
- Learning Material APIs: `docs/api/material_apis.md`
- My Learning APIs: `docs/api/my_learning_apis.md`
- Syllabus Full Create API: `docs/api/syllabus_full_create_api.md`
- Generated OpenAPI endpoint inventory: `docs/api/openapi-endpoints.md`
- Swagger UI verification checklist: `docs/api/swagger_ui_verification.md`
- Optimization plan and measured baseline: `docs/optimization-plan.md`
- Database artifacts: `docs/database/README.md`

Regenerate the OpenAPI endpoint inventory after controller mapping or annotation changes:

```powershell
node .\scripts\generate-openapi-endpoints.js
```

With the local backend running, verify the complete API flow with:

```powershell
.\scripts\verify-e2e-flow.ps1 -BaseUrl "http://localhost:8080/api/v1"
```
