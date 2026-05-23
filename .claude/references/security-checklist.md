# Security Checklist

- [ ] No hardcoded secrets.
- [ ] No real `.env` committed.
- [ ] Passwords are BCrypt hashed.
- [ ] JWT access tokens have short TTL.
- [ ] Refresh tokens are stored and rotated.
- [ ] `/api/v1/**` is protected by default.
- [ ] Public endpoints are explicitly listed.
- [ ] Request DTOs use Jakarta Validation.
- [ ] JPA entities are not exposed as API responses.
- [ ] Permission checks are action-based.
- [ ] Ownership checks are implemented where required.
- [ ] Sensitive values are not logged.
- [ ] CORS origins are environment-driven.
- [ ] Actuator exposure is limited.
- [ ] New dependencies are reviewed before adoption.
