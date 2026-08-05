# FAP OpenAPI Endpoint Documentation

Generated from Spring MVC controller mappings and OpenAPI annotations.

## Swagger / OpenAPI URLs

| Resource | URL |
|---|---|
| Swagger UI | `/swagger-ui/index.html` |
| OpenAPI JSON | `/v3/api-docs` |
| OpenAPI YAML | `/v3/api-docs.yaml` |

## Authentication

Most `/api/v1/**` endpoints require JWT Bearer authentication. Use Swagger UI **Authorize** with:

```text
Bearer <access-token>
```

## Pagination and sorting

All paginated GET list endpoints accept these common parameters:

| Parameter | Meaning | Default |
|---|---|---|
| `page` | Page number, starting from 1 | `1` |
| `limit` | Number of records per page | `20` |
| `sortBy` | Entity field used for sorting | Endpoint default |
| `order` | `asc` or `desc` | `asc` when `sortBy` is provided |

Example:

```text
GET /api/v1/syllabuses?page=1&limit=20&sortBy=name&order=asc
```

If `sortBy` is omitted, the endpoint keeps its existing default order. Unsupported
fields or values other than `asc` and `desc` return `400 INVALID_SORT_FIELD` or
`400 INVALID_SORT_ORDER`.

## Endpoint Inventory (115 operations)

### Audit Logs

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/audit-logs` | List audit logs | 200, 400, 401, 403, 404, 409 |

### Authentication

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| PATCH | `/api/v1/auth/change-password` | Change current user password | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/auth/forgot-password` | Request password reset OTP | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/auth/google` | Authenticate with Google OAuth token | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/auth/login` | Authenticate with email and password | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/auth/logout` | Logout and revoke refresh token | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/auth/refresh` | Refresh access token | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/auth/reset-password` | Reset password using OTP | 200, 400, 401, 403, 404, 409 |

### Classes

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/classes` | List classes | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/classes` | Create classes | 201, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/classes/{id}` | Delete classes | 204, 400, 401, 403, 404, 409 |
| GET | `/api/v1/classes/{id}` | Get classes detail | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/classes/{id}` | Update classes | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/classes/{id}/admins` | List admins | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/classes/{id}/admins` | Replace admins | 200, 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/classes/{id}/status` | Update status | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/classes/{id}/trainers` | List trainers | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/classes/{id}/trainers` | Replace trainers | 200, 400, 401, 403, 404, 409 |

### Materials

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/materials` | List materials | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/materials` | Create materials | 201, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/materials/{id}` | Delete materials | 204, 400, 401, 403, 404, 409 |
| GET | `/api/v1/materials/{id}` | Get materials detail | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/materials/{id}` | Update materials | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/me/materials` | List current user assigned materials | 200, 400, 401, 403, 404, 409 |

### My Learning

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/me/classes` | Classes | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/me/classes/{classId}` | Class Detail | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/me/classes/{classId}/learning-content` | Learning Content | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/me/classes/{classId}/progress` | Progress | 200, 400, 401, 403, 404, 409 |

### My Training

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/me/attendance` | Attendance | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/me/class-admin-dashboard` | Get current class admin dashboard | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/me/trainer-dashboard` | Get current trainer teaching dashboard | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/me/training-dashboard` | Get current trainee learning dashboard | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/me/training-registrations` | Registrations | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/me/training-sessions` | Sessions | 200, 400, 401, 403, 404, 409 |

### Notifications

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/notifications` | List notifications | 200, 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/notifications/{id}/read` | Mark notification as read | 200, 400, 401, 403, 404, 409 |

### Questions

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/questions` | List questions | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/questions` | Create questions | 201, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/questions/{id}` | Delete questions | 204, 400, 401, 403, 404, 409 |
| GET | `/api/v1/questions/{id}` | Get questions detail | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/questions/{id}` | Update questions | 200, 400, 401, 403, 404, 409 |

### Quiz Attempts

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/quiz-attempts/{attemptId}` | Get quiz attempts detail | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/quiz-attempts/{attemptId}/answers` | Save quiz attempt answers | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/quiz-attempts/{attemptId}/review` | Review submitted quiz attempt | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/quiz-attempts/{attemptId}/submit` | Submit quiz attempt | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/quizzes/{quizId}/attempts` | Start quiz attempt | 201, 400, 401, 403, 404, 409 |
| GET | `/api/v1/quizzes/assigned` | List assigned quizzes for current trainee | 200, 400, 401, 403, 404, 409 |

### Quiz Results

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/quizzes/{quizId}/attempt-summary` | Get summary | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/quizzes/{quizId}/attempts` | List attempts | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/quizzes/{quizId}/attempts/{attemptId}` | Get attempt detail detail | 200, 400, 401, 403, 404, 409 |

### Quizzes

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/quizzes` | List quizzes | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/quizzes` | Create quizzes | 201, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/quizzes/{id}` | Delete quizzes | 204, 400, 401, 403, 404, 409 |
| GET | `/api/v1/quizzes/{id}` | Get quizzes detail | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/quizzes/{id}` | Update quizzes | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/quizzes/{id}/assignments` | List assignments | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/quizzes/{id}/assignments` | Assign | 201, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/quizzes/{id}/assignments/{assignmentId}` | Delete assignment | 204, 400, 401, 403, 404, 409 |
| GET | `/api/v1/quizzes/{id}/questions` | List questions | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/quizzes/{id}/questions` | Replace questions | 200, 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/quizzes/{id}/status` | Update status | 200, 400, 401, 403, 404, 409 |

### Roles and Permissions

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/roles` | List roles | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/roles/permissions` | Get role permission matrix | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/roles/permissions` | Update role permission matrix | 200, 400, 401, 403, 404, 409 |

### Settings

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/settings` | Get settings detail | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/settings` | Update settings | 200, 400, 401, 403, 404, 409 |

### Syllabus

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/syllabuses` | List syllabus | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/syllabuses` | Create syllabus | 201, 400, 401, 403, 404, 409 |
| POST | `/api/v1/syllabuses/quick-create` | Quick create syllabus draft | 201, 400, 401, 403, 404, 409 |
| POST | `/api/v1/syllabuses/full` | Create full syllabus with outline | 201, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/syllabuses/{id}/full` | Update full syllabus with outline | 200, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/syllabuses/{id}` | Delete syllabus | 204, 400, 401, 403, 404, 409 |
| GET | `/api/v1/syllabuses/{id}` | Get syllabus detail | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/syllabuses/{id}/full` | Get full syllabus detail | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/syllabuses/{id}` | Update syllabus | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/syllabuses/{id}/days` | Create day | 201, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/syllabuses/{id}/days/{dayId}` | Delete day | 204, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/syllabuses/{id}/days/{dayId}` | Update day | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/syllabuses/{id}/days/{dayId}/units` | Create unit | 201, 400, 401, 403, 404, 409 |
| GET | `/api/v1/syllabuses/{id}/outline` | Get outline detail | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/syllabuses/{id}/output-standards` | Get output standards detail | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/syllabuses/{id}/output-standards` | Replace output standards | 200, 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/syllabuses/{id}/status` | Update status | 200, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/syllabuses/{id}/topics/{topicId}` | Delete topic | 204, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/syllabuses/{id}/topics/{topicId}` | Update topic | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/syllabuses/{id}/topics/{topicId}/materials` | List materials | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/syllabuses/{id}/topics/{topicId}/materials` | Create material | 201, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/syllabuses/{id}/topics/{topicId}/materials/{materialId}` | Delete material | 204, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/syllabuses/{id}/units/{unitId}` | Delete unit | 204, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/syllabuses/{id}/units/{unitId}` | Update unit | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/syllabuses/{id}/units/{unitId}/topics` | Create topic | 201, 400, 401, 403, 404, 409 |
| POST | `/api/v1/syllabuses/import` | Import syllabuses from CSV file | 200, 400, 401, 403, 404, 409 |

### Training Feedback

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/me/feedback` | List current user training feedback | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/training-sessions/{id}/feedback` | Submit quiz attempt | 201, 400, 401, 403, 404, 409 |
| GET | `/api/v1/training-sessions/{id}/feedback-summary` | Get summary | 200, 400, 401, 403, 404, 409 |

### Training Programs

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/training-programs` | List training programs | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/training-programs` | Create training programs | 201, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/training-programs/{id}` | Delete training programs | 204, 400, 401, 403, 404, 409 |
| GET | `/api/v1/training-programs/{id}` | Get training programs detail | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/training-programs/{id}` | Update training programs | 200, 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/training-programs/{id}/status` | Update status | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/training-programs/{id}/syllabuses` | List syllabuses | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/training-programs/{id}/syllabuses` | Replace syllabuses | 200, 400, 401, 403, 404, 409 |

### Training Sessions

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/training-sessions` | List training sessions | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/training-sessions` | Create training sessions | 201, 400, 401, 403, 404, 409 |
| GET | `/api/v1/training-sessions/{id}` | Get training sessions detail | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/training-sessions/{id}` | Update training sessions | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/training-sessions/{id}/attendance` | Attendance | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/training-sessions/{id}/attendance` | Upsert Attendance | 200, 400, 401, 403, 404, 409 |
| GET | `/api/v1/training-sessions/{id}/participants` | Participants | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/training-sessions/{id}/registrations` | Register | 200, 400, 401, 403, 404, 409 |
| DELETE | `/api/v1/training-sessions/{id}/registrations/me` | Cancel My Registration | 200, 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/training-sessions/{id}/status` | Update status | 200, 400, 401, 403, 404, 409 |

### Users

| Method | Path | Summary | Documented responses |
|---|---|---|---|
| GET | `/api/v1/users` | List users | 200, 400, 401, 403, 404, 409 |
| POST | `/api/v1/users` | Create users | 201, 400, 401, 403, 404, 409 |
| GET | `/api/v1/users/{id}` | Get users detail | 200, 400, 401, 403, 404, 409 |
| PUT | `/api/v1/users/{id}` | Update users | 200, 400, 401, 403, 404, 409 |
| PATCH | `/api/v1/users/{id}/status` | Update status | 200, 400, 401, 403, 404, 409 |

## Verification Checklist

- Every controller has `@Tag`.
- Every mapped endpoint has `@Operation`.
- Every mapped endpoint has documented OpenAPI response codes.
- Bearer JWT security scheme is configured as `bearerAuth`.
- `Accept-Language` header is globally documented.
