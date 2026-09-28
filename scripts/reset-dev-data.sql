-- DEV/LOCAL ONLY.
-- Run with SQL*Plus while the backend is stopped:
--   sqlplus fap/<local-password>@localhost:1521/XEPDB1 @scripts/reset-dev-data.sql
--
-- This script deletes only the coherent E2E dataset and run-scoped records created by
-- docs/api-test-flow.http, scripts/verify-e2e-flow.ps1, or docs/UI_TEST_FLOW.md. It deliberately
-- does not reset sequences, truncate tables, disable constraints, or touch the older V22 dataset.

WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

DECLARE
    v_seed_marker NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_seed_marker
    FROM system_settings
    WHERE category = 'DEV_SEED'
      AND setting_key = 'dataset_version'
      AND DBMS_LOB.COMPARE(setting_value, TO_CLOB('e2e-v1')) = 0;

    IF v_seed_marker != 1 THEN
        RAISE_APPLICATION_ERROR(
            -20001,
            'Refusing reset: DEV_SEED dataset marker e2e-v1 was not found.'
        );
    END IF;
END;
/

-- Remove run-scoped API/UI flow data first. Every predicate is restricted to the documented
-- FLOW-/API Flow/UI- prefixes so unrelated local records are preserved.
DELETE FROM refresh_tokens
WHERE user_id IN (
    SELECT id FROM users
    WHERE REGEXP_LIKE(email, '^flow\.(classadmin|trainer|trainee)\.[0-9]+@fap\.local$')
       OR email LIKE 'ui.trainee.%@fap.local'
);

DELETE FROM password_reset_tokens
WHERE user_id IN (
    SELECT id FROM users
    WHERE REGEXP_LIKE(email, '^flow\.(classadmin|trainer|trainee)\.[0-9]+@fap\.local$')
       OR email LIKE 'ui.trainee.%@fap.local'
);

DELETE FROM audit_logs
WHERE user_id IN (
    SELECT id FROM users
    WHERE REGEXP_LIKE(email, '^flow\.(classadmin|trainer|trainee)\.[0-9]+@fap\.local$')
       OR email LIKE 'ui.trainee.%@fap.local'
);

DELETE FROM notifications
WHERE user_id IN (
    SELECT id FROM users
    WHERE REGEXP_LIKE(email, '^flow\.(classadmin|trainer|trainee)\.[0-9]+@fap\.local$')
       OR email LIKE 'ui.trainee.%@fap.local'
);

DELETE FROM quiz_attempts
WHERE quiz_id IN (SELECT id FROM quizzes WHERE title LIKE 'API Flow Java Readiness %')
   OR user_id IN (
       SELECT id FROM users
       WHERE REGEXP_LIKE(email, '^flow\.trainee\.[0-9]+@fap\.local$')
          OR email LIKE 'ui.trainee.%@fap.local'
   );

DELETE FROM quiz_assignments
WHERE quiz_id IN (SELECT id FROM quizzes WHERE title LIKE 'API Flow Java Readiness %')
   OR class_id IN (SELECT id FROM classes WHERE class_code LIKE 'FLOW-CLASS-%' OR class_code LIKE 'UI-JAVA-CLASS-%')
   OR training_session_id IN (
       SELECT ts.id
       FROM training_sessions ts
       JOIN classes c ON c.id = ts.class_id
       WHERE c.class_code LIKE 'FLOW-CLASS-%' OR c.class_code LIKE 'UI-JAVA-CLASS-%'
   );

DELETE FROM quiz_questions
WHERE quiz_id IN (SELECT id FROM quizzes WHERE title LIKE 'API Flow Java Readiness %')
   OR question_id IN (SELECT id FROM questions WHERE content LIKE 'API Flow:%');

DELETE FROM quizzes WHERE title LIKE 'API Flow Java Readiness %';
DELETE FROM questions WHERE content LIKE 'API Flow:%';

DELETE FROM training_feedbacks
WHERE training_id IN (
    SELECT ts.id
    FROM training_sessions ts
    JOIN classes c ON c.id = ts.class_id
    WHERE c.class_code LIKE 'FLOW-CLASS-%' OR c.class_code LIKE 'UI-JAVA-CLASS-%'
);

DELETE FROM attendance_records
WHERE training_id IN (
    SELECT ts.id
    FROM training_sessions ts
    JOIN classes c ON c.id = ts.class_id
    WHERE c.class_code LIKE 'FLOW-CLASS-%' OR c.class_code LIKE 'UI-JAVA-CLASS-%'
);

DELETE FROM training_registrations
WHERE training_id IN (
    SELECT ts.id
    FROM training_sessions ts
    JOIN classes c ON c.id = ts.class_id
    WHERE c.class_code LIKE 'FLOW-CLASS-%' OR c.class_code LIKE 'UI-JAVA-CLASS-%'
);

DELETE FROM training_sessions
WHERE class_id IN (
    SELECT id FROM classes WHERE class_code LIKE 'FLOW-CLASS-%' OR class_code LIKE 'UI-JAVA-CLASS-%'
);

DELETE FROM class_trainers
WHERE class_id IN (
    SELECT id FROM classes WHERE class_code LIKE 'FLOW-CLASS-%' OR class_code LIKE 'UI-JAVA-CLASS-%'
);

DELETE FROM class_admins
WHERE class_id IN (
    SELECT id FROM classes WHERE class_code LIKE 'FLOW-CLASS-%' OR class_code LIKE 'UI-JAVA-CLASS-%'
);

DELETE FROM classes WHERE class_code LIKE 'FLOW-CLASS-%' OR class_code LIKE 'UI-JAVA-CLASS-%';

DELETE FROM training_program_syllabuses
WHERE program_id IN (
    SELECT id FROM training_programs
    WHERE name LIKE 'API Flow Training Program %' OR name LIKE 'UI Java Backend Program %'
)
OR syllabus_id IN (
    SELECT id FROM syllabuses WHERE code LIKE 'FLOW-JAVA-%' OR code LIKE 'UI-JAVA-%'
);

DELETE FROM training_programs
WHERE name LIKE 'API Flow Training Program %' OR name LIKE 'UI Java Backend Program %';

DELETE FROM material_file_contents
WHERE material_file_id IN (
    SELECT mf.id
    FROM material_files mf
    JOIN syllabus_topics st ON st.id = mf.topic_id
    JOIN syllabus_units su ON su.id = st.unit_id
    JOIN syllabus_days sd ON sd.id = su.day_id
    JOIN syllabuses s ON s.id = sd.syllabus_id
    WHERE s.code LIKE 'FLOW-JAVA-%' OR s.code LIKE 'UI-JAVA-%'
);

DELETE FROM material_files
WHERE topic_id IN (
    SELECT st.id
    FROM syllabus_topics st
    JOIN syllabus_units su ON su.id = st.unit_id
    JOIN syllabus_days sd ON sd.id = su.day_id
    JOIN syllabuses s ON s.id = sd.syllabus_id
    WHERE s.code LIKE 'FLOW-JAVA-%' OR s.code LIKE 'UI-JAVA-%'
);

DELETE FROM syllabus_topics
WHERE unit_id IN (
    SELECT su.id
    FROM syllabus_units su
    JOIN syllabus_days sd ON sd.id = su.day_id
    JOIN syllabuses s ON s.id = sd.syllabus_id
    WHERE s.code LIKE 'FLOW-JAVA-%' OR s.code LIKE 'UI-JAVA-%'
);

DELETE FROM syllabus_units
WHERE day_id IN (
    SELECT sd.id
    FROM syllabus_days sd
    JOIN syllabuses s ON s.id = sd.syllabus_id
    WHERE s.code LIKE 'FLOW-JAVA-%' OR s.code LIKE 'UI-JAVA-%'
);

DELETE FROM syllabus_days
WHERE syllabus_id IN (
    SELECT id FROM syllabuses WHERE code LIKE 'FLOW-JAVA-%' OR code LIKE 'UI-JAVA-%'
);

DELETE FROM syllabus_output_standards
WHERE syllabus_id IN (
    SELECT id FROM syllabuses WHERE code LIKE 'FLOW-JAVA-%' OR code LIKE 'UI-JAVA-%'
);

DELETE FROM syllabuses WHERE code LIKE 'FLOW-JAVA-%' OR code LIKE 'UI-JAVA-%';

DELETE FROM user_roles
WHERE user_id IN (
    SELECT id FROM users
    WHERE REGEXP_LIKE(email, '^flow\.(classadmin|trainer|trainee)\.[0-9]+@fap\.local$')
       OR email LIKE 'ui.trainee.%@fap.local'
);

DELETE FROM user_avatar_contents
WHERE user_id IN (
    SELECT id FROM users
    WHERE REGEXP_LIKE(email, '^flow\.(classadmin|trainer|trainee)\.[0-9]+@fap\.local$')
       OR email LIKE 'ui.trainee.%@fap.local'
);

DELETE FROM users
WHERE REGEXP_LIKE(email, '^flow\.(classadmin|trainer|trainee)\.[0-9]+@fap\.local$')
   OR email LIKE 'ui.trainee.%@fap.local';

DELETE FROM refresh_tokens
WHERE user_id IN (SELECT id FROM users WHERE REGEXP_LIKE(email, '^e2e\.(superadmin|classadmin0[12]|trainer0[1-3]|trainee(0[1-9]|1[0-9]))@fap\.local$'));

DELETE FROM password_reset_tokens
WHERE user_id IN (SELECT id FROM users WHERE REGEXP_LIKE(email, '^e2e\.(superadmin|classadmin0[12]|trainer0[1-3]|trainee(0[1-9]|1[0-9]))@fap\.local$'));

DELETE FROM audit_logs
WHERE user_id IN (SELECT id FROM users WHERE REGEXP_LIKE(email, '^e2e\.(superadmin|classadmin0[12]|trainer0[1-3]|trainee(0[1-9]|1[0-9]))@fap\.local$'));

DELETE FROM notifications
WHERE user_id IN (SELECT id FROM users WHERE REGEXP_LIKE(email, '^e2e\.(superadmin|classadmin0[12]|trainer0[1-3]|trainee(0[1-9]|1[0-9]))@fap\.local$'));

DELETE FROM quiz_attempts
WHERE quiz_id IN (SELECT id FROM quizzes WHERE title IN (
    '[E2E] Java Backend Readiness',
    '[E2E] Spring Security Draft',
    '[E2E] Archived Fundamentals'
))
OR user_id IN (SELECT id FROM users WHERE REGEXP_LIKE(email, '^e2e\.trainee(0[1-9]|1[0-9])@fap\.local$'));

DELETE FROM quiz_assignments
WHERE quiz_id IN (SELECT id FROM quizzes WHERE title IN (
    '[E2E] Java Backend Readiness',
    '[E2E] Spring Security Draft',
    '[E2E] Archived Fundamentals'
));

DELETE FROM quiz_questions
WHERE quiz_id IN (SELECT id FROM quizzes WHERE title IN (
    '[E2E] Java Backend Readiness',
    '[E2E] Spring Security Draft',
    '[E2E] Archived Fundamentals'
))
OR question_id IN (SELECT id FROM questions WHERE content LIKE '[E2E]%');

DELETE FROM quizzes WHERE title IN (
    '[E2E] Java Backend Readiness',
    '[E2E] Spring Security Draft',
    '[E2E] Archived Fundamentals'
);

DELETE FROM questions WHERE content LIKE '[E2E]%';

DELETE FROM training_feedbacks
WHERE training_id IN (
    SELECT ts.id
    FROM training_sessions ts
    JOIN classes c ON c.id = ts.class_id
    WHERE c.class_code IN (
        'E2E-JAVA-2026-01', 'E2E-JAVA-2026-02', 'E2E-FULLSTACK-2026-01',
        'E2E-JAVA-WEEKEND', 'E2E-JAVA-2025-ALUMNI', 'E2E-X'
    )
);

DELETE FROM attendance_records
WHERE training_id IN (
    SELECT ts.id
    FROM training_sessions ts
    JOIN classes c ON c.id = ts.class_id
    WHERE c.class_code IN (
        'E2E-JAVA-2026-01', 'E2E-JAVA-2026-02', 'E2E-FULLSTACK-2026-01',
        'E2E-JAVA-WEEKEND', 'E2E-JAVA-2025-ALUMNI', 'E2E-X'
    )
);

DELETE FROM training_registrations
WHERE training_id IN (
    SELECT ts.id
    FROM training_sessions ts
    JOIN classes c ON c.id = ts.class_id
    WHERE c.class_code IN (
        'E2E-JAVA-2026-01', 'E2E-JAVA-2026-02', 'E2E-FULLSTACK-2026-01',
        'E2E-JAVA-WEEKEND', 'E2E-JAVA-2025-ALUMNI', 'E2E-X'
    )
);

DELETE FROM training_sessions
WHERE class_id IN (SELECT id FROM classes WHERE class_code IN (
    'E2E-JAVA-2026-01', 'E2E-JAVA-2026-02', 'E2E-FULLSTACK-2026-01',
    'E2E-JAVA-WEEKEND', 'E2E-JAVA-2025-ALUMNI', 'E2E-X'
));

DELETE FROM class_trainers
WHERE class_id IN (SELECT id FROM classes WHERE class_code IN (
    'E2E-JAVA-2026-01', 'E2E-JAVA-2026-02', 'E2E-FULLSTACK-2026-01',
    'E2E-JAVA-WEEKEND', 'E2E-JAVA-2025-ALUMNI', 'E2E-X'
));

DELETE FROM class_admins
WHERE class_id IN (SELECT id FROM classes WHERE class_code IN (
    'E2E-JAVA-2026-01', 'E2E-JAVA-2026-02', 'E2E-FULLSTACK-2026-01',
    'E2E-JAVA-WEEKEND', 'E2E-JAVA-2025-ALUMNI', 'E2E-X'
));

DELETE FROM classes WHERE class_code IN (
    'E2E-JAVA-2026-01', 'E2E-JAVA-2026-02', 'E2E-FULLSTACK-2026-01',
    'E2E-JAVA-WEEKEND', 'E2E-JAVA-2025-ALUMNI', 'E2E-X'
);

DELETE FROM training_program_syllabuses
WHERE program_id IN (SELECT id FROM training_programs WHERE name IN (
    '[E2E] Java Backend Development',
    '[E2E] Full Stack Web Development',
    '[E2E] Python Programming Fundamentals',
    '[E2E] Freshers DevOps Foundation',
    '[E2E] Legacy Orientation Archive'
))
OR syllabus_id IN (SELECT id FROM syllabuses WHERE code IN (
    'E2E-JAVA-CORE', 'E2E-SPRING-API', 'E2E-FULLSTACK', 'E2E-DEVOPS', 'E2E-LEGACY'
));

DELETE FROM training_programs WHERE name IN (
    '[E2E] Java Backend Development',
    '[E2E] Full Stack Web Development',
    '[E2E] Python Programming Fundamentals',
    '[E2E] Freshers DevOps Foundation',
    '[E2E] Legacy Orientation Archive'
);

DELETE FROM material_file_contents
WHERE material_file_id IN (
    SELECT mf.id
    FROM material_files mf
    JOIN syllabus_topics st ON st.id = mf.topic_id
    JOIN syllabus_units su ON su.id = st.unit_id
    JOIN syllabus_days sd ON sd.id = su.day_id
    JOIN syllabuses s ON s.id = sd.syllabus_id
    WHERE s.code IN ('E2E-JAVA-CORE', 'E2E-SPRING-API', 'E2E-FULLSTACK', 'E2E-DEVOPS', 'E2E-LEGACY')
);

DELETE FROM material_files
WHERE topic_id IN (
    SELECT st.id
    FROM syllabus_topics st
    JOIN syllabus_units su ON su.id = st.unit_id
    JOIN syllabus_days sd ON sd.id = su.day_id
    JOIN syllabuses s ON s.id = sd.syllabus_id
    WHERE s.code IN ('E2E-JAVA-CORE', 'E2E-SPRING-API', 'E2E-FULLSTACK', 'E2E-DEVOPS', 'E2E-LEGACY')
);

DELETE FROM syllabus_topics
WHERE unit_id IN (
    SELECT su.id
    FROM syllabus_units su
    JOIN syllabus_days sd ON sd.id = su.day_id
    JOIN syllabuses s ON s.id = sd.syllabus_id
    WHERE s.code IN ('E2E-JAVA-CORE', 'E2E-SPRING-API', 'E2E-FULLSTACK', 'E2E-DEVOPS', 'E2E-LEGACY')
);

DELETE FROM syllabus_units
WHERE day_id IN (
    SELECT sd.id
    FROM syllabus_days sd
    JOIN syllabuses s ON s.id = sd.syllabus_id
    WHERE s.code IN ('E2E-JAVA-CORE', 'E2E-SPRING-API', 'E2E-FULLSTACK', 'E2E-DEVOPS', 'E2E-LEGACY')
);

DELETE FROM syllabus_days
WHERE syllabus_id IN (SELECT id FROM syllabuses WHERE code IN (
    'E2E-JAVA-CORE', 'E2E-SPRING-API', 'E2E-FULLSTACK', 'E2E-DEVOPS', 'E2E-LEGACY'
));

DELETE FROM syllabus_output_standards
WHERE syllabus_id IN (SELECT id FROM syllabuses WHERE code IN (
    'E2E-JAVA-CORE', 'E2E-SPRING-API', 'E2E-FULLSTACK', 'E2E-DEVOPS', 'E2E-LEGACY'
));

DELETE FROM syllabuses WHERE code IN (
    'E2E-JAVA-CORE', 'E2E-SPRING-API', 'E2E-FULLSTACK', 'E2E-DEVOPS', 'E2E-LEGACY'
);

DELETE FROM user_roles
WHERE user_id IN (SELECT id FROM users WHERE REGEXP_LIKE(email, '^e2e\.(superadmin|classadmin0[12]|trainer0[1-3]|trainee(0[1-9]|1[0-9]))@fap\.local$'));

DELETE FROM user_avatar_contents
WHERE user_id IN (SELECT id FROM users WHERE REGEXP_LIKE(email, '^e2e\.(superadmin|classadmin0[12]|trainer0[1-3]|trainee(0[1-9]|1[0-9]))@fap\.local$'));

DELETE FROM users
WHERE REGEXP_LIKE(email, '^e2e\.(superadmin|classadmin0[12]|trainer0[1-3]|trainee(0[1-9]|1[0-9]))@fap\.local$');

DELETE FROM system_settings WHERE category = 'DEV_SEED';

-- Let Flyway apply the repeatable seed again on the next local startup.
DELETE FROM "flyway_schema_history" WHERE "script" = 'R__seed_e2e_test_data.sql';

COMMIT;
PROMPT E2E dev dataset removed successfully.
EXIT SUCCESS;
