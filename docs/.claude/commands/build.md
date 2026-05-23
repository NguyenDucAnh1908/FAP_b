---
name: build
description: Implement Spring Boot tasks incrementally with focused tests
---

# /build

## Workflow

1. Read the task and acceptance criteria.
2. Check `docs/07_scope_freeze.md` and `docs/06_business_logic_review.md`.
3. Identify the controller, service, repository, DTO, mapper, entity, and migration impact.
4. Write or update the smallest useful test first.
5. Implement the smallest vertical slice.
6. Run relevant tests.
7. Run full verification when feasible.

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

## Rules

- Keep the project buildable after each increment.
- Do not implement endpoints outside the frozen scope.
- Do not create schema changes without Flyway migration files.
- Do not use `ddl-auto=update`.
- Do not expose JPA entities through controllers.
- Do not mix unrelated refactors into feature work.
