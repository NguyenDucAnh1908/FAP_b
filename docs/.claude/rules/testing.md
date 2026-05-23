# Testing Standards

## Tools

- Unit tests: JUnit 5 + Mockito
- API integration: Spring Boot Test + MockMvc
- Repository tests: `@DataJpaTest`
- Security tests: Spring Security Test
- Migration/database validation: Flyway + Testcontainers Oracle-compatible DB

## Commands

```bash
./mvnw test
./mvnw clean test
```

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean test
```

## Requirements

- Service/domain logic target: 80% coverage.
- Every bug fix must include a regression test.
- Every status transition must include allowed and rejected cases.
- Permission evaluator tests must cover every permission level and action mapping.
- Ownership-sensitive endpoints need forbidden and allowed integration tests.

## Test Placement

```text
src/test/java/com/fap/
  common/
  auth/
  user/
  syllabus/
  program/
  clazz/
  quiz/
  calendar/
```

## Test Types

| Type | Use For |
|---|---|
| Unit | Services, state machines, permission evaluator |
| Repository | JPA mappings and custom queries |
| API integration | Controllers, validation, response envelopes |
| Security regression | Authentication, authorization, ownership |
| Migration validation | Flyway schema boots and JPA validates |

## Good Test Rules

- Test behavior, not implementation details.
- Use descriptive method names.
- Keep tests deterministic.
- Avoid over-mocking service logic.
- Prefer builders or fixtures for repeated domain setup.
- Do not leave skipped tests without a tracked reason.
