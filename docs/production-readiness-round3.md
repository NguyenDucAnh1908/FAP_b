# Production Readiness — Round 3

Branch: `feature/production-readiness-round3`
Reconstructed on 2026-08-09 from the branch commit scope (`main..HEAD`, merge-base `11590ed`)
and the project's own required-signals checklist in `.claude/rules/monitoring.md`.

> The original round-3 task plan was never committed; it lived only in a prior session's
> context and was lost. This document re-establishes it so the plan survives future sessions.
> If the exact original priority ordering resurfaces, reconcile it here rather than forking.

## Objective

Take the FAP backend from "feature-complete" to "safe to run in a real environment":
security hardening, database migration safety, observability, and deployment ergonomics —
without adding scope outside `docs/07_scope_freeze.md`.

## Status Legend

- ✅ done and verified
- 🔜 next up
- ⬜ planned, not started

## Completed (verified in git history)

| ID | Commit | Item | Notes |
|----|--------|------|-------|
| C1 | `301b237` | Context boot verify, quiz-attempt pessimistic lock, material file storage | |
| C2 | `962855a` | Require `JWT_SECRET`; removed hardcoded fallback | app fails fast if unset |
| C3 | `83da00b` | Sample seed data moved to `db/seed` (local profile only) | prod scans `db/migration` alone |
| C4 | `f90cde5` | Rate-limit auth endpoints (bucket4j, bounded key map) | login/google/reset/forgot/refresh |
| C5 | `157f165`+`eb0ec47` | Request correlation id + structured JSON logging | per-profile logback, secret masking |
| P3 | `b1b70ba` | Metrics: Micrometer + Prometheus | see P3 detail below |

Test baseline at end of Round 3: **244 tests, 0 failures / 0 errors / 0 skipped**.

## Delivery Status

### P3 — Metrics (Micrometer + Prometheus)  ✅

Delivered in commit `b1b70ba` (branch `feature/production-readiness-round3`):

| File | Change |
|------|--------|
| `pom.xml` | Added `micrometer-registry-prometheus` (Boot-managed version) |
| `application.yaml` | Exposed `health,info,prometheus`; added `management.metrics.tags.application/env` global tags |
| `SecurityConfig.java` | `/actuator/health/**` public; all other `/actuator/**` require JWT auth and return 401 when unauthenticated |
| `common/metrics/DomainMetrics.java` | New `@Component` — wraps `MeterRegistry`, exposes typed methods for all required domain signals |
| `AuthService.java` | `domainMetrics.recordLoginFailure()` on bad-credential catch |
| `QuizAttemptService.java` | `domainMetrics.recordQuizSubmit(Supplier)` wraps full grading path |
| `TrainingRegistrationService.java` | `recordRegistrationOutcome(REGISTERED/WAITLISTED/CONFLICT/PROMOTED)` at every assignment/conflict site |
| `MaterialFileService.java` | `recordUpload(true/false)` with try/finally counted-flag pattern |
| `UserAvatarService.java` | Same try/finally upload pattern |
| `DomainMetricsTest.java` | 7 focused unit tests via `SimpleMeterRegistry` |
| `ActuatorMetricsIntegrationTest.java` | 3 integration tests for public health, protected Prometheus, exported domain metrics, and global tags |

Signals covered:
- ✅ HTTP request rate/error/duration — `http.server.requests` from Boot auto-config
- ✅ DB pool usage — HikariCP Micrometer metrics from Boot auto-config
- ✅ Flyway migration status — Flyway startup logs report validation, current version, and migration result; Spring Boot does not auto-configure a Flyway Prometheus meter
- ✅ Auth failure count — `fap.auth.login.failures`
- ✅ Quiz submit latency — `fap.quiz.attempt.submit` (Timer)
- ✅ Registration outcomes — `fap.training.registration{outcome=registered|waitlisted|conflict|promoted}`
- ✅ File upload success/failure — `file.upload{result=success|failure}`

### P4 — Deployment ergonomics  ✅

Delivered in working tree (branch `feature/production-readiness-round3`):

| File | Change |
|------|--------|
| `application.yaml` | Enabled graceful shutdown with a 30-second phase timeout; added bounded, environment-tunable Hikari pool settings |
| `.env.example` | Documented shutdown and Hikari environment variables with production defaults |
| `README.md` | Documented liveness/readiness URLs, shutdown behavior, pool sizing rule, and timeout guidance |
| `DeploymentReadinessIntegrationTest.java` | 4 integration tests for graceful shutdown, Hikari binding, and public liveness/readiness probes |

Verified configuration and behavior:
- ✅ Spring Boot binds graceful shutdown with `SERVER_SHUTDOWN_TIMEOUT` as the per-phase limit; an external `SIGTERM` lifecycle test is still deployment-level validation.
- ✅ `GET /actuator/health/liveness` and `GET /actuator/health/readiness` are public and return `UP` in a healthy context.
- ✅ Hikari keeps 5 idle connections and caps each instance at 20 by default; all reviewed timeout values are externally configurable.

### P5 — HTTP security headers  ✅

Delivered in working tree:

| File | Change |
|------|--------|
| `SecurityConfig.java` | Explicitly configured HSTS for HTTPS, frame denial, content-type protection, and strict-origin referrer policy |
| `SecurityHeadersIntegrationTest.java` | 3 integration tests for HTTP/HTTPS headers and Swagger UI compatibility |

Verified behavior:
- ✅ HTTPS responses include one-year, host-only HSTS; plain HTTP does not emit HSTS. Subdomains/preload stay disabled until the production domain topology is verified.
- ✅ Responses include `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, and `Referrer-Policy: strict-origin-when-cross-origin`.
- ✅ Swagger's public redirect and final HTML resource remain accessible with the explicit headers.

### P6 — Supply-chain / CI  ✅

Delivered in working tree:

| File | Change |
|------|--------|
| `.github/workflows/backend-ci.yml` | Java 21 `clean verify` gate on pushes/PRs plus high-severity dependency review on PRs |
| `.github/dependabot.yml` | Weekly Maven and GitHub Actions update PRs |
| `README.md` | Documented the CI and dependency update policy |

Security posture:
- ✅ Workflow permissions are read-only and checkout credentials are not persisted.
- ✅ Third-party actions are pinned to immutable commit SHAs with release versions documented inline.
- ✅ Both workflow files parse as valid YAML; the same `clean verify` command is verified locally.
- ⚠️ The dependency-review API job can only be runtime-verified after pushing the workflow and opening a GitHub pull request.

## Working Order

P3 through P6 are implemented. Before merge, confirm the first GitHub Actions run and perform
the deployment-level `SIGTERM` check noted under P4.

## Verification Rule

After each item: `./mvnw clean test` (Windows: `.\mvnw.cmd clean test`) must pass with no new
failures, and the item's own focused tests must be added. Never report an item done on an
unverified build.
