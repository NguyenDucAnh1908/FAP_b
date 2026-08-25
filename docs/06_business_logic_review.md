# 6. Business Logic Review and Corrections

This document summarizes the business-rule review across database schema, state machines, API contracts, security, and transactions. It is the source-of-truth checklist for backend implementation.

---

## 6.1 Key Corrections

| Area | Problem Found | Correction |
|---|---|---|
| Permission model | `create` and `modify` were treated as a simple hierarchy. | Evaluate by action map, not ordinal level comparison. |
| Syllabus lifecycle | `Active -> Inactive` was implied despite UI disabling close. | `Active` syllabus is immutable; create a new version for changes. |
| Training program lifecycle | Active programs were deletable in the inferred state machine. | Active programs cannot be deleted; close to `Inactive`. |
| Class lifecycle | UI condition implied non-active classes, including `Closed`, can be published. | Only `Planning -> Active`; `Closed` cannot be republished. |
| Quiz lifecycle | UI condition implied `Closed -> Published` and deleting published quizzes. | Only `Draft -> Published -> Closed`; clone for republish/revision. |
| Quiz visibility | Published quizzes had no assignment scope. | Add `quiz_assignments` for class/session-scoped visibility. |
| Registration capacity | `enrolled_count` could drift under concurrency. | Treat as cached counter updated atomically with registration rows. |
| Attendance | Batch delete/reinsert loses audit history. | Upsert by participant and require correction reason after completion. |
| Database dependency guards | Soft-delete rules were too broad. | Block deletion while active dependent records exist. |

---

## 6.2 Correct Permission Semantics

Permission levels remain compatible with the frontend matrix, but backend checks must use actions.

| Stored level | Allowed actions |
|---|---|
| `access_denied` | none |
| `view` | `read` |
| `create` | `read`, `create` |
| `modify` | `read`, `update`, `transition` |
| `full_access` | `read`, `create`, `update`, `transition`, `delete`, `admin` |

Rules:
- Do not compare levels with `>=`.
- Ownership checks run after resource permission.
- Super Admin bypasses ownership checks.
- Class Admin is scoped to administered classes.
- Trainer is scoped to assigned classes/sessions.
- Trainee is scoped to self-service actions and assigned learning/quiz resources.

---

## 6.3 Correct Status Lifecycles

### Syllabus

Allowed:
- `Drafting -> Pending`
- `Pending -> Active`
- `Drafting -> Inactive`
- `Pending -> Inactive`

Rules:
- Submit/publish requires at least one day, unit, topic, assessment total = 100, and time allocation total = 100.
- `Active` is immutable and cannot be deleted or closed by normal flow.
- `Inactive` is read-only and terminal for content editing.

### Training Program

Allowed:
- `Planning -> Active`
- `Planning -> Inactive`
- `Active -> Inactive`

Rules:
- Publishing requires at least one linked syllabus.
- Every linked syllabus must be `Active`.
- Active programs cannot be deleted.
- Inactive programs cannot be assigned to new classes.

### Class

Allowed:
- `Planning -> Active`
- `Active -> Closed`

Rules:
- Publishing requires active program, class admin, trainer assignments, and valid dates.
- Active classes cannot be deleted.
- Closed classes cannot be republished.
- Closing requires every session to be `Completed` or `Canceled`, at least one completed session, and every required quiz to be `Closed`.
- Closing calculates authoritative `course_results`; it does not mass-convert class enrollments to `Completed`.
- Default minimum attendance is 80%; `Present` and `Late` count as attended.
- Required class quizzes use the best submitted attempt and the class policy passing score.
- Manual result adjustment is limited to `Passed`/`Failed`, requires a reason, retains history, and invalidates prior publication.
- Result publication is allowed only for a closed class and sends an audit event plus trainee notifications.

### Quiz

Allowed:
- `Draft -> Published`
- `Published -> Closed`

Rules:
- Publishing requires questions, valid score range, duration, and valid open/close dates.
- Published grading content is immutable.
- Closed quizzes cannot be republished.
- Attempts require assignment to the trainee's class/session.

### Training Session and Registration

Rules:
- Registration only for `Upcoming` sessions.
- `enrolled_count` counts only `Registered` rows.
- Waitlist promotion is FIFO by `registered_at`.
- Completed registration is assigned by system/admin after attendance finalization.

---

## 6.4 Database Corrections

Required additions:
- `quiz_assignments` table for class/session quiz scope.
- `quiz_attempts.attempt_number` with unique `(quiz_id, user_id, attempt_number)`.
- Unique `(quiz_id, question_id)` and `(quiz_id, sort_order)` for quiz questions.
- Optional `training_sessions.class_id` to connect calendar sessions to class ownership.
- Unique `(training_id, user_id)` for attendance records.
- `attendance_records.updated_by` and `correction_reason`.
- `version_no` optimistic lock column on mutable aggregate roots.

Derived/cached data:
- `training_sessions.enrolled_count` is cached, not authoritative.
- Source of truth is `training_registrations` where `status = 'Registered'`.

Delete guards:
- No delete for active/published operational records.
- No soft-delete while active dependencies exist.
- Hard delete is retention-only.

---

## 6.5 Backend Enforcement Checklist

- Validate current status and requested transition in service layer.
- Add DB unique constraints for all ordering and attempt-number rules.
- Add optimistic locking on stateful aggregate roots.
- Replace permission ordinal comparison with action-based permission evaluation.
- Add ownership predicates to trainer/class-admin endpoints.
- Enforce quiz assignment before allowing attempt submission.
- Use atomic SQL or row locks for registration capacity and waitlist promotion.
- Upsert attendance instead of delete/reinsert.
- Audit every permission change, status transition, delete, material file operation, and post-completion attendance correction.
