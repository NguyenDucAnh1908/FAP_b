# Clean Code Rules

## Java And Spring

- Prefer clear names over abbreviations.
- Keep methods small and focused.
- Keep controllers thin.
- Put business rules in services.
- Put persistence concerns in repositories.
- Avoid static utility sprawl; use Spring beans when behavior has dependencies.
- Use records for simple immutable request/response DTOs where practical.
- Use enums for closed domain states.
- Use domain-specific exceptions instead of generic `RuntimeException`.

## Comments

- Comment why a decision exists, not what the code literally does.
- Do not leave commented-out code.
- Use Javadoc sparingly for public contracts that are not self-evident.

## Design

- Add abstractions only when they remove real duplication or isolate a real boundary.
- Do not introduce generic base services before at least two concrete modules prove the pattern.
- Keep package dependencies one-way: controller -> service -> repository.
