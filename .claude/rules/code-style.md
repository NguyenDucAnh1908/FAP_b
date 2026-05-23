# Code Style

## Java

- Package names: lowercase, under `com.fap`.
- Classes: `PascalCase`.
- Methods and fields: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- DTO request classes end with `Request`.
- DTO response classes end with `Response`.
- JPA entities are singular nouns, for example `User`.
- Repositories end with `Repository`.
- Services end with `Service`.
- Controllers end with `Controller`.
- Mappers end with `Mapper`.

## Formatting

- Use the Maven/Spring Boot defaults unless the project adds a formatter.
- Keep imports organized.
- Avoid wildcard imports.
- Prefer constructor injection.
- Avoid field injection.

## Spring

- Use `@RestController` for API controllers.
- Use `@RequestMapping("/api/v1/...")` at controller class level.
- Use `@Valid` on request DTOs.
- Use `@Transactional` on write service methods.
- Use `@Transactional(readOnly = true)` for read service methods when useful.
