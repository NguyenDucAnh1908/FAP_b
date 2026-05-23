-- Create Oracle sequences for JPA ID generation and seed initial access data.
-- Initial local password for admin@fap.local: password
-- Change this password immediately after first login.

BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE users_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE roles_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE permissions_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE refresh_tokens_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT roles_seq.NEXTVAL,
       'Super Admin',
       'Full system administrator',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'Super Admin');

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT roles_seq.NEXTVAL,
       'Class Admin',
       'Class and training operation administrator',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'Class Admin');

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT roles_seq.NEXTVAL,
       'Trainer',
       'Trainer assigned to classes and sessions',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'Trainer');

INSERT INTO roles (id, name, description, created_at, updated_at)
SELECT roles_seq.NEXTVAL,
       'Trainee',
       'Self-service trainee user',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'Trainee');

INSERT INTO users (
    id,
    full_name,
    email,
    password_hash,
    gender,
    status,
    is_deleted,
    version_no,
    created_at,
    updated_at
)
SELECT users_seq.NEXTVAL,
       'System Super Admin',
       'admin@fap.local',
       '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi7xP18N3aZ51WkS4h.9i5wVP5PZ8y6',
       'Male',
       'Active',
       0,
       0,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@fap.local');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.email = 'admin@fap.local'
  AND r.name = 'Super Admin'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
  );

INSERT INTO permissions (id, role_id, resource_name, permission_level)
SELECT permissions_seq.NEXTVAL,
       r.id,
       perm_resource.resource_name,
       'full_access'
FROM roles r
CROSS JOIN (
    SELECT 'user' resource_name FROM dual UNION ALL
    SELECT 'syllabus' FROM dual UNION ALL
    SELECT 'training_program' FROM dual UNION ALL
    SELECT 'class' FROM dual UNION ALL
    SELECT 'learning_material' FROM dual
) perm_resource
WHERE r.name = 'Super Admin'
  AND NOT EXISTS (
      SELECT 1
      FROM permissions p
      WHERE p.role_id = r.id
        AND p.resource_name = perm_resource.resource_name
  );

INSERT INTO permissions (id, role_id, resource_name, permission_level)
SELECT permissions_seq.NEXTVAL,
       r.id,
       perm_resource.resource_name,
       perm_resource.permission_level
FROM roles r
CROSS JOIN (
    SELECT 'user' resource_name, 'view' permission_level FROM dual UNION ALL
    SELECT 'syllabus', 'modify' FROM dual UNION ALL
    SELECT 'training_program', 'modify' FROM dual UNION ALL
    SELECT 'class', 'full_access' FROM dual UNION ALL
    SELECT 'learning_material', 'modify' FROM dual
) perm_resource
WHERE r.name = 'Class Admin'
  AND NOT EXISTS (
      SELECT 1
      FROM permissions p
      WHERE p.role_id = r.id
        AND p.resource_name = perm_resource.resource_name
  );

INSERT INTO permissions (id, role_id, resource_name, permission_level)
SELECT permissions_seq.NEXTVAL,
       r.id,
       perm_resource.resource_name,
       perm_resource.permission_level
FROM roles r
CROSS JOIN (
    SELECT 'user' resource_name, 'view' permission_level FROM dual UNION ALL
    SELECT 'syllabus', 'view' FROM dual UNION ALL
    SELECT 'training_program', 'view' FROM dual UNION ALL
    SELECT 'class', 'modify' FROM dual UNION ALL
    SELECT 'learning_material', 'modify' FROM dual
) perm_resource
WHERE r.name = 'Trainer'
  AND NOT EXISTS (
      SELECT 1
      FROM permissions p
      WHERE p.role_id = r.id
        AND p.resource_name = perm_resource.resource_name
  );

INSERT INTO permissions (id, role_id, resource_name, permission_level)
SELECT permissions_seq.NEXTVAL,
       r.id,
       perm_resource.resource_name,
       'view'
FROM roles r
CROSS JOIN (
    SELECT 'syllabus' resource_name FROM dual UNION ALL
    SELECT 'training_program' FROM dual UNION ALL
    SELECT 'class' FROM dual UNION ALL
    SELECT 'learning_material' FROM dual
) perm_resource
WHERE r.name = 'Trainee'
  AND NOT EXISTS (
      SELECT 1
      FROM permissions p
      WHERE p.role_id = r.id
        AND p.resource_name = perm_resource.resource_name
  );
