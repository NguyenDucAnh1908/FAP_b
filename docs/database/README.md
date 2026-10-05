# FAP Database Artifacts

Generated from:
- `../01_database_schema.md`
- `../06_business_logic_review.md`
- `../07_scope_freeze.md`

## Files

| File | Purpose |
|---|---|
| `oracle/schema.sql` | Canonical Oracle 19c+ DDL for FAP backend v1 |
| `oracle/indexes.sql` | Standalone index DDL extracted for DBA review |
| `oracle/constraints_validation.sql` | Oracle data dictionary queries to validate constraints and indexes after migration |
| `../../src/main/resources/db/migration/` | **Canonical schema history**: every Flyway migration (V1 ... V33). The copies that used to live under `docs/database/flyway/` were removed because they had drifted from the applied migrations. |
| `../../src/main/resources/db/seed/` | Demo accounts, sample classes and e2e fixtures. Loaded only by the `local` (and `it`) profile and excluded from the deployable jar. |
| `../../src/main/resources/db/migration/V29__create_class_enrollments_and_registration_modes.sql` | Adds the official class roster, enrollment settings, data migration, and session registration modes |
| `../../src/main/resources/db/migration/V30__create_course_results_and_completion_policy.sql` | Adds class completion policy, final course results, quiz snapshots, adjustment history, and result backfill |
| `../../src/main/resources/db/migration/V31__add_class_enrollment_approval_workflow.sql` | Adds pending approval/rejection states and enrollment review audit fields |
| `../../src/main/resources/db/migration/V32__add_performance_indexes.sql` | Adds function-based case-insensitive lookup indexes, missing foreign-key indexes, and schedule-conflict/dashboard indexes; replaces `idx_ts_class`/`idx_ts_trainer` with composites |
| `../../src/main/resources/db/migration/V33__use_pooled_sequence_allocation.sql` | Sets every sequence to `INCREMENT BY 50 CACHE 20` to match the entities' `allocationSize = 50` (ids are no longer contiguous) |
| `liquibase/db.changelog-master.xml` | Liquibase changelog that executes the canonical Oracle DDL |
| `liquibase/rollback/001_drop_fap_schema_oracle.sql` | Liquibase rollback SQL |
| `indexes_and_constraints.md` | Human-readable inventory of indexes, constraints, and non-DDL rules |

## Flyway

`src/main/resources/db/migration` is the single source of truth for the schema; this folder holds
DBA/reference material only. `oracle/schema.sql` and `oracle/indexes.sql` are consolidated
snapshots for review and may lag behind the latest migration; when they disagree, the migrations
win. `DatabaseSchemaIT` boots the application against Oracle with `ddl-auto=validate`, so the
migrations and the JPA entities are verified to match on every `mvnw verify`.

## Liquibase

Use `liquibase/db.changelog-master.xml` as the master changelog. It imports `oracle/schema.sql` with `sqlFile`, so Liquibase and Flyway share the same canonical DDL.

## Oracle Notes

- Enums are implemented as `VARCHAR2` plus `CHECK` constraints.
- Booleans are implemented as `NUMBER(1)` plus `CHECK (... IN (0, 1))`.
- JSON columns are implemented as `CLOB` plus `IS JSON` constraints.
- `TIME` fields from the logical schema are represented as `TIMESTAMP` in Oracle for simpler comparisons.
- JPA ID generation uses explicit Oracle sequences created by Flyway. Tables keep `NUMBER(19) PRIMARY KEY` columns rather than Oracle identity columns.
- Optimistic locking uses `version_no`.
- Audit timestamps are stored as `TIMESTAMP`.
