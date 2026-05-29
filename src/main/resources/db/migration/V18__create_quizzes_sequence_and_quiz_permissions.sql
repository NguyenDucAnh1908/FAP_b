BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE quizzes_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE quiz_questions ADD points NUMBER(5,2) DEFAULT 1 NOT NULL';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE quiz_questions ADD CONSTRAINT ck_qq_points CHECK (points > 0)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2264 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE permissions DROP CONSTRAINT ck_permissions_resource';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2443 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE q'[
        ALTER TABLE permissions ADD CONSTRAINT ck_permissions_resource
        CHECK (resource_name IN ('syllabus', 'training_program', 'class', 'learning_material', 'user', 'quiz'))
    ]';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2264 THEN
            RAISE;
        END IF;
END;
/

INSERT INTO permissions (id, role_id, resource_name, permission_level)
SELECT permissions_seq.NEXTVAL,
       r.id,
       'quiz',
       CASE
           WHEN r.name IN ('Super Admin', 'Class Admin', 'Trainer') THEN 'full_access'
           ELSE 'view'
       END
FROM roles r
WHERE r.name IN ('Super Admin', 'Class Admin', 'Trainer', 'Trainee')
  AND NOT EXISTS (
      SELECT 1
      FROM permissions p
      WHERE p.role_id = r.id
        AND p.resource_name = 'quiz'
  );
