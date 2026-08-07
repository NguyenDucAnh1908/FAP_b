BEGIN
    EXECUTE IMMEDIATE '
        CREATE TABLE user_avatar_contents (
            user_id      NUMBER(19)     PRIMARY KEY,
            file_data    BLOB           NOT NULL,
            content_type VARCHAR2(100)  NOT NULL,
            CONSTRAINT fk_avatar_user FOREIGN KEY (user_id)
                REFERENCES users(id) ON DELETE CASCADE
        )
    ';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/
