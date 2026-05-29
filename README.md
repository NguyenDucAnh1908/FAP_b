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
- Database artifacts: `docs/database/README.md`
