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
| `BadRequestException(code, message)` | 400 | explicit code |
| `MaxUploadSizeExceededException` / `MissingServletRequestPartException` / `MultipartException` | 400 | `FILE_TOO_LARGE` / `FILE_REQUIRED` / `INVALID_MULTIPART` |
| `BadCredentialsException`, `UnauthorizedException` | 401 | `UNAUTHORIZED` |
| `ForbiddenException` | 403 | `ACCESS_DENIED` |
| `AccessDeniedException` (failed `@PreAuthorize`) | 403 | `FORBIDDEN` |
| `NotFoundException(resource, message)` | 404 | `RESOURCE_NOT_FOUND` |
| `ConflictException(code, message)` | 409 | explicit code (`BUSINESS_CONFLICT` when omitted) |
| `BusinessException(code, message)` | 409 | explicit code |
| `OptimisticLockingFailureException` / `OptimisticLockException` | 409 | `CONCURRENT_MODIFICATION` |
| `HttpRequestMethodNotSupportedException` | 405 | `METHOD_NOT_ALLOWED` |
| unexpected `Exception` | 500 | `INTERNAL_ERROR` |

## Messages And i18n

- The API `code` is the contract; the `message` is localized from `messages.properties` /
  `messages_vi.properties` (`Accept-Language`). Resolution order: the exception's message key,
  then `error.<CODE>`, then the exception's English `getMessage()`.
- Every thrown code needs an `error.<CODE>` key in both files; `ErrorMessageKeysTest` fails the
  build otherwise. Keep both files' key sets identical.
- `NotFoundException("<resource>", "<English message>")` keeps the code `RESOURCE_NOT_FOUND` and
  selects `error.RESOURCE_NOT_FOUND.<resource>` (for example `class`, `quiz_attempt`).
- Use `.withMessageKey("error.<CODE>.<variant>")` when one code is thrown with different wordings,
  and `.withMessageArgs(...)` with `{0}` placeholders for runtime values.
- Login failures stay generic (`error.UNAUTHORIZED`) so a response never reveals whether an email
  exists.

## Rules

- Business rule violations usually return `409 Conflict`.
- Validation failures return `422 Unprocessable Entity`.
- Authentication failures return `401 Unauthorized`.
- Authorization failures return `403 Forbidden`.
- Services throw domain exceptions; controllers do not build error bodies manually.
