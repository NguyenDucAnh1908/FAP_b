-- Performance indexes found by the optimization audit (docs/optimization-plan.md, phase 1).
-- Each block tolerates ORA-00955 (name already used) and ORA-01408 (column list already indexed)
-- so a partially applied run can be repaired and re-run, matching the style of earlier migrations.

-- Case-insensitive lookups. Spring Data derives *IgnoreCase as UPPER(column) = UPPER(?), which
-- cannot use the plain unique index, so login and duplicate checks scanned the whole table.
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_users_email_upper ON users (UPPER(email))';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_classes_code_upper ON classes (UPPER(class_code))';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_syllabuses_code_upper ON syllabuses (UPPER(code))';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

-- Foreign keys that are filtered/joined on and had no index. Besides the lookups, an unindexed FK
-- makes a delete or key update on the parent lock the whole child table.
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_user_roles_role ON user_roles (role_id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_qa_quiz ON quiz_assignments (quiz_id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_qa_class ON quiz_assignments (class_id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_qa_session ON quiz_assignments (training_session_id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_result_quizzes_quiz ON course_result_quizzes (quiz_id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_result_quizzes_attempt ON course_result_quizzes (best_attempt_id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_completion_quizzes_quiz ON class_completion_quizzes (quiz_id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_class_trainers_syllabus ON class_trainers (syllabus_id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

-- Schedule conflict checks run on every session create/update:
--   class/trainer/room = ? AND start_time < :end AND end_time > :start
-- The composite indexes lead with the same columns as idx_ts_class / idx_ts_trainer, which are
-- therefore redundant and dropped to avoid paying for both on every write.
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_ts_class_time ON training_sessions (class_id, start_time, end_time)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_ts_trainer_time ON training_sessions (trainer_id, start_time, end_time)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX idx_ts_class';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1418 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX idx_ts_trainer';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1418 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_ts_room_time ON training_sessions (LOWER(TRIM(room)), start_time)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

-- Analytics and dashboards filter sessions by date without a status (idx_ts_status_date leads
-- with status, so it cannot serve them).
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_ts_session_date ON training_sessions (session_date)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

-- Audit log list defaults to newest first with no filter.
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_audit_created_at ON audit_logs (created_at)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/

-- Dashboard counts of submitted/passed attempts within a date range.
BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_attempt_status_submitted ON quiz_attempts (status, submitted_at)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE NOT IN (-955, -1408) THEN
            RAISE;
        END IF;
END;
/
