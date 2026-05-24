CREATE TABLE password_reset_tokens (
                                       id NUMBER(19) PRIMARY KEY,
                                       user_id NUMBER(19) NOT NULL,
                                       token_hash VARCHAR2(64) NOT NULL,
                                       expires_at TIMESTAMP NOT NULL,
                                       used NUMBER(1) DEFAULT 0 NOT NULL,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                       used_at TIMESTAMP,
                                       CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id),
                                       CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
                                       CONSTRAINT ck_password_reset_used CHECK (used IN (0, 1))
);

CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_exp ON password_reset_tokens(expires_at);

BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE password_reset_tokens_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/
