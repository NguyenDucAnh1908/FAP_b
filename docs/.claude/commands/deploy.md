# Deploy Command

## Pre-Deploy Checklist

- [ ] JDK 21 is configured.
- [ ] `./mvnw clean test` passes.
- [ ] Required environment variables are configured.
- [ ] Flyway migrations are reviewed.
- [ ] No real `.env` file or secret is committed.
- [ ] Health endpoint is enabled.

## Build

```bash
./mvnw clean package
```

Windows:

```powershell
.\mvnw.cmd clean package
```

## Run

```bash
java -jar target/fap-backend-0.0.1-SNAPSHOT.jar
```

## Post-Deploy Verification

- Check `/actuator/health`.
- Check application logs for startup and Flyway errors.
- Check `/swagger-ui/index.html` if enabled in the environment.
- Smoke test protected `/api/v1/**` behavior.

## Rollback

Rollback must be environment-specific. Never rollback by manually editing database schema. Use approved database rollback procedures and artifact rollback.
