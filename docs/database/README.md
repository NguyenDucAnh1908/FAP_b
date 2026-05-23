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
| `flyway/V1__create_fap_schema.sql` | Flyway migration using the canonical Oracle DDL |
| `flyway/V2__seed_initial_roles_and_admin.sql` | Initial roles, permission matrix, and local Super Admin seed |
| `liquibase/db.changelog-master.xml` | Liquibase changelog that executes the canonical Oracle DDL |
| `liquibase/rollback/001_drop_fap_schema_oracle.sql` | Liquibase rollback SQL |
| `indexes_and_constraints.md` | Human-readable inventory of indexes, constraints, and non-DDL rules |

## Flyway

Place Flyway migrations under your backend migration folder, for example:

```text
src/main/resources/db/migration/V1__create_fap_schema.sql
src/main/resources/db/migration/V2__seed_initial_roles_and_admin.sql
```

## Liquibase

Use `liquibase/db.changelog-master.xml` as the master changelog. It imports `oracle/schema.sql` with `sqlFile`, so Liquibase and Flyway share the same canonical DDL.

## Oracle Notes

- Enums are implemented as `VARCHAR2` plus `CHECK` constraints.
- Booleans are implemented as `NUMBER(1)` plus `CHECK (... IN (0, 1))`.
- JSON columns are implemented as `CLOB` plus `IS JSON` constraints.
- `TIME` fields from the logical schema are represented as `TIMESTAMP` in Oracle for simpler comparisons.
- Optimistic locking uses `version_no`.
- Audit timestamps are stored as `TIMESTAMP`.
