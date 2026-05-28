# 7. System Scope Freeze

Status: frozen for backend v1 implementation. Any change after this point must be handled as a change request and reviewed against business logic, database impact, API compatibility, permission impact, and migration cost.

---

## 7.1 Frozen Modules

### In Scope for v1

| Module | Scope |
|---|---|
| Authentication | Login, Google login, refresh token, logout, current user context. |
| User and Role Management | User CRUD, status toggle, role assignment, permission matrix. |
| Syllabus | Create, edit draft, submit, publish, close draft/pending, import CSV, manage outline and materials. |
| Training Program | Create, edit planning program, attach ordered syllabuses, publish, close. |
| Class Management | Create class from active training program, assign class admins/trainers, publish, close. |
| Quiz and Question Bank | Question CRUD, quiz draft, publish, close, assignment, trainee attempt, result, review. |
| Training Calendar | Training sessions, registration, waitlist, attendance, trainer/session management. |
| Notifications | In-app notifications for core workflow events. Email remains supported where settings exist. |
| Files | Avatar upload, syllabus material upload, import CSV storage/validation. |
| Settings and Audit | System settings, notification settings, permission settings, audit log view. |

### Out of Scope for v1

| Area | Reason |
|---|---|
| Payment, billing, invoice | No frontend evidence and not part of FAP training flow. |
| External LMS integration | Not required by current screens. |
| Advanced reporting/dashboard BI | Existing dashboards are summary views only. |
| Certificate generation engine | Certificate download is visible, but generation template workflow is not specified. Keep issuance metadata only unless later defined. |
| Multi-tenant organization management | Current scope assumes one FAP deployment with roles and ownership scopes. |
| Real-time collaborative editing | No frontend support. |
| Complex approval chains | Syllabus has simple submit/publish, not multi-step approval. |

---

## 7.2 Frozen Roles

| Role | Scope |
|---|---|
| Super Admin | Full system administration, all resources, all ownership scopes. |
| Class Admin | Manage assigned classes and training operations; may manage syllabus/program/class if permission matrix grants it. |
| Trainer | Manage assigned sessions/classes, attendance, learning materials if granted, quiz/question operations if granted. |
| Trainee | Self-service user: view assigned learning resources, register training sessions, take assigned quizzes, view own results/profile. |

Role rules:
- The frontend permission matrix remains resource-level, but backend authorization is action-based.
- Permission check runs first, ownership check runs second.
- Super Admin bypasses ownership checks.
- Trainer and Class Admin must be scoped to assigned classes/sessions.
- Trainee must be scoped to own registrations, attempts, attendance, profile, and assigned quizzes.

---

## 7.3 Frozen Entities

### Identity and Security

| Entity | Status |
|---|---|
| `users` | Canonical |
| `roles` | Canonical |
| `user_roles` | Canonical |
| `permissions` | Canonical |
| `refresh_tokens` | Canonical for auth/session security |

### Syllabus

| Entity | Status |
|---|---|
| `syllabuses` | Canonical aggregate root |
| `syllabus_output_standards` | Canonical |
| `syllabus_days` | Canonical |
| `syllabus_units` | Canonical |
| `syllabus_topics` | Canonical |
| `material_files` | Canonical |

### Training Program and Class

| Entity | Status |
|---|---|
| `training_programs` | Canonical aggregate root |
| `training_program_syllabuses` | Canonical ordered join |
| `classes` | Canonical aggregate root |
| `class_trainers` | Canonical |
| `class_admins` | Canonical |

### Quiz

| Entity | Status |
|---|---|
| `questions` | Canonical |
| `quizzes` | Canonical aggregate root |
| `quiz_questions` | Canonical ordered join |
| `quiz_assignments` | Canonical scope/eligibility table |
| `quiz_attempts` | Canonical attempt/result table |

### Training Calendar

| Entity | Status |
|---|---|
| `training_sessions` | Canonical aggregate root |
| `training_registrations` | Canonical registration/waitlist state |
| `attendance_records` | Canonical attendance state |
| `training_feedbacks` | Canonical post-session feedback state |

### System

| Entity | Status |
|---|---|
| `notifications` | Canonical in-app notification table |
| `audit_logs` | Canonical immutable audit table |
| `system_settings` | Canonical key/value settings table |

Entity rules:
- No new aggregate root should be added to v1 without replacing one of the frozen entities above.
- Cached counters are allowed only when explicitly documented. Currently `training_sessions.enrolled_count` is cached.
- `version_no` optimistic locking is required on mutable aggregate roots.

---

## 7.4 Frozen API Surface

### Authentication

| Method | Endpoint | Scope |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Password login |
| `POST` | `/api/v1/auth/google` | Google OAuth login |
| `POST` | `/api/v1/auth/refresh` | Rotate refresh token |
| `POST` | `/api/v1/auth/logout` | Revoke refresh token/session |

### Users, Roles, Permissions

| Method | Endpoint | Scope |
|---|---|---|
| `GET` | `/api/v1/users` | List/filter users |
| `POST` | `/api/v1/users` | Create user |
| `GET` | `/api/v1/users/{id}` | User detail |
| `PUT` | `/api/v1/users/{id}` | Update user |
| `PATCH` | `/api/v1/users/{id}/status` | Activate/deactivate |
| `GET` | `/api/v1/roles` | List roles |
| `GET` | `/api/v1/roles/permissions` | Permission matrix |
| `PUT` | `/api/v1/roles/permissions` | Update permission matrix |

### Syllabus

| Method | Endpoint | Scope |
|---|---|---|
| `GET` | `/api/v1/syllabuses` | List/filter |
| `POST` | `/api/v1/syllabuses` | Create draft |
| `GET` | `/api/v1/syllabuses/{id}` | Detail |
| `PUT` | `/api/v1/syllabuses/{id}` | Update editable syllabus |
| `PATCH` | `/api/v1/syllabuses/{id}/status` | Submit/publish/close allowed states |
| `DELETE` | `/api/v1/syllabuses/{id}` | Soft-delete allowed states |
| `POST` | `/api/v1/syllabuses/import` | Import CSV |
| `POST` | `/api/v1/syllabuses/{id}/topics/{topicId}/materials` | Upload material |
| `PUT` | `/api/v1/materials/{id}` | Rename/update material metadata |
| `DELETE` | `/api/v1/materials/{id}` | Delete material |

### Training Program and Class

| Method | Endpoint | Scope |
|---|---|---|
| `GET` | `/api/v1/training-programs` | List/filter |
| `POST` | `/api/v1/training-programs` | Create planning program |
| `GET` | `/api/v1/training-programs/{id}` | Detail |
| `PUT` | `/api/v1/training-programs/{id}` | Update planning program |
| `PATCH` | `/api/v1/training-programs/{id}/status` | Publish/close |
| `DELETE` | `/api/v1/training-programs/{id}` | Soft-delete allowed states |
| `GET` | `/api/v1/classes` | List/filter |
| `POST` | `/api/v1/classes` | Create planning class |
| `GET` | `/api/v1/classes/{id}` | Detail |
| `PUT` | `/api/v1/classes/{id}` | Update planning class |
| `GET` | `/api/v1/classes/{id}/admins` | List class admins |
| `PUT` | `/api/v1/classes/{id}/admins` | Assign class admins |
| `GET` | `/api/v1/classes/{id}/trainers` | List trainers |
| `PUT` | `/api/v1/classes/{id}/trainers` | Assign trainers |
| `PATCH` | `/api/v1/classes/{id}/status` | Publish/close |
| `DELETE` | `/api/v1/classes/{id}` | Soft-delete allowed states |

### Quiz

| Method | Endpoint | Scope |
|---|---|---|
| `GET` | `/api/v1/questions` | List/filter question bank |
| `POST` | `/api/v1/questions` | Create question |
| `GET` | `/api/v1/questions/{id}` | Question detail |
| `PUT` | `/api/v1/questions/{id}` | Update question |
| `DELETE` | `/api/v1/questions/{id}` | Soft-delete unused question |
| `GET` | `/api/v1/quizzes` | List/filter quizzes |
| `POST` | `/api/v1/quizzes` | Create draft quiz |
| `GET` | `/api/v1/quizzes/{id}` | Quiz detail |
| `PUT` | `/api/v1/quizzes/{id}` | Update draft quiz |
| `PATCH` | `/api/v1/quizzes/{id}/status` | Publish/close |
| `POST` | `/api/v1/quizzes/{id}/assignments` | Assign quiz to class/session |
| `GET` | `/api/v1/quizzes/assigned` | Trainee assigned quizzes |
| `POST` | `/api/v1/quizzes/{id}/attempts` | Submit attempt |
| `GET` | `/api/v1/quizzes/{id}/attempts/{attemptId}` | Result/review |

### Training Calendar

| Method | Endpoint | Scope |
|---|---|---|
| `GET` | `/api/v1/training-sessions` | List/filter sessions |
| `POST` | `/api/v1/training-sessions` | Create session |
| `GET` | `/api/v1/training-sessions/{id}` | Session detail |
| `PUT` | `/api/v1/training-sessions/{id}` | Update upcoming session |
| `PATCH` | `/api/v1/training-sessions/{id}/status` | Complete/cancel |
| `POST` | `/api/v1/training-sessions/{id}/registrations` | Register self |
| `DELETE` | `/api/v1/training-sessions/{id}/registrations/me` | Cancel self registration |
| `GET` | `/api/v1/training-sessions/{id}/participants` | Participant/waitlist view |
| `PUT` | `/api/v1/training-sessions/{id}/attendance` | Upsert attendance batch |
| `POST` | `/api/v1/training-sessions/{id}/feedback` | Submit post-session feedback |
| `GET` | `/api/v1/training-sessions/{id}/feedback-summary` | Feedback rating summary |

### Dashboards and Self-Service Views

| Method | Endpoint | Scope |
|---|---|---|
| `GET` | `/api/v1/me/training-registrations` | Current user's training registration list |
| `GET` | `/api/v1/me/training-sessions` | Current user's registered training sessions |
| `GET` | `/api/v1/me/attendance` | Current user's attendance records |
| `GET` | `/api/v1/me/feedback` | Current user's submitted training feedback |
| `GET` | `/api/v1/me/training-dashboard` | Current user's trainee learning summary |
| `GET` | `/api/v1/me/trainer-dashboard` | Current trainer's teaching dashboard |
| `GET` | `/api/v1/me/class-admin-dashboard` | Current class admin's managed-class dashboard |

### System

| Method | Endpoint | Scope |
|---|---|---|
| `GET` | `/api/v1/notifications` | My notifications |
| `PATCH` | `/api/v1/notifications/{id}/read` | Mark read |
| `GET` | `/api/v1/settings` | Read settings |
| `PUT` | `/api/v1/settings` | Update settings |
| `GET` | `/api/v1/audit-logs` | Audit log list/filter |

API rules:
- Status updates use `PATCH /{resource}/{id}/status`.
- Deletes are soft-delete unless explicitly retention cleanup.
- List endpoints use standard pagination response.
- API additions after freeze require updating this document first.

---

## 7.5 Frozen Workflows

### Syllabus Workflow

1. Admin/authorized user creates syllabus in `Drafting`.
2. User edits general info, outline, assessment, time allocation, output standards, and materials.
3. User submits `Drafting -> Pending`.
4. Authorized publisher publishes `Pending -> Active`.
5. `Active` syllabus is immutable. Revisions are new versions.

### Training Program Workflow

1. Admin creates program in `Planning`.
2. Admin attaches ordered active syllabuses.
3. Admin publishes `Planning -> Active`.
4. Program can be used by classes.
5. Program closes `Active -> Inactive` when no longer used for new classes.

### Class Workflow

1. Class Admin/Super Admin creates class in `Planning` from active program.
2. Assign class admins and trainers.
3. Validate schedule and required assignments.
4. Publish `Planning -> Active`.
5. Close `Active -> Closed` after delivery.

### Quiz Workflow

1. Admin/Trainer creates questions.
2. Admin/Trainer creates quiz in `Draft`.
3. Add ordered questions and configure duration, dates, passing score, attempts.
4. Publish `Draft -> Published`.
5. Assign quiz to class/session.
6. Trainee sees assigned quiz, submits attempts, views result/review.
7. Close `Published -> Closed`.

### Training Session Registration Workflow

1. Admin/Trainer creates `Upcoming` session.
2. Trainee registers.
3. If capacity exists, status is `Registered`.
4. If full, status is `Waitlist`.
5. Cancellation promotes earliest waitlisted trainee atomically.
6. Session completion finalizes attendance and participant completion.

### Attendance Workflow

1. Trainer/Class Admin opens session attendance.
2. Attendance is upserted per participant.
3. QR check-in marks only current authenticated user.
4. Manual correction after completion requires reason and audit log.

### Training Feedback Workflow

1. Session must be marked `Completed`.
2. Trainee submits one feedback record per completed session.
3. Backend validates current user is a registered or completed participant.
4. Trainer/Class Admin/Super Admin can view feedback summary within their session scope.

### Permission Workflow

1. Super Admin updates role-resource permission matrix.
2. Backend stores permission level.
3. Runtime authorization maps level to action.
4. Ownership scope is checked after permission.
5. Every permission matrix update is audited.

---

## 7.6 Freeze Rules

- Treat this file as the v1 contract between frontend, backend, QA, and database.
- If an entity is not listed, it is not part of v1 persistence scope.
- If an endpoint is not listed, it is not part of v1 API scope.
- If a status transition is not listed, backend must reject it with `409 Conflict`.
- If a role behavior is not listed, backend must deny by default.
- Any post-freeze change must update: scope freeze, schema, API contract, permission matrix, workflow docs, and tests.
