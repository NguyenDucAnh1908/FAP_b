# Security Rules

## Critical Rules

- Never hardcode secrets, passwords, API keys, JWT secrets, or tokens.
- Never commit real `.env` files.
- Never log passwords, JWTs, refresh tokens, authorization headers, or sensitive PII.
- Validate every request DTO with Jakarta Validation.
- Protect all `/api/v1/**` endpoints unless explicitly public.

## Authentication

- Use Spring Security.
- Use short-lived JWT access tokens.
- Store refresh tokens in `refresh_tokens`.
- Rotate refresh tokens on refresh.
- Hash passwords with BCrypt.
- Rate limit authentication endpoints when rate limiting infrastructure is added.

## Authorization

- Permission checks are action-based.
- Do not compare permission levels ordinally.
- Ownership checks run after permission checks.
- Super Admin bypasses ownership checks only as defined in scope docs.
- Deny by default when role behavior is not specified.

## Spring Practices

- Use a `SecurityFilterChain` bean.
- Keep controllers free of authorization logic except method annotations.
- Prefer `@PreAuthorize` with explicit action checks, for example:

```java
@PreAuthorize("@permissionEvaluator.hasAction(authentication, 'syllabus', 'create')")
```

## Data Protection

- Do not expose JPA entities directly.
- Do not return password hashes or token values in API responses.
- Avoid broad serialization of user/session objects.
- Audit permission changes, status transitions, deletes, material operations, and post-completion attendance corrections.

## Dependency Security

Use Maven dependency checks in CI when configured. Review new dependencies for maintenance, license, and security posture before adoption.
