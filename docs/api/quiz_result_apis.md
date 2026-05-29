# Quiz Result Management APIs

Quiz Result APIs let Super Admin, Class Admin, and Trainer review submitted quiz attempts and aggregate result metrics.

All endpoints require a valid JWT access token.

## Endpoint Summary

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/v1/quizzes/{quizId}/attempts` | List attempts for a quiz. |
| `GET` | `/api/v1/quizzes/{quizId}/attempts/{attemptId}` | Review a submitted trainee attempt. |
| `GET` | `/api/v1/quizzes/{quizId}/attempt-summary` | Get summary metrics for quiz attempts. |

## Scope Rules

- Super Admin can view all quiz results.
- Class Admin can view results for classes they manage.
- Trainer can view results for classes/sessions they are assigned to.
- Trainee cannot use these management APIs.
- `classId` and `trainingSessionId` filters are validated against the current user's assignment scope.
- Attempt detail/review is available only after the attempt is `Submitted`.

## List Attempts

```http
GET /api/v1/quizzes/1000/attempts?classId=1000&status=Submitted&passed=true&page=1&limit=20
Authorization: Bearer <accessToken>
Accept-Language: vi
```

Available filters:

| Query | Type | Notes |
|---|---|---|
| `classId` | number | Limit to trainees registered in this class. |
| `trainingSessionId` | number | Limit to trainees registered in this session. |
| `userId` | number | Limit to one trainee. |
| `status` | enum | `InProgress` or `Submitted`. |
| `passed` | boolean | Applies to submitted attempts. |
| `page` | number | 1-based page. |
| `limit` | number | Max 100. |

Success response:

```json
{
  "success": true,
  "data": [
    {
      "id": 1000,
      "quizId": 1000,
      "quizTitle": "Java Core Basics Quiz",
      "userId": 1010,
      "userFullName": "Nguyen Van A",
      "userEmail": "trainee@fap.local",
      "attemptNumber": 1,
      "status": "Submitted",
      "score": 85,
      "correctCount": 17,
      "totalQuestions": 20,
      "passed": true,
      "timeTakenSeconds": 1420,
      "startedAt": "2026-05-29T08:30:00",
      "submittedAt": "2026-05-29T08:53:40"
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

## Review Attempt

```http
GET /api/v1/quizzes/1000/attempts/1000
Authorization: Bearer <accessToken>
```

Success response returns the submitted attempt with selected answers, correct answers, correctness per question, and explanation.

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
          { "key": "B", "text": "implements" }
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

## Attempt Summary

```http
GET /api/v1/quizzes/1000/attempt-summary?classId=1000
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "success": true,
  "data": {
    "quizId": 1000,
    "quizTitle": "Java Core Basics Quiz",
    "totalAttempts": 12,
    "inProgressAttempts": 2,
    "submittedAttempts": 10,
    "passedAttempts": 8,
    "failedAttempts": 2,
    "passRate": 80.0,
    "averageScore": 78.5,
    "highestScore": 100,
    "lowestScore": 45
  }
}
```

Common errors:

| HTTP | Code | Meaning |
|---|---|---|
| `403` | `ACCESS_DENIED` | Current user is not allowed to view this result scope. |
| `404` | `NOT_FOUND` | Quiz, attempt, class, or training session was not found. |
| `409` | `QUIZ_ATTEMPT_REVIEW_UNAVAILABLE` | Attempt is still in progress. |
