# System Design Rules

## Baseline

The FAP backend is a modular Spring Boot service backed by Oracle.

Prefer a modular monolith until there is a concrete operational reason to split services.

## Boundaries

- Feature modules own their service and repository logic.
- Shared infrastructure belongs in `common`.
- Cross-feature writes go through the owning service.
- Database schema changes go through Flyway.

## Reliability

- Make state transitions transactional.
- Use optimistic locking on mutable aggregate roots.
- Use atomic database operations for registration capacity and waitlist promotion.
- Keep external integrations behind service abstractions.

## Performance

- Paginate list endpoints.
- Avoid N+1 queries.
- Add indexes for frequent filters and foreign keys.
- Prefer projections for read-heavy list views.
- Cache only after correctness and invalidation rules are clear.

## Documentation

Use ADRs for major architecture deviations from `docs/08_backend_spring_boot_project_blueprint.md`.
