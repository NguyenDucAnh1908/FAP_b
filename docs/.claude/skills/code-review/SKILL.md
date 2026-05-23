# Code Review Skill

Review changes across five axes:

1. Correctness.
2. Readability.
3. Architecture.
4. Security.
5. Performance.

## Spring Backend Checks

- Controllers are thin.
- Services own transactions and business rules.
- Repositories do not contain business transitions.
- DTOs are used for API input/output.
- Entities are not exposed directly.
- Migrations are present for schema changes.
- Tests cover new behavior.
- Security and ownership rules are enforced.
