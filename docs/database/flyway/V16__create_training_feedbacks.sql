BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE training_feedbacks_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE '
        CREATE TABLE training_feedbacks (
            id NUMBER(19) PRIMARY KEY,
            training_id NUMBER(19) NOT NULL,
            user_id NUMBER(19) NOT NULL,
            rating_content NUMBER(2) NOT NULL,
            rating_trainer NUMBER(2) NOT NULL,
            rating_organization NUMBER(2) NOT NULL,
            comment CLOB,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
            CONSTRAINT fk_feedback_training FOREIGN KEY (training_id) REFERENCES training_sessions(id) ON DELETE CASCADE,
            CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users(id),
            CONSTRAINT uk_feedback_training_user UNIQUE (training_id, user_id),
            CONSTRAINT ck_feedback_content_rating CHECK (rating_content BETWEEN 1 AND 5),
            CONSTRAINT ck_feedback_trainer_rating CHECK (rating_trainer BETWEEN 1 AND 5),
            CONSTRAINT ck_feedback_org_rating CHECK (rating_organization BETWEEN 1 AND 5)
        )';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_feedback_training ON training_feedbacks(training_id, created_at)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE INDEX idx_feedback_user ON training_feedbacks(user_id, created_at)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/
