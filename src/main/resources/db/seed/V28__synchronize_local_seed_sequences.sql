-- LOCAL/DEV ONLY.
--
-- V23 restarted several sequences at 100 even when a table already contained IDs >= 1000.
-- Keep every sequence at or above both its current next value and MAX(id) + 1 so future
-- JPA inserts cannot collide with the initial admin or manually-created local records.

DECLARE
    PROCEDURE synchronize_sequence(
        p_sequence_name IN VARCHAR2,
        p_table_name    IN VARCHAR2
    ) IS
        v_table_next    NUMBER;
        v_sequence_next NUMBER;
        v_target        NUMBER;
    BEGIN
        EXECUTE IMMEDIATE
            'SELECT NVL(MAX(id), 0) + 1 FROM ' || DBMS_ASSERT.SIMPLE_SQL_NAME(p_table_name)
            INTO v_table_next;

        SELECT last_number
        INTO v_sequence_next
        FROM user_sequences
        WHERE sequence_name = UPPER(p_sequence_name);

        v_target := GREATEST(v_table_next, v_sequence_next);

        EXECUTE IMMEDIATE
            'ALTER SEQUENCE ' || DBMS_ASSERT.SIMPLE_SQL_NAME(p_sequence_name)
            || ' RESTART START WITH ' || TO_CHAR(v_target, 'FM9999999999999999990');
    END;
BEGIN
    synchronize_sequence('users_seq', 'users');
    synchronize_sequence('roles_seq', 'roles');
    synchronize_sequence('permissions_seq', 'permissions');
    synchronize_sequence('refresh_tokens_seq', 'refresh_tokens');
    synchronize_sequence('password_reset_tokens_seq', 'password_reset_tokens');
    synchronize_sequence('audit_logs_seq', 'audit_logs');
    synchronize_sequence('syllabuses_seq', 'syllabuses');
    synchronize_sequence('syllabus_days_seq', 'syllabus_days');
    synchronize_sequence('syllabus_units_seq', 'syllabus_units');
    synchronize_sequence('syllabus_topics_seq', 'syllabus_topics');
    synchronize_sequence('material_files_seq', 'material_files');
    synchronize_sequence('training_programs_seq', 'training_programs');
    synchronize_sequence('classes_seq', 'classes');
    synchronize_sequence('class_trainers_seq', 'class_trainers');
    synchronize_sequence('training_sessions_seq', 'training_sessions');
    synchronize_sequence('training_registrations_seq', 'training_registrations');
    synchronize_sequence('attendance_records_seq', 'attendance_records');
    synchronize_sequence('notifications_seq', 'notifications');
    synchronize_sequence('training_feedbacks_seq', 'training_feedbacks');
    synchronize_sequence('questions_seq', 'questions');
    synchronize_sequence('quizzes_seq', 'quizzes');
    synchronize_sequence('quiz_assignments_seq', 'quiz_assignments');
    synchronize_sequence('quiz_attempts_seq', 'quiz_attempts');
    synchronize_sequence('system_settings_seq', 'system_settings');
END;
/
