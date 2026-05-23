# Plan Command

## Purpose

Break backend work into small vertical slices.

## Each Slice Should Include

- Scope doc/API reference.
- Controller impact.
- DTO impact.
- Service/business rule impact.
- Repository/entity impact.
- Migration impact.
- Security/ownership impact.
- Test plan.

## Output Template

```markdown
## Slice: [name]

Acceptance criteria:
- ...

Files likely changed:
- `src/main/java/com/fap/...`
- `src/test/java/com/fap/...`

Verification:
- `./mvnw test`
```
