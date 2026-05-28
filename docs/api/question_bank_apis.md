# Question Bank APIs

Question Bank is the first step of the quiz module. It stores reusable single-choice and multiple-choice questions.

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
| `GET` | `/api/v1/questions` | `class:view` | List/filter question bank. |
| `POST` | `/api/v1/questions` | `class:modify` | Create a question. |
| `GET` | `/api/v1/questions/{id}` | `class:view` | View question detail. |
| `PUT` | `/api/v1/questions/{id}` | `class:modify` | Update a question. |
| `DELETE` | `/api/v1/questions/{id}` | `class:modify` | Soft-delete a question. |

## Rules

- `questionType` accepts `single` or `multiple`.
- `difficulty` accepts `Easy`, `Medium`, or `Hard`.
- `optionsJson` must be a non-empty JSON array.
- `correctAnswersJson` must be a non-empty JSON array.
- `single` questions must have exactly one correct answer.
- Delete is soft-delete via `is_deleted`.
- Super Admin can access all questions through the permission bypass.

## Create Single-Choice Question

```http
POST /api/v1/questions
Authorization: Bearer <accessToken>
Content-Type: application/json
Accept-Language: vi
```

```json
{
  "content": "Which Java keyword is used to inherit a class?",
  "questionType": "single",
  "category": "Java Core",
  "difficulty": "Easy",
  "optionsJson": [
    {
      "key": "A",
      "text": "implements"
    },
    {
      "key": "B",
      "text": "extends"
    },
    {
      "key": "C",
      "text": "imports"
    },
    {
      "key": "D",
      "text": "instanceof"
    }
  ],
  "correctAnswersJson": [
    "B"
  ],
  "explanation": "`extends` is used when a class inherits from another class."
}
```

## Create Multiple-Choice Question

```json
{
  "content": "Which statements about REST are correct?",
  "questionType": "multiple",
  "category": "Backend",
  "difficulty": "Medium",
  "optionsJson": [
    {
      "key": "A",
      "text": "REST APIs usually use HTTP methods."
    },
    {
      "key": "B",
      "text": "REST requires server-side session state."
    },
    {
      "key": "C",
      "text": "Resources are commonly identified by URLs."
    },
    {
      "key": "D",
      "text": "JSON is the only valid REST response format."
    }
  ],
  "correctAnswersJson": [
    "A",
    "C"
  ],
  "explanation": "REST commonly uses HTTP methods and resource URLs. JSON is common but not required."
}
```

## List Questions

```http
GET /api/v1/questions?questionType=single&difficulty=Easy&category=Java%20Core&keyword=inherit&page=1&limit=20
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "success": true,
  "data": [
    {
      "id": 1000,
      "content": "Which Java keyword is used to inherit a class?",
      "questionType": "single",
      "category": "Java Core",
      "difficulty": "Easy",
      "optionsJson": [
        {
          "key": "A",
          "text": "implements"
        },
        {
          "key": "B",
          "text": "extends"
        }
      ],
      "correctAnswersJson": [
        "B"
      ],
      "explanation": "`extends` is used when a class inherits from another class.",
      "createdBy": 1000,
      "updatedBy": 1000,
      "createdAt": "2026-05-28T15:00:00",
      "updatedAt": "2026-05-28T15:00:00"
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
| `400` | `INVALID_QUESTION_OPTIONS_JSON` | `optionsJson` is not a non-empty JSON array. |
| `400` | `INVALID_QUESTION_CORRECT_ANSWERS_JSON` | `correctAnswersJson` is not a non-empty JSON array. |
| `400` | `INVALID_SINGLE_QUESTION_ANSWER` | Single-choice question has zero or multiple correct answers. |
| `404` | `NOT_FOUND` | Question does not exist or was soft-deleted. |
