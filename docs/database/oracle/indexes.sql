-- FAP Backend v1 Oracle indexes
-- These are the non-PK/non-unique-support indexes used for list filters, ownership checks, and hot paths.
-- The canonical migration already includes these statements in schema.sql.

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_deleted ON users(is_deleted);
CREATE INDEX idx_permissions_role ON permissions(role_id);

CREATE INDEX idx_syll_status ON syllabuses(status);
CREATE INDEX idx_syll_name ON syllabuses(name);
CREATE INDEX idx_syll_deleted ON syllabuses(is_deleted);
CREATE INDEX idx_syll_created_by ON syllabuses(created_by);
CREATE INDEX idx_material_topic ON material_files(topic_id);

CREATE INDEX idx_tp_status ON training_programs(status);
CREATE INDEX idx_tp_deleted ON training_programs(is_deleted);
CREATE INDEX idx_tp_syll_syllabus ON training_program_syllabuses(syllabus_id);

CREATE INDEX idx_classes_status ON classes(status);
CREATE INDEX idx_classes_tp ON classes(training_program_id);
CREATE INDEX idx_classes_dates ON classes(start_date, end_date);
CREATE INDEX idx_classes_deleted ON classes(is_deleted);
CREATE INDEX idx_class_trainers_user ON class_trainers(user_id);
CREATE INDEX idx_class_admins_user ON class_admins(user_id);
CREATE INDEX idx_class_enrollments_class_status ON class_enrollments(class_id, status);
CREATE INDEX idx_class_enrollments_user_status ON class_enrollments(user_id, status);
CREATE INDEX idx_completion_quizzes_class ON class_completion_quizzes(class_id);
CREATE INDEX idx_course_results_class_status ON course_results(class_id, calculated_status);
CREATE INDEX idx_course_results_publish ON course_results(class_id, published_at);
CREATE INDEX idx_result_adjustments_result ON course_result_adjustments(course_result_id, adjusted_at);

CREATE INDEX idx_questions_category ON questions(category);
CREATE INDEX idx_questions_diff ON questions(difficulty);
CREATE INDEX idx_questions_deleted ON questions(is_deleted);
CREATE INDEX idx_quizzes_status ON quizzes(status);
CREATE INDEX idx_quizzes_category ON quizzes(category);
CREATE INDEX idx_quizzes_dates ON quizzes(open_date, close_date);
CREATE INDEX idx_quizzes_deleted ON quizzes(is_deleted);
CREATE INDEX idx_qq_question ON quiz_questions(question_id);
CREATE INDEX idx_attempt_user ON quiz_attempts(user_id);
CREATE INDEX idx_attempt_quiz_user ON quiz_attempts(quiz_id, user_id);

CREATE INDEX idx_ts_class ON training_sessions(class_id);
CREATE INDEX idx_ts_trainer ON training_sessions(trainer_id);
CREATE INDEX idx_ts_status_date ON training_sessions(status, session_date);
CREATE INDEX idx_ts_deleted ON training_sessions(is_deleted);
CREATE INDEX idx_reg_training_status ON training_registrations(training_id, status, registered_at);
CREATE INDEX idx_reg_user ON training_registrations(user_id);
CREATE INDEX idx_att_user ON attendance_records(user_id);

CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read, created_at);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_user_time ON audit_logs(user_id, created_at);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_exp ON refresh_tokens(expires_at);
