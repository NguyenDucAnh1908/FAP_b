# Quiz Assignment APIs

Quiz Assignment APIs make a published quiz available to a class or a training session.

All endpoints require a valid JWT access token.

Swagger test flow:

1. Create questions with `/api/v1/questions`.
2. Create a quiz with `/api/v1/quizzes`.
3. Attach questions with `PUT /api/v1/quizzes/{id}/questions`.
4. Publish the quiz with `PATCH /api/v1/quizzes/{id}/status`.
5. Assign the published quiz to a class or training session.

## Endpoint Summary

| Method | Endpoint | Permission | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/quizzes/{id}/assignments` | `quiz:view` | List assignments for a quiz. |
| `POST` | `/api/v1/quizzes/{id}/assignments` | `quiz:modify` | Assign a published quiz. |
| `DELETE` | `/api/v1/quizzes/{id}/assignments/{assignmentId}` | `quiz:modify` | Remove an assignment. |

## Rules

- Only `Published` quizzes can be assigned.
- The request must provide exactly one of `classId` or `trainingSessionId`.
- Duplicate assignment for the same quiz and class is rejected.
- Duplicate assignment for the same quiz and training session is rejected.
- Class or training session must exist.
- Closed quizzes cannot have assignments added or removed.
- Every assign/unassign action is audited.

## Assign to Class

```http
POST /api/v1/quizzes/1000/assignments
Authorization: Bearer <accessToken>
Content-Type: application/json
Accept-Language: vi
```

```json
{
  "classId": 1000
}
```

## Assign to Training Session

```json
{
  "trainingSessionId": 1000
}
```

## List Assignments

```http
GET /api/v1/quizzes/1000/assignments
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "success": true,
  "data": [
    {
      "id": 1000,
      "quizId": 1000,
      "quizTitle": "Java Core Basics Quiz",
      "classId": 1000,
      "className": "Java Foundation",
      "classCode": "JAVA-FOUNDATION-01",
      "trainingSessionId": null,
      "trainingSessionTitle": null,
      "assignedBy": 1000,
      "assignedByName": "Super Admin",
      "assignedByEmail": "admin@fap.local",
      "assignedAt": "2026-05-29T10:00:00"
    }
  ],
  "message": null
}
```

Common errors:

| HTTP | Code | Meaning |
|---|---|---|
| `400` | `INVALID_QUIZ_ASSIGNMENT_SCOPE` | Provide exactly one of `classId` or `trainingSessionId`. |
| `404` | `NOT_FOUND` | Quiz, class, training session, or assignment was not found. |
| `409` | `QUIZ_NOT_ASSIGNABLE` | Quiz is not `Published`. |
| `409` | `DUPLICATE_QUIZ_CLASS_ASSIGNMENT` | Quiz is already assigned to this class. |
| `409` | `DUPLICATE_QUIZ_SESSION_ASSIGNMENT` | Quiz is already assigned to this training session. |
