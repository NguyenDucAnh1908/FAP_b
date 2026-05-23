# Review Command

## Description

Review backend code changes for correctness, architecture, security, performance, and tests.

## Checklist

### Correctness

- [ ] Matches frozen scope.
- [ ] Matches business rules.
- [ ] Handles edge cases and error paths.

### Architecture

- [ ] Controller -> service -> repository dependency flow is respected.
- [ ] No business logic in controllers.
- [ ] No persistence logic in controllers.
- [ ] No JPA entities exposed in API responses.

### Security

- [ ] Protected endpoints require authentication.
- [ ] Permission and ownership checks are present.
- [ ] No secrets or sensitive values are logged.

### Database

- [ ] Flyway migration exists for schema changes.
- [ ] No `ddl-auto=update`.
- [ ] Queries avoid obvious N+1 behavior.
- [ ] Transactions are used for write workflows.

### Testing

- [ ] Unit tests cover service logic.
- [ ] Integration tests cover API/security behavior where relevant.
- [ ] Bug fixes include regression tests.

## Output

Lead with findings ordered by severity and include file/line references.
