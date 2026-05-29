BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE quiz_attempts_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE q'[ALTER TABLE quiz_attempts ADD status VARCHAR2(20) DEFAULT 'Submitted' NOT NULL]';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE quiz_attempts ADD started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE quiz_attempts MODIFY (score NULL, correct_count NULL, total_questions NULL, passed NULL, time_taken_seconds NULL, submitted_at NULL)';
END;
/

BEGIN
    EXECUTE IMMEDIATE q'[ALTER TABLE quiz_attempts ADD CONSTRAINT ck_attempt_answers_json CHECK (answers_json IS JSON)]';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2264 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE q'[ALTER TABLE quiz_attempts ADD CONSTRAINT ck_attempt_status CHECK (status IN ('InProgress', 'Submitted'))]';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2264 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE q'[
        ALTER TABLE quiz_attempts ADD CONSTRAINT ck_attempt_submission_complete CHECK (
            status = 'InProgress'
            OR (
                score IS NOT NULL
                AND correct_count IS NOT NULL
                AND total_questions IS NOT NULL
                AND passed IS NOT NULL
                AND time_taken_seconds IS NOT NULL
                AND submitted_at IS NOT NULL
            )
        )
    ]';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2264 THEN
            RAISE;
        END IF;
END;
/
