# Naming Conventions

## API

- Base path: `/api/v1`.
- Resource paths: plural kebab-case.
- Path variables and JSON fields: camelCase.
- Query parameters: camelCase.
- Status update endpoints: `PATCH /api/v1/{resources}/{id}/status`.

## Java

- Root package: `com.fap`.
- Feature packages: `auth`, `user`, `role`, `syllabus`, `program`, `clazz`, `quiz`, `calendar`, `notification`, `settings`, `storage`.
- DTOs: `CreateUserRequest`, `UserResponse`.
- Exceptions: `NotFoundException`, `ConflictException`.
- Tests: class name plus `Test`, for example `PermissionEvaluatorTest`.

## Database

- Tables: plural `snake_case`.
- Columns: `snake_case`.
- Primary keys: `id` unless join table composite keys are clearer.
- Foreign keys: `{referenced_singular}_id`.
- Indexes: `idx_{table}_{columns}`.
- Unique constraints: `uk_{table}_{columns}`.
- Foreign keys: `fk_{table}_{referenced_table}`.
- Check constraints: `ck_{table}_{rule}`.

## Environment Variables

- Use upper snake case.
- Examples: `DB_HOST`, `DB_USERNAME`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`.
