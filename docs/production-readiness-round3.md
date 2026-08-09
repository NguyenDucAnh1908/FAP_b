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
| P3 | (working tree) | Metrics: Micrometer + Prometheus | see P3 detail below |

Test baseline at end of P3: **237 tests, 0 failures / 0 errors / 0 skipped**.

## Remaining Backlog (prioritized)

### P3 — Metrics (Micrometer + Prometheus)  ✅

Delivered in working tree (branch `feature/production-readiness-round3`):

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

### P4 — Deployment ergonomics  🔜
- `server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase`.
- Confirm liveness/readiness health groups (probes flag is already on) and document probe URLs.
- HikariCP pool sizing / timeout review for prod.

### P5 — HTTP security headers  ⬜
`SecurityConfig` currently relies on Spring Security defaults with no explicit `.headers(...)`.
Add an explicit, reviewed posture (HSTS for TLS deployments, frame options, content-type
options, referrer policy). Keep it compatible with Swagger UI.

### P6 — Supply-chain / CI  ⬜
No `.github` workflows exist. Add dependency vulnerability scanning + build/test gate
(`monitoring.md` / `security.md` both call for dependency checks in CI when configured).

## Working Order

Do P3 first (biggest requirement gap, self-contained). P4–P6 are independent and can follow
in any order; re-confirm priority with the user before starting each.

## Verification Rule

After each item: `./mvnw clean test` (Windows: `.\mvnw.cmd clean test`) must pass with no new
failures, and the item's own focused tests must be added. Never report an item done on an
unverified build.
