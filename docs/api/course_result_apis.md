# Course Result & Completion APIs

Base path: `/api/v1`. All endpoints require JWT and the existing `class` permission.

## Completion policy

### `GET /classes/{classId}/completion-policy`

Visible to Super Admin, assigned Class Admin, and assigned Trainer.

### `PUT /classes/{classId}/completion-policy`

Super Admin or assigned Class Admin only. A required quiz must already be assigned directly to the class.

```json
{
  "minimumAttendanceRate": 80,
  "requiredQuizzes": [
    { "quizId": 12, "passingScore": 70 }
  ]
}
```

## Gradebook

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/classes/{classId}/results` | Summary and all learner results |
| `GET` | `/classes/{classId}/results/{userId}` | Attendance, quiz snapshots, override, publication, and adjustment history |
| `POST` | `/classes/{classId}/results/calculate` | Recalculate an active class without closing it |
| `PATCH` | `/classes/{classId}/results/{userId}` | Override to `Passed` or `Failed`; reason is mandatory |
| `POST` | `/classes/{classId}/results/publish` | Publish all unpublished results of a closed class |

Override request:

```json
{
  "status": "Passed",
  "reason": "Approved after attendance evidence review"
}
```

Trainer has read-only gradebook access. Mutating endpoints require Super Admin or the assigned Class Admin.

## Trainee result

`GET /me/classes/{classId}/result` returns only the authenticated trainee's result and only after publication. Before publication it returns `409 COURSE_RESULT_NOT_PUBLISHED`.

## Calculation and close rules

- Course result states: `InProgress`, `Passed`, `Failed`, `Withdrawn`.
- Waitlisted learners do not have results.
- `Present` and `Late` count as attended.
- Only completed sessions for which the learner has a registered/completed registration are included; canceled sessions are excluded.
- Required quiz result uses the highest score from submitted attempts.
- `Active -> Closed` fails when an upcoming session remains, no completed session exists, a required quiz is not closed, or a result cannot be finalized.
- Publication is idempotent: an already published unchanged result does not receive another notification.
