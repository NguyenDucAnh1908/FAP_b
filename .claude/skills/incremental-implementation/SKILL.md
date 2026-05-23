# Incremental Implementation Skill

Use this skill to implement Spring Boot backend features in small vertical slices.

## Slice Shape

A complete backend slice usually includes:

- request/response DTOs
- controller endpoint
- service method
- repository/entity changes when needed
- mapper when needed
- Flyway migration when schema changes
- tests

## Order

1. Confirm endpoint/entity is in `docs/07_scope_freeze.md`.
2. Confirm business rule in `docs/06_business_logic_review.md`.
3. Add or update tests.
4. Implement DTO/controller/service/repository.
5. Add migration if needed.
6. Run focused tests.
7. Run `./mvnw clean test` when feasible.

## Rules

- Keep each slice independently understandable.
- Do not batch unrelated modules.
- Keep controllers thin.
- Keep services transactional where writes occur.
- Do not expose entities.
- Do not bypass ownership checks.
