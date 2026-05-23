# Deploy Skill

Use for packaging and deployment checks of the Spring Boot backend.

## Commands

```bash
./mvnw clean test
./mvnw clean package
java -jar target/fap-backend-0.0.1-SNAPSHOT.jar
```

Windows:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
java -jar target\fap-backend-0.0.1-SNAPSHOT.jar
```

## Checks

- JDK 21 configured.
- Environment variables configured.
- Flyway migrations reviewed.
- `/actuator/health` returns UP after startup.
