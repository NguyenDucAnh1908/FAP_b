# Error Handling

## Standard Response Envelope

Success:

```json
{
  "success": true,
  "data": {},
  "message": "Optional"
}
```

Error:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": []
  }
}
```

## Spring Implementation

- Use `@RestControllerAdvice` for centralized handling.
- Use domain exceptions from `com.fap.common.exception`.
- Do not expose stack traces to clients.
- Log unexpected errors with request ID when request correlation is implemented.

## Status Mapping

| Exception | Status | Code |
|---|---:|---|
| `MethodArgumentNotValidException` | 422 | `VALIDATION_ERROR` |
| `ConstraintViolationException` | 422 | `VALIDATION_ERROR` |
| `NotFoundException` | 404 | `RESOURCE_NOT_FOUND` |
| `ForbiddenException` | 403 | `ACCESS_DENIED` |
| `ConflictException` | 409 | `BUSINESS_CONFLICT` |
| `BadCredentialsException` | 401 | `UNAUTHORIZED` |
| unexpected `Exception` | 500 | `INTERNAL_ERROR` |

## Rules

- Business rule violations usually return `409 Conflict`.
- Validation failures return `422 Unprocessable Entity`.
- Authentication failures return `401 Unauthorized`.
- Authorization failures return `403 Forbidden`.
- Services throw domain exceptions; controllers do not build error bodies manually.
