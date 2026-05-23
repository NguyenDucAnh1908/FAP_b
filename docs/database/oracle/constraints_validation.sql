-- Constraint/index validation queries for Oracle.
-- Run after Flyway or Liquibase migration to inspect generated database objects.

SELECT
    table_name,
    constraint_name,
    constraint_type,
    status,
    search_condition_vc
FROM user_constraints
WHERE table_name IN (
    'USERS', 'ROLES', 'USER_ROLES', 'PERMISSIONS',
    'SYLLABUSES', 'SYLLABUS_OUTPUT_STANDARDS', 'SYLLABUS_DAYS', 'SYLLABUS_UNITS',
    'SYLLABUS_TOPICS', 'MATERIAL_FILES',
    'TRAINING_PROGRAMS', 'TRAINING_PROGRAM_SYLLABUSES', 'CLASSES', 'CLASS_TRAINERS', 'CLASS_ADMINS',
    'QUESTIONS', 'QUIZZES', 'QUIZ_QUESTIONS', 'QUIZ_ASSIGNMENTS', 'QUIZ_ATTEMPTS',
    'TRAINING_SESSIONS', 'TRAINING_REGISTRATIONS', 'ATTENDANCE_RECORDS',
    'NOTIFICATIONS', 'AUDIT_LOGS', 'SYSTEM_SETTINGS', 'REFRESH_TOKENS'
)
ORDER BY table_name, constraint_type, constraint_name;

SELECT
    c.table_name,
    c.constraint_name,
    c.constraint_type,
    cc.column_name,
    cc.position
FROM user_constraints c
JOIN user_cons_columns cc
    ON cc.constraint_name = c.constraint_name
WHERE c.table_name IN (
    'USERS', 'ROLES', 'USER_ROLES', 'PERMISSIONS',
    'SYLLABUSES', 'SYLLABUS_OUTPUT_STANDARDS', 'SYLLABUS_DAYS', 'SYLLABUS_UNITS',
    'SYLLABUS_TOPICS', 'MATERIAL_FILES',
    'TRAINING_PROGRAMS', 'TRAINING_PROGRAM_SYLLABUSES', 'CLASSES', 'CLASS_TRAINERS', 'CLASS_ADMINS',
    'QUESTIONS', 'QUIZZES', 'QUIZ_QUESTIONS', 'QUIZ_ASSIGNMENTS', 'QUIZ_ATTEMPTS',
    'TRAINING_SESSIONS', 'TRAINING_REGISTRATIONS', 'ATTENDANCE_RECORDS',
    'NOTIFICATIONS', 'AUDIT_LOGS', 'SYSTEM_SETTINGS', 'REFRESH_TOKENS'
)
ORDER BY c.table_name, c.constraint_name, cc.position;

SELECT
    table_name,
    index_name,
    uniqueness,
    index_type,
    status
FROM user_indexes
WHERE table_name IN (
    'USERS', 'ROLES', 'USER_ROLES', 'PERMISSIONS',
    'SYLLABUSES', 'SYLLABUS_OUTPUT_STANDARDS', 'SYLLABUS_DAYS', 'SYLLABUS_UNITS',
    'SYLLABUS_TOPICS', 'MATERIAL_FILES',
    'TRAINING_PROGRAMS', 'TRAINING_PROGRAM_SYLLABUSES', 'CLASSES', 'CLASS_TRAINERS', 'CLASS_ADMINS',
    'QUESTIONS', 'QUIZZES', 'QUIZ_QUESTIONS', 'QUIZ_ASSIGNMENTS', 'QUIZ_ATTEMPTS',
    'TRAINING_SESSIONS', 'TRAINING_REGISTRATIONS', 'ATTENDANCE_RECORDS',
    'NOTIFICATIONS', 'AUDIT_LOGS', 'SYSTEM_SETTINGS', 'REFRESH_TOKENS'
)
ORDER BY table_name, index_name;
