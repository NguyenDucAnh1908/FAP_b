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
| `flyway/V2__create_sequences_and_seed_initial_roles_and_admin.sql` | Oracle sequences for JPA ID generation plus initial roles, permission matrix, and local Super Admin seed |
| `flyway/V3__fix_initial_admin_password_hash.sql` | Corrects the initial local Super Admin BCrypt password hash |
| `flyway/V4__create_password_reset_tokens.sql` | Adds password reset OTP storage for forgot-password flow |
| `flyway/V5__create_audit_logs_sequence.sql` | Adds Oracle sequence for audit log JPA ID generation |
| `flyway/V6__create_syllabus_sequences.sql` | Adds Oracle sequence for syllabus JPA ID generation |
| `flyway/V7__create_syllabus_outline_sequences.sql` | Adds Oracle sequences for syllabus day, unit, and topic JPA ID generation |
| `flyway/V8__create_material_files_sequence.sql` | Adds Oracle sequence for material file JPA ID generation |
| `flyway/V9__create_training_programs_sequence.sql` | Adds Oracle sequence for training program JPA ID generation |
| `flyway/V10__create_classes_sequence.sql` | Adds Oracle sequence for class JPA ID generation |
| `flyway/V11__create_class_trainers_sequence.sql` | Adds Oracle sequence for class trainer assignment JPA ID generation |
| `flyway/V12__create_training_sessions_sequence.sql` | Adds Oracle sequence for training session JPA ID generation |
| `flyway/V13__create_training_registrations_sequence.sql` | Adds Oracle sequence for training registration JPA ID generation |
| `flyway/V14__create_attendance_records_sequence.sql` | Adds Oracle sequence for attendance record JPA ID generation |
| `flyway/V15__create_notifications_sequence.sql` | Adds Oracle sequence for notification JPA ID generation |
| `flyway/V16__create_training_feedbacks.sql` | Adds training feedback table, sequence, constraints, and indexes |
| `liquibase/db.changelog-master.xml` | Liquibase changelog that executes the canonical Oracle DDL |
| `liquibase/rollback/001_drop_fap_schema_oracle.sql` | Liquibase rollback SQL |
| `indexes_and_constraints.md` | Human-readable inventory of indexes, constraints, and non-DDL rules |

## Flyway

Place Flyway migrations under your backend migration folder, for example:

```text
src/main/resources/db/migration/V1__create_fap_schema.sql
src/main/resources/db/migration/V2__create_sequences_and_seed_initial_roles_and_admin.sql
src/main/resources/db/migration/V3__fix_initial_admin_password_hash.sql
src/main/resources/db/migration/V4__create_password_reset_tokens.sql
```

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
