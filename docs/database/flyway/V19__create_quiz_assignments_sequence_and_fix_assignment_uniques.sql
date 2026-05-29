BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE quiz_assignments_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX uk_qa_quiz_class';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1418 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'DROP INDEX uk_qa_quiz_session';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1418 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE '
        CREATE UNIQUE INDEX uk_qa_quiz_class ON quiz_assignments (
            CASE WHEN class_id IS NOT NULL THEN quiz_id END,
            CASE WHEN class_id IS NOT NULL THEN class_id END
        )';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE '
        CREATE UNIQUE INDEX uk_qa_quiz_session ON quiz_assignments (
            CASE WHEN training_session_id IS NOT NULL THEN quiz_id END,
            CASE WHEN training_session_id IS NOT NULL THEN training_session_id END
        )';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/
