-- Pooled id allocation (docs/optimization-plan.md, phase 3).
--
-- With INCREMENT BY 1 Hibernate made one sequence round trip per inserted row, which also kept
-- JDBC insert batching from paying off. Each NEXTVAL now reserves a block of 50 ids that the
-- entities' allocationSize = 50 (Hibernate's pooled optimizer) hands out in memory. The value
-- returned by NEXTVAL is the upper end of its block, so rows inserted with NEXTVAL directly (seed
-- scripts) can never collide with ids Hibernate hands out.
--
-- Ids are no longer contiguous: a restart abandons the rest of a block, and CACHE 20 abandons
-- cached values on instance shutdown. Accepted in the optimization plan.
--
-- allocationSize and INCREMENT BY must change together: Hibernate refuses to start when they
-- differ, so deploy this migration with the matching entity change.

ALTER SEQUENCE attendance_records_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE audit_logs_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE class_completion_quizzes_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE class_enrollments_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE class_trainers_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE classes_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE course_result_adjustments_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE course_result_quizzes_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE course_results_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE material_files_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE notifications_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE password_reset_tokens_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE permissions_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE questions_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE quiz_assignments_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE quiz_attempts_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE quizzes_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE refresh_tokens_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE roles_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE syllabus_days_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE syllabus_topics_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE syllabus_units_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE syllabuses_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE system_settings_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE training_feedbacks_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE training_programs_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE training_registrations_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE training_sessions_seq INCREMENT BY 50 CACHE 20;
ALTER SEQUENCE users_seq INCREMENT BY 50 CACHE 20;
