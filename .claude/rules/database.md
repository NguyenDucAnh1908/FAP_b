# Database Rules

## Canonical Database

- Database: Oracle 19c+
- Runtime migrations: Flyway
- ORM: Spring Data JPA + Hibernate
- Canonical migration path: `src/main/resources/db/migration`
- Sample/demo seed path: `src/main/resources/db/seed` (local profile only)
- Generated DB references: `docs/database`

## Migration Rules

- Every schema change must be a new Flyway migration.
- Do not edit an already-applied migration.
- Do not manually change the database outside migrations.
- Do not use `spring.jpa.hibernate.ddl-auto=update`.
- Runtime JPA DDL mode must be `validate`.
- `db/migration` holds schema and production-required reference data only.
- Sample data, demo accounts, and fake settings go in `db/seed`, which only the
  `local` profile adds to `spring.flyway.locations`. Non-local profiles scan
  `classpath:db/migration` alone, so seed data can never reach a real environment.
- `baseline-on-migrate` is enabled only for local convenience. Non-local profiles
  set it to `false` so an existing schema without history fails loudly instead of
  silently skipping migrations.

## JPA Rules

- Use JPA entities only for persistence.
- Do not expose entities in controller responses.
- Use DTOs and MapStruct for API mapping.
- Repositories should be interfaces extending Spring Data repositories unless custom queries are needed.
- Put business rules in services, not repositories.
- Use `@Transactional` at service boundaries for write workflows.

## Oracle Rules

- Tables and columns use `snake_case`.
- Booleans are represented as `NUMBER(1)` with check constraints.
- Enums are represented as `VARCHAR2` with check constraints.
- JSON data uses `CLOB` with `IS JSON` constraints where needed.
- Mutable aggregate roots require `version_no` optimistic locking.

## Query Rules

- Prefer derived queries, specifications, or JPQL before native SQL.
- Native SQL is allowed for performance-critical or Oracle-specific queries after review.
- Avoid N+1 queries; use fetch joins, entity graphs, projections, or explicit query DTOs.
- Paginate list endpoints.
- Add indexes for foreign keys and frequent filters.

## Transaction Rules

- Multi-step state transitions must be transactional.
- Registration capacity and waitlist promotion must be atomic.
- Attendance updates must be upsert-style and preserve audit history.
- Permission changes, status transitions, deletes, material operations, and post-completion attendance corrections must be audited.
