---
name: test
description: Verify Spring Boot code with JUnit, MockMvc, and Maven
---

# /test

## Standard Verification

```bash
./mvnw clean test
```

Windows:

```powershell
.\mvnw.cmd clean test
```

## Test Focus

- Services: JUnit 5 + Mockito.
- Controllers: MockMvc.
- Security: Spring Security Test.
- Repositories: `@DataJpaTest`.
- Database migrations: Flyway + Testcontainers when available.

## Bug Fix Pattern

1. Add a test that reproduces the bug.
2. Verify it fails.
3. Fix the root cause.
4. Verify the test passes.
5. Run the relevant wider suite.

## Completion Checklist

- [ ] New behavior is tested.
- [ ] Bug fixes include regression tests.
- [ ] Security/ownership behavior is covered where relevant.
- [ ] No skipped tests without a tracked reason.
- [ ] Maven test command passes or the blocker is documented.
