# FAP Database Indexes and Constraints

Source DDL: [database/oracle/schema.sql](./oracle/schema.sql)

---

## Primary Keys

All aggregate/root tables use `NUMBER(19) PRIMARY KEY`. JPA ID generation is backed by explicit Oracle sequences created in Flyway migrations.

Composite primary keys:
- `user_roles(user_id, role_id)`
- `syllabus_output_standards(syllabus_id, standard_code)`
- `training_program_syllabuses(program_id, syllabus_id)`
- `class_admins(class_id, user_id)`
- `quiz_questions(quiz_id, question_id)`

---

## Unique Constraints

Identity/security:
- `users.email`
- `roles.name`
- `permissions(role_id, resource_name)`

Syllabus:
- `syllabuses.code`
- `syllabus_days(syllabus_id, day_number)`
- `syllabus_days(syllabus_id, sort_order)`
- `syllabus_units(day_id, sort_order)`
- `syllabus_topics(unit_id, sort_order)`

Training program/class:
- `training_program_syllabuses(program_id, sort_order)`
- `classes.class_code`
- `class_trainers(class_id, user_id, NVL(syllabus_id, -1))` via function-based unique index
- `class_enrollments(class_id, user_id)`

Quiz:
- `quiz_questions(quiz_id, question_id)` via primary key
- `quiz_questions(quiz_id, sort_order)`
- `quiz_assignments(quiz_id, class_id)` via function-based unique index for class-scoped assignments only
- `quiz_assignments(quiz_id, training_session_id)` via function-based unique index for session-scoped assignments only
- `quiz_attempts(quiz_id, user_id, attempt_number)`

Calendar:
- `training_registrations(training_id, user_id)`
- `attendance_records(training_id, user_id)`
- `training_feedbacks(training_id, user_id)`

System:
- `system_settings(category, setting_key)`
- `refresh_tokens.token`

---

## Business Check Constraints

Identity/security:
- `users.gender IN ('Male', 'Female')`
- `users.status IN ('Active', 'Inactive')`
- `permissions.resource_name IN ('syllabus', 'training_program', 'class', 'learning_material', 'user', 'quiz')`
- `permissions.permission_level IN ('access_denied', 'view', 'create', 'modify', 'full_access')`

Syllabus:
- `syllabuses.status IN ('Drafting', 'Pending', 'Active', 'Inactive')`
- `syllabuses.level_name IN ('Beginner', 'Intermediate', 'Advanced', 'All levels')`
- `syllabuses.attendees > 0`
- Time allocation total must equal `100`
- Assessment total must equal `100`
- Output standards limited to `H4SD`, `K6SD`, `H1SD`, `C3SD`, `H2SD`
- Day/unit/topic sort orders must be positive
- Topic duration must be positive

Training program/class:
- `training_programs.status IN ('Planning', 'Active', 'Inactive')`
- `training_programs.total_hours IS NULL OR total_hours >= 0`
- `classes.status IN ('Planning', 'Active', 'Closed')`
- `classes.start_date <= classes.end_date` when both are set
- `classes.capacity > 0`
- `classes.self_enrollment_enabled IN (0, 1)`
- `classes.enrollment_start_date <= classes.enrollment_end_date` when both are set
- `class_enrollments.status IN ('PendingApproval', 'Enrolled', 'Waitlisted', 'Rejected', 'Withdrawn', 'Completed')`
- `class_enrollments.source IN ('AdminAdded', 'SelfRegistered', 'Migration')`
- Withdrawn/completed enrollments require their matching timestamp

Quiz:
- `questions.question_type IN ('single', 'multiple')`
- `questions.difficulty IN ('Easy', 'Medium', 'Hard')`
- `quizzes.duration_minutes > 0`
- `quizzes.passing_score BETWEEN 0 AND 100`
- `quizzes.max_attempts > 0`
- `quizzes.status IN ('Draft', 'Published', 'Closed')`
- `quizzes.open_date <= quizzes.close_date` when both are set
- `quiz_questions.points > 0`
- `quiz_assignments` requires exactly one of `class_id` or `training_session_id`
- `quiz_attempts.answers_json IS JSON`
- `quiz_attempts.status IN ('InProgress', 'Submitted')`
- Attempt score/count/time bounds are enforced
- Submitted attempts require score, counts, pass flag, time taken, and `submitted_at`

Calendar:
- `training_sessions.session_type IN ('Offline', 'Online', 'Hybrid')`
- `training_sessions.status IN ('Upcoming', 'Completed', 'Canceled')`
- `training_sessions.capacity > 0`
- `training_sessions.enrolled_count >= 0 AND enrolled_count <= capacity`
- `training_sessions.registration_mode IN ('AutoEnroll', 'SelfEnroll')`
- `training_sessions.end_time > training_sessions.start_time`
- `training_registrations.status IN ('Registered', 'Waitlist', 'Completed', 'Cancelled')`
- `Cancelled` registrations require `cancelled_at`
- `Completed` registrations require `completed_at`
- `attendance_records.status IN ('Present', 'Late', 'Absent')`
- `attendance_records.check_in_method IN ('Manual', 'QR')`
- `training_feedbacks.rating_content BETWEEN 1 AND 5`
- `training_feedbacks.rating_trainer BETWEEN 1 AND 5`
- `training_feedbacks.rating_organization BETWEEN 1 AND 5`

System:
- Boolean-like flags are `NUMBER(1)` with `CHECK (... IN (0, 1))`

---

## Foreign Key Rules

Cascade delete is used only for owned child rows:
- Syllabus children: output standards, days, units, topics, material files
- Training program syllabus join rows
- Class trainer/admin join rows
- Quiz question rows and quiz assignment rows
- Training registration and attendance rows under training session
- Training feedback rows under training session

No cascade delete from shared/reference parents:
- `users`
- `roles`
- `syllabuses` referenced by programs/classes
- `training_programs`
- `classes`
- `questions`
- `quizzes`

This preserves business history and prevents accidental removal of audit-relevant records.

---

## Indexes by Query Pattern

User/permission:
- `idx_users_status`
- `idx_users_deleted`
- `idx_permissions_role`

Syllabus/material:
- `idx_syll_status`
- `idx_syll_name`
- `idx_syll_deleted`
- `idx_syll_created_by`
- `idx_material_topic`

Program/class:
- `idx_tp_status`
- `idx_tp_deleted`
- `idx_tp_syll_syllabus`
- `idx_classes_status`
- `idx_classes_tp`
- `idx_classes_dates`
- `idx_classes_deleted`
- `idx_class_trainers_user`
- `idx_class_admins_user`
- `idx_class_enrollments_class_status`
- `idx_class_enrollments_user_status`
- `idx_completion_quizzes_class`
- `idx_course_results_class_status`
- `idx_course_results_publish`
- `idx_result_adjustments_result`

Quiz/question:
- `idx_questions_category`
- `idx_questions_diff`
- `idx_questions_deleted`
- `idx_quizzes_status`
- `idx_quizzes_category`
- `idx_quizzes_dates`
- `idx_quizzes_deleted`
- `idx_qq_question`
- `idx_attempt_user`
- `idx_attempt_quiz_user`

Training calendar:
- `idx_ts_class`
- `idx_ts_trainer`
- `idx_ts_status_date`
- `idx_ts_deleted`
- `idx_reg_training_status`
- `idx_reg_user`
- `idx_att_user`
- `idx_feedback_training`
- `idx_feedback_user`

System:
- `idx_notifications_user_read`
- `idx_audit_entity`
- `idx_audit_user_time`
- `idx_refresh_user`
- `idx_refresh_exp`

---

## Constraints Not Fully Enforceable by DDL Alone

These must be implemented in service logic, triggers, or stored procedures:

- Active syllabus immutability.
- Published quiz grading immutability after attempts exist.
- Question `options_json` and `correct_answers_json` must be valid non-empty JSON arrays.
- Single-choice questions must have exactly one correct answer.
- Training program publish requires at least one active syllabus.
- Class publish requires active program, class admin, trainer assignment, and valid schedule.
- Quiz publish requires at least one question.
- Quiz attempt requires assignment to trainee class/session.
- `training_sessions.enrolled_count` must equal count of `Registered` rows under concurrent registration.
- `classes.capacity` must equal or exceed the number of `Enrolled` class enrollments.
- `AutoEnroll` sessions synchronize registrations from the current class roster.
- Leaving a class cancels only future session registrations and preserves learning history.
- Closing a class requires completed/canceled sessions, closed required quizzes, and atomically finalized course results.
- Course result adjustment accepts only `Passed`/`Failed`, requires a reason, retains history, and clears publication.
- Waitlist FIFO promotion on cancellation.
- Feedback is allowed only once per participant per completed session.
- Trainer/Class Admin ownership scopes.
- Soft-delete blocked when active dependencies exist.
