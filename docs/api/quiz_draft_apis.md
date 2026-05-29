# Quiz Draft APIs

Quiz Draft APIs let admins and trainers create draft quizzes and attach ordered questions from the question bank.

All endpoints require a valid JWT access token.

Swagger test flow:

1. Login with `POST /api/v1/auth/login`.
2. Copy `accessToken`.
3. Click `Authorize` in Swagger UI.
4. Enter `Bearer <accessToken>`.
5. Optional: set `Accept-Language` to `vi` or `en`.

## Endpoint Summary

| Method | Endpoint | Permission | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/quizzes` | `quiz:view` | List/filter quizzes. |
| `POST` | `/api/v1/quizzes` | `quiz:create` | Create a draft quiz. |
| `GET` | `/api/v1/quizzes/{id}` | `quiz:view` | View quiz detail. |
| `PUT` | `/api/v1/quizzes/{id}` | `quiz:modify` | Update a draft quiz. |
| `PATCH` | `/api/v1/quizzes/{id}/status` | `quiz:modify` | Publish or close a quiz. |
| `DELETE` | `/api/v1/quizzes/{id}` | `quiz:modify` | Soft-delete a draft quiz. |
| `GET` | `/api/v1/quizzes/{id}/questions` | `quiz:view` | List ordered quiz questions. |
| `PUT` | `/api/v1/quizzes/{id}/questions` | `quiz:modify` | Replace ordered quiz questions. |

## Rules

- New quizzes are created as `Draft`.
- Only `Draft` quizzes can be edited, deleted, or have questions replaced.
- Allowed status transitions are `Draft -> Published` and `Published -> Closed`.
- Published quizzes cannot be edited or have questions replaced.
- A quiz must contain at least one question before it can be published.
- `durationMinutes` and `maxAttempts` must be greater than `0`.
- `passingScore` must be between `0` and `100`.
- `openDate` must be before or equal to `closeDate` when both are provided.
- A quiz cannot contain the same question twice.
- `sortOrder` must be unique within a quiz.
- `points` must be greater than `0`.
- Deleted questions cannot be attached to a quiz.

## Create Draft Quiz

```http
POST /api/v1/quizzes
Authorization: Bearer <accessToken>
Content-Type: application/json
Accept-Language: vi
```

```json
{
  "title": "Java Core Basics Quiz",
  "description": "Short quiz for Java Core fundamentals.",
  "durationMinutes": 30,
  "passingScore": 70,
  "maxAttempts": 2,
  "randomize": true,
  "category": "Java Core",
  "openDate": "2026-06-01",
  "closeDate": "2026-06-30"
}
```

## Attach Questions

Create questions first using `/api/v1/questions`, then attach their IDs:

```http
PUT /api/v1/quizzes/1000/questions
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "questions": [
    {
      "questionId": 1000,
      "sortOrder": 1,
      "points": 1.00
    },
    {
      "questionId": 1001,
      "sortOrder": 2,
      "points": 2.00
    }
  ]
}
```

## Publish Quiz

```http
PATCH /api/v1/quizzes/1000/status
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "status": "Published"
}
```

## Close Quiz

```json
{
  "status": "Closed"
}
```

## List Quizzes

```http
GET /api/v1/quizzes?status=Draft&category=Java%20Core&keyword=basics&page=1&limit=20
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "success": true,
  "data": [
    {
      "id": 1000,
      "title": "Java Core Basics Quiz",
      "description": "Short quiz for Java Core fundamentals.",
      "durationMinutes": 30,
      "passingScore": 70,
      "maxAttempts": 2,
      "randomize": true,
      "category": "Java Core",
      "status": "Draft",
      "openDate": "2026-06-01",
      "closeDate": "2026-06-30",
      "questionCount": 2,
      "createdBy": 1000,
      "updatedBy": 1000,
      "createdAt": "2026-05-28T16:00:00",
      "updatedAt": "2026-05-28T16:00:00"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

Common errors:

| HTTP | Code | Meaning |
|---|---|---|
| `400` | `INVALID_QUIZ_DATE_RANGE` | `openDate` is after `closeDate`. |
| `400` | `DUPLICATE_QUIZ_QUESTION` | The same question appears more than once. |
| `400` | `DUPLICATE_QUIZ_QUESTION_SORT_ORDER` | More than one question uses the same `sortOrder`. |
| `404` | `NOT_FOUND` | Quiz or one of the questions does not exist. |
| `409` | `QUIZ_NOT_EDITABLE` | Only draft quizzes can be edited. |
| `409` | `QUIZ_QUESTION_REQUIRED` | Quiz has no questions and cannot be published. |
| `409` | `INVALID_QUIZ_STATUS_TRANSITION` | Requested status transition is not allowed. |
