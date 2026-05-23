# Debug Command

## Workflow

1. Capture the failing command, stack trace, request, or test.
2. Identify the layer: configuration, controller, service, repository, migration, security, or environment.
3. Reproduce with the smallest command.
4. Inspect logs and relevant code.
5. Fix the root cause.
6. Add a regression test when feasible.

## Useful Commands

```bash
./mvnw test
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

## Common Backend Failure Areas

- Missing or invalid `JAVA_HOME`.
- Oracle connection configuration.
- Flyway migration failure.
- JPA validation mismatch.
- Spring Security filter chain behavior.
- Bean creation or component scan issue.
