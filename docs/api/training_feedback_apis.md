# Training Feedback APIs

Training feedback closes the loop after a training session is completed.

All endpoints require a valid JWT access token.

Swagger test flow:

1. Login with `POST /api/v1/auth/login`.
2. Copy `accessToken`.
3. Click `Authorize` in Swagger UI.
4. Enter `Bearer <accessToken>`.
5. Optional: set `Accept-Language` to `vi` or `en`.

## Endpoint Summary

| Method | Endpoint | Actor | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/training-sessions/{id}/feedback` | Trainee / registered participant | Submit feedback for a completed training session. |
| `GET` | `/api/v1/training-sessions/{id}/feedback-summary` | Super Admin, assigned Class Admin, assigned Trainer | View aggregate feedback ratings. |
| `GET` | `/api/v1/me/feedback` | Current user | View feedback submitted by the logged-in user. |

## Rules

- Feedback is allowed only for `Completed` training sessions.
- The current user must have a `Registered` or `Completed` registration for the session.
- Each user can submit feedback only once per training session.
- Feedback summary follows existing session ownership scope:
  - Super Admin can view all.
  - Class Admin can view assigned classes.
  - Trainer can view assigned or taught sessions.
- The client never sends `userId`; the backend uses the authenticated principal.

## Submit Feedback

```http
POST /api/v1/training-sessions/1001/feedback
Authorization: Bearer <accessToken>
Content-Type: application/json
Accept-Language: vi
```

Request body:

```json
{
  "ratingContent": 5,
  "ratingTrainer": 5,
  "ratingOrganization": 4,
  "comment": "Session was practical and easy to follow."
}
```

Success response:

```json
{
  "success": true,
  "data": {
    "id": 1001,
    "trainingSessionId": 1001,
    "trainingSessionTitle": "Java Foundation - Day 1",
    "userId": 1005,
    "userFullName": "Trainee User",
    "userEmail": "trainee@fap.local",
    "ratingContent": 5,
    "ratingTrainer": 5,
    "ratingOrganization": 4,
    "comment": "Session was practical and easy to follow.",
    "createdAt": "2026-05-28T15:00:00",
    "updatedAt": "2026-05-28T15:00:00"
  },
  "message": null
}
```

Common errors:

| HTTP | Code | Meaning |
|---|---|---|
| `409` | `FEEDBACK_SESSION_NOT_COMPLETED` | Session is not completed yet. |
| `409` | `FEEDBACK_ALREADY_SUBMITTED` | User already submitted feedback for this session. |
| `409` | `FEEDBACK_REGISTRATION_REQUIRED` | User is not registered for this session. |
| `409` | `FEEDBACK_REGISTRATION_NOT_ELIGIBLE` | Registration status is not eligible. |

## Feedback Summary

```http
GET /api/v1/training-sessions/1001/feedback-summary
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "success": true,
  "data": {
    "trainingSessionId": 1001,
    "feedbackCount": 12,
    "averageContentRating": 4.75,
    "averageTrainerRating": 4.83,
    "averageOrganizationRating": 4.41,
    "overallAverageRating": 4.663333333333333
  },
  "message": null
}
```

If a session has no feedback yet, averages are returned as `0.0`.

## My Feedback

```http
GET /api/v1/me/feedback?page=1&limit=20
Authorization: Bearer <accessToken>
```

Success response:

```json
{
  "success": true,
  "data": [
    {
      "id": 1001,
      "trainingSessionId": 1001,
      "trainingSessionTitle": "Java Foundation - Day 1",
      "userId": 1005,
      "userFullName": "Trainee User",
      "userEmail": "trainee@fap.local",
      "ratingContent": 5,
      "ratingTrainer": 5,
      "ratingOrganization": 4,
      "comment": "Session was practical and easy to follow.",
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

## Minimal Data Setup

1. Create or reuse an active training program and active class.
2. Assign Class Admin and Trainer.
3. Create a training session.
4. Register a trainee for the session.
5. Upsert attendance if your flow requires it.
6. Mark session `Completed`.
7. Login as the trainee and submit feedback.
8. Login as Trainer/Class Admin/Super Admin and view summary.
