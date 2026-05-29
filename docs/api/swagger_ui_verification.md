# Swagger UI Verification

Status: static documentation verification completed.

## URLs

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`
- OpenAPI YAML: `/v3/api-docs.yaml`

## Static Verification Result

| Check | Result |
|---|---:|
| Controllers scanned | 18 |
| Spring MVC operations scanned | 114 |
| Operations with `@Operation` | 114 |
| Operations with OpenAPI response annotations | 114 |
| Controllers with `@Tag` | 18 |
| Missing operation annotations | 0 |
| Missing response annotations | 0 |
| Missing tags | 0 |

## Swagger UI Completeness Checklist

- [x] API title, version, description configured.
- [x] JWT bearer security scheme `bearerAuth` configured.
- [x] Controller groups/tags configured.
- [x] All mapped operations have summaries.
- [x] All mapped operations have documented response codes.
- [x] `Accept-Language` header is documented globally.
- [x] Generated endpoint inventory: `docs/api/openapi-endpoints.md`.

## Runtime Verification

Runtime Swagger UI verification requires a valid Java 21 `JAVA_HOME` and a reachable configured database. In this workspace, `JAVA_HOME` currently points to an old Java 7 path, so runtime startup should be verified after fixing the local JDK path.

Recommended Windows PowerShell verification commands:

```powershell
$env:JAVA_HOME = "C:\\Path\\To\\jdk-21"
$env:Path = "$env:JAVA_HOME\\bin;$env:Path"
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

After startup:

```powershell
Invoke-WebRequest http://localhost:8080/v3/api-docs | Select-Object -ExpandProperty StatusCode
Invoke-WebRequest http://localhost:8080/swagger-ui/index.html | Select-Object -ExpandProperty StatusCode
```

Expected status: `200` for both URLs.

