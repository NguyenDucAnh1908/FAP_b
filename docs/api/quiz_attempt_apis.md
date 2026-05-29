# Quiz Attempt APIs

Quiz Attempt APIs let a trainee see assigned quizzes, start an attempt, save answers, submit, and view the result.

All endpoints require a valid JWT access token.

## Endpoint Summary

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/v1/quizzes/assigned` | List quizzes assigned to the current user. |
| `POST` | `/api/v1/quizzes/{quizId}/attempts` | Start a new in-progress attempt. |
| `GET` | `/api/v1/quiz-attempts/{attemptId}` | Get own attempt detail/result. |
| `PUT` | `/api/v1/quiz-attempts/{attemptId}/answers` | Save answers for an in-progress attempt. |
| `POST` | `/api/v1/quiz-attempts/{attemptId}/submit` | Submit and grade an in-progress attempt. |
| `GET` | `/api/v1/quiz-attempts/{attemptId}/review` | Review submitted answers with correct answers and explanations. |

## Rules

- Only `Published` quizzes can be attempted.
- `openDate` and `closeDate` are enforced when configured.
- The current user must have a `Registered` or `Completed` training registration matching the quiz assignment.
- A quiz assignment can target either a class or a training session.
- A user can have only one `InProgress` attempt per quiz.
- `maxAttempts` limits total attempts per user per quiz.
- `durationMinutes` is enforced from `startedAt`.
- If an in-progress attempt is already expired when the user calls detail, save, submit, or review, the backend auto-submits the last saved answers.
- Submitted attempts are immutable.
- Answers must use JSON arrays, for example `["A"]` or `["A", "C"]`.
- Multiple-choice grading is order-insensitive.
- Review is available only after the attempt is submitted.

## List Assigned Quizzes

```http
GET /api/v1/quizzes/assigned?page=1&limit=20
Authorization: Bearer <accessToken>
Accept-Language: vi
```

Success response:

```json
{
  "success": true,
  "data": [
    {
      "id": 1000,
      "title": "Java Core Basics Quiz",
      "description": "Quick check for Java fundamentals",
      "durationMinutes": 30,
      "passingScore": 70,
      "maxAttempts": 2,
      "category": "Java",
      "openDate": "2026-05-29",
      "closeDate": "2026-06-05",
      "questionCount": 2,
      "attemptCount": 0,
      "remainingAttempts": 2,
      "latestAttemptId": null,
      "latestAttemptStatus": null,
      "latestScore": null,
      "latestPassed": null
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

## Start Attempt

```http
POST /api/v1/quizzes/1000/attempts
Authorization: Bearer <accessToken>
```

Success response returns the attempt and quiz questions without correct answers:

```json
{
  "success": true,
  "data": {
    "id": 1000,
    "quizId": 1000,
    "quizTitle": "Java Core Basics Quiz",
    "attemptNumber": 1,
    "status": "InProgress",
    "answersJson": [],
    "score": null,
    "correctCount": null,
    "totalQuestions": null,
    "passed": null,
    "timeTakenSeconds": null,
    "startedAt": "2026-05-29T08:30:00",
    "submittedAt": null,
    "questions": [
      {
        "questionId": 1000,
        "sortOrder": 1,
        "points": 1.0,
        "content": "Which keyword creates inheritance in Java?",
        "questionType": "single",
        "category": "Java",
        "difficulty": "Easy",
        "optionsJson": [
          { "key": "A", "text": "extends" },
          { "key": "B", "text": "implements" },
          { "key": "C", "text": "inherits" }
        ]
      }
    ]
  }
}
```

## Save Answers

```http
PUT /api/v1/quiz-attempts/1000/answers
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "answers": [
    {
      "questionId": 1000,
      "selectedAnswersJson": ["A"]
    },
    {
      "questionId": 1001,
      "selectedAnswersJson": ["B", "C"]
    }
  ]
}
```

## Submit Attempt

```http
POST /api/v1/quiz-attempts/1000/submit
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "success": true,
  "data": {
    "id": 1000,
    "quizId": 1000,
    "quizTitle": "Java Core Basics Quiz",
    "attemptNumber": 1,
    "status": "Submitted",
    "answersJson": [
      {
        "questionId": 1000,
        "selectedAnswersJson": ["A"]
      }
    ],
    "score": 100,
    "correctCount": 1,
    "totalQuestions": 1,
    "passed": true,
    "timeTakenSeconds": 120,
    "startedAt": "2026-05-29T08:30:00",
    "submittedAt": "2026-05-29T08:32:00",
    "questions": [
      {
        "questionId": 1000,
        "sortOrder": 1,
        "points": 1.0,
        "content": "Which keyword creates inheritance in Java?",
        "questionType": "single",
        "category": "Java",
        "difficulty": "Easy",
        "optionsJson": [
          { "key": "A", "text": "extends" },
          { "key": "B", "text": "implements" },
          { "key": "C", "text": "inherits" }
        ]
      }
    ]
  }
}
```

## Review Attempt

```http
GET /api/v1/quiz-attempts/1000/review
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "success": true,
  "data": {
    "id": 1000,
    "quizId": 1000,
    "quizTitle": "Java Core Basics Quiz",
    "attemptNumber": 1,
    "status": "Submitted",
    "answersJson": [
      {
        "questionId": 1000,
        "selectedAnswersJson": ["A"]
      }
    ],
    "score": 100,
    "correctCount": 1,
    "totalQuestions": 1,
    "passed": true,
    "timeTakenSeconds": 120,
    "startedAt": "2026-05-29T08:30:00",
    "submittedAt": "2026-05-29T08:32:00",
    "questions": [
      {
        "questionId": 1000,
        "sortOrder": 1,
        "points": 1.0,
        "content": "Which keyword creates inheritance in Java?",
        "questionType": "single",
        "category": "Java",
        "difficulty": "Easy",
        "optionsJson": [
          { "key": "A", "text": "extends" },
          { "key": "B", "text": "implements" },
          { "key": "C", "text": "inherits" }
        ],
        "selectedAnswersJson": ["A"],
        "correctAnswersJson": ["A"],
        "correct": true,
        "explanation": "Java classes inherit from another class with extends."
      }
    ]
  }
}
```

Common errors:

| HTTP | Code | Meaning |
|---|---|---|
| `400` | `DUPLICATE_QUIZ_ANSWER` | Request contains duplicate question answers. |
| `400` | `QUIZ_ANSWER_ARRAY_REQUIRED` | `selectedAnswersJson` must be a JSON array. |
| `400` | `QUIZ_ANSWER_QUESTION_NOT_FOUND` | Answer references a question outside the quiz. |
| `404` | `NOT_FOUND` | Quiz or attempt was not found. |
| `409` | `QUIZ_NOT_AVAILABLE` | Quiz is not `Published`. |
| `409` | `QUIZ_NOT_OPEN` | Quiz is not open yet. |
| `409` | `QUIZ_CLOSED` | Quiz is already closed. |
| `409` | `QUIZ_ASSIGNMENT_REQUIRED` | Current user is not eligible for this quiz. |
| `409` | `QUIZ_ATTEMPT_IN_PROGRESS` | User already has an in-progress attempt for this quiz. |
| `409` | `QUIZ_ATTEMPT_LIMIT_REACHED` | User has reached `maxAttempts`. |
| `409` | `QUIZ_ATTEMPT_NOT_EDITABLE` | Attempt has already been submitted. |
| `409` | `QUIZ_ATTEMPT_REVIEW_UNAVAILABLE` | Attempt is still in progress and not yet expired. |
