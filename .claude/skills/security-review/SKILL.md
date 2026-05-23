# Security Review Skill

Review Spring Boot backend security.

## Checklist

- Public endpoint list is intentional.
- `/api/v1/**` is protected by default.
- JWT and refresh token behavior is explicit.
- Passwords use BCrypt.
- Permission checks are action-based.
- Ownership checks exist where required.
- Request DTOs are validated.
- Sensitive values are not logged.
- Actuator endpoints are limited.
- CORS is environment-driven.
