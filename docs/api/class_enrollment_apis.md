# Class Enrollment APIs

`class_enrollments` is the official class roster. `training_registrations` remains the roster for one training session.

The schema is introduced by `V29__create_class_enrollments_and_registration_modes.sql`. Version 28 is already used by local seed sequence synchronization.

## Class fields

Class create/update requests include:

- `capacity`: positive integer, default `30` in the database.
- `selfEnrollmentEnabled`: whether Trainees may enroll themselves.
- `enrollmentStartDate`, `enrollmentEndDate`: optional inclusive date window.

Class responses also include `enrolledCount` and `waitlistCount`.

Training session create/update requests include `registrationMode`: `AutoEnroll` or `SelfEnroll`.

## Staff endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/classes/{id}/enrollments` | Search and page class enrollment history |
| `POST` | `/api/v1/classes/{id}/enrollments` | Add active Trainees with `{ "userIds": [1, 2] }` |
| `PATCH` | `/api/v1/classes/{id}/enrollments/{userId}/approve` | Approve a pending self-enrollment request |
| `PATCH` | `/api/v1/classes/{id}/enrollments/{userId}/reject` | Reject a pending self-enrollment request |
| `DELETE` | `/api/v1/classes/{id}/enrollments/{userId}` | Withdraw a Trainee without deleting history |

Super Admin manages every class. Class Admin manages assigned classes. Trainer has read-only access to assigned classes.

## Trainee endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/me/available-classes` | List active classes currently open for self enrollment |
| `GET` | `/api/v1/me/class-enrollments` | List the current user's class enrollment history |
| `POST` | `/api/v1/classes/{id}/enrollments/me` | Submit an enrollment request with status `PendingApproval` |
| `DELETE` | `/api/v1/classes/{id}/enrollments/me` | Cancel a pending request or withdraw from the class/waitlist |

## Rules

- Only active users with role `Trainee` can join a class.
- Self-enrollment never grants immediate class access. Super Admin or the assigned Class Admin must approve it first.
- An approved request becomes `Enrolled` when capacity remains, otherwise it becomes `Waitlisted`.
- A rejected or withdrawn request may be submitted again while the class is still open for enrollment.
- Admin-added Trainees do not require approval.
- Capacity is serialized with a database row lock during approval. A full class places approved requests on the waitlist.
- Withdrawing an enrolled Trainee promotes the earliest waitlisted Trainee.
- Closing a class calculates each learner's course result (`Passed`, `Failed`, or `Withdrawn`). Enrollment rows are not mass-changed to `Completed`.
- `AutoEnroll` sessions mirror the class roster. `SelfEnroll` sessions require an `Enrolled` class enrollment first.
- Leaving a class preserves existing attendance and quiz history, but cancels future session registrations.
