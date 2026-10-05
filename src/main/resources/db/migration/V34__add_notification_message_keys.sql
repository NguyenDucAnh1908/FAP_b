-- Notifications store the message key and its arguments so the text can be rendered in the
-- reader's language when it is read. title/message stay NOT NULL: they hold the English rendering,
-- which is the fallback for rows written before this migration (key columns NULL) and for a key
-- that later disappears from the bundles.
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE notifications ADD title_key VARCHAR2(100)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE notifications ADD message_key VARCHAR2(100)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE notifications ADD message_args CLOB';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE q'[ALTER TABLE notifications ADD CONSTRAINT ck_notifications_args_json CHECK (message_args IS JSON)]';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2264 THEN
            RAISE;
        END IF;
END;
/
