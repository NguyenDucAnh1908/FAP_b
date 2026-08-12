-- LOCAL/DEV ONLY: coherent data for manual UI, API Center, and end-to-end checks.
-- Every section is keyed by a stable email/code/title and is safe to run repeatedly.
-- Password for all e2e.* accounts: Password@123

-- =====================================================
-- 1. TEST ACCOUNTS AND ROLES
-- =====================================================
MERGE INTO users target
USING (
    SELECT 'E2E Super Admin' full_name, 'e2e.superadmin@fap.local' email, '0902000000' phone, DATE '1985-01-01' date_of_birth, 'Male' gender, 'Active' status FROM dual UNION ALL
    SELECT 'Nguyễn Minh An' full_name, 'e2e.classadmin01@fap.local' email, '0902000001' phone, DATE '1990-02-15' date_of_birth, 'Male' gender, 'Active' status FROM dual UNION ALL
    SELECT 'Trần Thu Hà', 'e2e.classadmin02@fap.local', '0902000002', DATE '1991-07-20', 'Female', 'Active' FROM dual UNION ALL
    SELECT 'Lê Hoàng Nam', 'e2e.trainer01@fap.local', '0902000011', DATE '1988-03-12', 'Male', 'Active' FROM dual UNION ALL
    SELECT 'Phạm Khánh Linh', 'e2e.trainer02@fap.local', '0902000012', DATE '1989-11-08', 'Female', 'Active' FROM dual UNION ALL
    SELECT 'Đỗ Quang Huy', 'e2e.trainer03@fap.local', '0902000013', DATE '1987-05-24', 'Male', 'Active' FROM dual UNION ALL
    SELECT 'Võ Hải Anh', 'e2e.trainee01@fap.local', '0902000101', DATE '2001-01-10', 'Female', 'Active' FROM dual UNION ALL
    SELECT 'Nguyễn Đức Bình', 'e2e.trainee02@fap.local', '0902000102', DATE '2000-02-11', 'Male', 'Active' FROM dual UNION ALL
    SELECT 'Trần Gia Chi', 'e2e.trainee03@fap.local', '0902000103', DATE '2002-03-12', 'Female', 'Active' FROM dual UNION ALL
    SELECT 'Lê Minh Dũng', 'e2e.trainee04@fap.local', '0902000104', DATE '2001-04-13', 'Male', 'Active' FROM dual UNION ALL
    SELECT 'Phạm Ngọc Hân', 'e2e.trainee05@fap.local', '0902000105', DATE '2000-05-14', 'Female', 'Active' FROM dual UNION ALL
    SELECT 'Hoàng Tuấn Kiệt', 'e2e.trainee06@fap.local', '0902000106', DATE '2002-06-15', 'Male', 'Active' FROM dual UNION ALL
    SELECT 'Bùi Thảo Lam', 'e2e.trainee07@fap.local', '0902000107', DATE '2001-07-16', 'Female', 'Active' FROM dual UNION ALL
    SELECT 'Đặng Quốc Minh', 'e2e.trainee08@fap.local', '0902000108', DATE '2000-08-17', 'Male', 'Active' FROM dual UNION ALL
    SELECT 'Hồ Yến Nhi', 'e2e.trainee09@fap.local', '0902000109', DATE '2002-09-18', 'Female', 'Active' FROM dual UNION ALL
    SELECT 'Vũ Thành Phúc', 'e2e.trainee10@fap.local', '0902000110', DATE '2001-10-19', 'Male', 'Active' FROM dual UNION ALL
    SELECT 'Đinh Mai Quỳnh', 'e2e.trainee11@fap.local', '0902000111', DATE '2000-11-20', 'Female', 'Active' FROM dual UNION ALL
    SELECT 'Tài khoản học viên tạm ngưng để kiểm tra bộ lọc trạng thái', 'e2e.trainee12@fap.local', NULL, DATE '2002-12-21', 'Male', 'Inactive' FROM dual UNION ALL
    SELECT 'An', 'e2e.trainee13@fap.local', '0902000113', DATE '2001-03-22', 'Male', 'Active' FROM dual UNION ALL
    SELECT 'Nguyễn Văn Thành Long', 'e2e.trainee14@fap.local', '0902000114', DATE '1999-04-23', 'Male', 'Active' FROM dual UNION ALL
    SELECT 'Alex Morgan', 'e2e.trainee15@fap.local', '0902000115', DATE '2000-05-24', 'Female', 'Active' FROM dual UNION ALL
    SELECT 'Lương Thị Hồng Phúc', 'e2e.trainee16@fap.local', '0902000116', DATE '2002-06-25', 'Female', 'Active' FROM dual UNION ALL
    SELECT 'Mai Anh', 'e2e.trainee17@fap.local', NULL, DATE '2001-07-26', 'Female', 'Active' FROM dual UNION ALL
    SELECT 'Christopher Nguyễn Hoàng Alexander', 'e2e.trainee18@fap.local', '0902000118', DATE '1998-08-27', 'Male', 'Active' FROM dual UNION ALL
    SELECT 'Trương Quốc Bảo', 'e2e.trainee19@fap.local', '0902000119', DATE '2000-09-28', 'Male', 'Active' FROM dual
) source
ON (LOWER(target.email) = LOWER(source.email))
WHEN MATCHED THEN UPDATE SET
    target.full_name = source.full_name,
    target.phone = source.phone,
    target.password_hash = '$2a$10$hBEgsOeLpTHqe3yH7/DYoubHuWKQx79Vec5vBwNnc/cNHoYmGFPU2',
    target.date_of_birth = source.date_of_birth,
    target.gender = source.gender,
    target.status = source.status,
    target.is_deleted = 0,
    target.deleted_at = NULL,
    target.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    id, full_name, email, phone, password_hash, date_of_birth, gender, status,
    is_deleted, version_no, created_at, updated_at
) VALUES (
    users_seq.NEXTVAL, source.full_name, source.email, source.phone,
    '$2a$10$hBEgsOeLpTHqe3yH7/DYoubHuWKQx79Vec5vBwNnc/cNHoYmGFPU2',
    source.date_of_birth, source.gender, source.status, 0, 0,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

MERGE INTO user_roles target
USING (
    SELECT u.id user_id,
           r.id role_id
    FROM users u
    JOIN roles r ON r.name = CASE
        WHEN u.email = 'e2e.superadmin@fap.local' THEN 'Super Admin'
        WHEN u.email LIKE 'e2e.classadmin%' THEN 'Class Admin'
        WHEN u.email LIKE 'e2e.trainer%' THEN 'Trainer'
        ELSE 'Trainee'
    END
    WHERE u.email LIKE 'e2e.%@fap.local'
) source
ON (target.user_id = source.user_id AND target.role_id = source.role_id)
WHEN NOT MATCHED THEN INSERT (user_id, role_id)
VALUES (source.user_id, source.role_id);

-- =====================================================
-- 2. SYLLABUS GENERAL INFO, OUTLINE, OUTPUT STANDARDS
-- =====================================================
MERGE INTO syllabuses target
USING (
    SELECT '[E2E] Java Core Programming' name, 'E2E-JAVA-CORE' code, 'v1.0' version, 'Active' status,
           'Beginner' level_name, 30 attendees, '40 hours' duration,
           'JDK 21, IntelliJ IDEA, Git' technical_requirements,
           'Build a maintainable Java application using OOP and collections.' course_objectives,
           'Attendance >= 80%; final score >= 60.' rules,
           50 assignment_lab, 30 concept_lecture, 10 guide_review, 10 test_quiz,
           15 quiz_pct, 25 assignment_pct, 60 final_pct,
           'Quiz 15%; Assignment 25%; Final exam 60%.' assessment_text FROM dual UNION ALL
    SELECT '[E2E] Spring Boot REST API', 'E2E-SPRING-API', 'v1.0', 'Active',
           'Intermediate', 24, '48 hours', 'Java 21, Maven, Oracle XE, Postman',
           'Design, secure, test, and document Spring Boot REST APIs.',
           'Complete all labs and obtain at least 70% overall.',
           45, 35, 10, 10, 20, 30, 50,
           'Quiz 20%; Assignment 30%; Final API project 50%.' FROM dual UNION ALL
    SELECT '[E2E] Full Stack Web Development', 'E2E-FULLSTACK', 'v2.0', 'Pending',
           'Intermediate', 28, '80 hours', 'Node.js, React, Java 21, Docker',
           'Deliver an end-to-end web application with authentication.',
           'Code review is required before submission.',
           55, 25, 10, 10, 15, 35, 50,
           'Quiz 15%; Team assignment 35%; Final product 50%.' FROM dual UNION ALL
    SELECT '[E2E] Freshers DevOps Foundation', 'E2E-DEVOPS', 'v1.0', 'Drafting',
           'Beginner', 32, '32 hours', 'Git, Docker Desktop',
           'Understand source control, containers, and a basic CI pipeline.',
           NULL, 60, 20, 10, 10, 20, 40, 40,
           NULL FROM dual UNION ALL
    SELECT '[E2E] Chuyên đề cũ ngừng sử dụng', 'E2E-LEGACY', 'v0.9', 'Inactive',
           'All levels', 20, NULL, NULL,
           'Dữ liệu tiếng Việt có dấu dùng để kiểm tra hiển thị và tìm kiếm.',
           NULL, 50, 30, 10, 10, 15, 15, 70,
           NULL FROM dual
) source
ON (UPPER(target.code) = source.code)
WHEN MATCHED THEN UPDATE SET
    target.name = source.name,
    target.version = source.version,
    target.status = source.status,
    target.level_name = source.level_name,
    target.attendees = source.attendees,
    target.duration = source.duration,
    target.technical_requirements = source.technical_requirements,
    target.course_objectives = source.course_objectives,
    target.rules = source.rules,
    target.time_alloc_assignment_lab = source.assignment_lab,
    target.time_alloc_concept_lecture = source.concept_lecture,
    target.time_alloc_guide_review = source.guide_review,
    target.time_alloc_test_quiz = source.test_quiz,
    target.assess_quiz_pct = source.quiz_pct,
    target.assess_assignment_pct = source.assignment_pct,
    target.assess_final_pct = source.final_pct,
    target.assessment_text = source.assessment_text,
    target.is_deleted = 0,
    target.deleted_at = NULL,
    target.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    id, name, code, version, status, level_name, attendees, duration,
    technical_requirements, course_objectives, rules,
    time_alloc_assignment_lab, time_alloc_concept_lecture,
    time_alloc_guide_review, time_alloc_test_quiz,
    assess_quiz_pct, assess_assignment_pct, assess_final_pct,
    assessment_text, is_deleted, version_no, created_at, updated_at, created_by, updated_by
) VALUES (
    syllabuses_seq.NEXTVAL, source.name, source.code, source.version, source.status,
    source.level_name, source.attendees, source.duration, source.technical_requirements,
    source.course_objectives, source.rules, source.assignment_lab, source.concept_lecture,
    source.guide_review, source.test_quiz, source.quiz_pct, source.assignment_pct,
    source.final_pct, source.assessment_text, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local'),
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local')
);

MERGE INTO syllabus_output_standards target
USING (
    SELECT s.id syllabus_id, standards.standard_code
    FROM syllabuses s
    JOIN (
        SELECT 'E2E-JAVA-CORE' code, 'H4SD' standard_code FROM dual UNION ALL
        SELECT 'E2E-JAVA-CORE', 'K6SD' FROM dual UNION ALL
        SELECT 'E2E-SPRING-API', 'H4SD' FROM dual UNION ALL
        SELECT 'E2E-SPRING-API', 'H1SD' FROM dual UNION ALL
        SELECT 'E2E-FULLSTACK', 'H4SD' FROM dual UNION ALL
        SELECT 'E2E-FULLSTACK', 'C3SD' FROM dual UNION ALL
        SELECT 'E2E-DEVOPS', 'H2SD' FROM dual UNION ALL
        SELECT 'E2E-LEGACY', 'K6SD' FROM dual
    ) standards ON standards.code = s.code
) source
ON (target.syllabus_id = source.syllabus_id AND target.standard_code = source.standard_code)
WHEN NOT MATCHED THEN INSERT (syllabus_id, standard_code)
VALUES (source.syllabus_id, source.standard_code);

MERGE INTO syllabus_days target
USING (
    SELECT s.id syllabus_id, days.day_number, days.sort_order
    FROM syllabuses s
    JOIN (
        SELECT 'E2E-JAVA-CORE' code, 1 day_number, 1 sort_order FROM dual UNION ALL
        SELECT 'E2E-JAVA-CORE', 2, 2 FROM dual UNION ALL
        SELECT 'E2E-SPRING-API', 1, 1 FROM dual UNION ALL
        SELECT 'E2E-SPRING-API', 2, 2 FROM dual UNION ALL
        SELECT 'E2E-FULLSTACK', 1, 1 FROM dual UNION ALL
        SELECT 'E2E-DEVOPS', 1, 1 FROM dual UNION ALL
        SELECT 'E2E-LEGACY', 1, 1 FROM dual
    ) days ON days.code = s.code
) source
ON (target.syllabus_id = source.syllabus_id AND target.day_number = source.day_number)
WHEN MATCHED THEN UPDATE SET target.sort_order = source.sort_order
WHEN NOT MATCHED THEN INSERT (id, syllabus_id, day_number, sort_order)
VALUES (syllabus_days_seq.NEXTVAL, source.syllabus_id, source.day_number, source.sort_order);

MERGE INTO syllabus_units target
USING (
    SELECT d.id day_id, units.name, units.sort_order
    FROM syllabus_days d
    JOIN syllabuses s ON s.id = d.syllabus_id
    JOIN (
        SELECT 'E2E-JAVA-CORE' code, 1 day_number, 1 sort_order, 'Java language foundations' name FROM dual UNION ALL
        SELECT 'E2E-JAVA-CORE', 1, 2, 'Object-oriented programming' FROM dual UNION ALL
        SELECT 'E2E-JAVA-CORE', 2, 1, 'Collections and error handling' FROM dual UNION ALL
        SELECT 'E2E-SPRING-API', 1, 1, 'Spring Boot REST foundations' FROM dual UNION ALL
        SELECT 'E2E-SPRING-API', 2, 1, 'Security and API testing' FROM dual UNION ALL
        SELECT 'E2E-FULLSTACK', 1, 1, 'Frontend and backend integration' FROM dual UNION ALL
        SELECT 'E2E-DEVOPS', 1, 1, 'Container and CI basics' FROM dual UNION ALL
        SELECT 'E2E-LEGACY', 1, 1, 'Nội dung lưu trữ' FROM dual
    ) units ON units.code = s.code AND units.day_number = d.day_number
) source
ON (target.day_id = source.day_id AND target.sort_order = source.sort_order)
WHEN MATCHED THEN UPDATE SET target.name = source.name
WHEN NOT MATCHED THEN INSERT (id, day_id, name, sort_order)
VALUES (syllabus_units_seq.NEXTVAL, source.day_id, source.name, source.sort_order);

MERGE INTO syllabus_topics target
USING (
    SELECT u.id unit_id, topics.name, topics.output_standard, topics.is_online,
           topics.duration_minutes, topics.status, topics.sort_order
    FROM syllabus_units u
    JOIN syllabus_days d ON d.id = u.day_id
    JOIN syllabuses s ON s.id = d.syllabus_id
    JOIN (
        SELECT 'E2E-JAVA-CORE' code, 1 day_number, 1 unit_order, 1 sort_order,
               'JDK setup and first Java program' name, 'H4SD' output_standard, 0 is_online, 60 duration_minutes, 'Active' status FROM dual UNION ALL
        SELECT 'E2E-JAVA-CORE', 1, 1, 2, 'Variables, control flow, and methods', 'K6SD', 1, 90, 'Active' FROM dual UNION ALL
        SELECT 'E2E-JAVA-CORE', 1, 2, 1, 'Classes, objects, and encapsulation', 'H4SD', 0, 120, 'Active' FROM dual UNION ALL
        SELECT 'E2E-JAVA-CORE', 2, 1, 1, 'Collections framework', 'K6SD', 1, 90, 'Active' FROM dual UNION ALL
        SELECT 'E2E-JAVA-CORE', 2, 1, 2, 'Exception handling', 'H4SD', 1, 60, 'Inactive' FROM dual UNION ALL
        SELECT 'E2E-SPRING-API', 1, 1, 1, 'REST controller and validation', 'H4SD', 1, 120, 'Active' FROM dual UNION ALL
        SELECT 'E2E-SPRING-API', 2, 1, 1, 'JWT authentication and authorization', 'H1SD', 1, 120, 'Active' FROM dual UNION ALL
        SELECT 'E2E-FULLSTACK', 1, 1, 1, 'React consumes a Spring Boot API', 'C3SD', 1, 150, 'Active' FROM dual UNION ALL
        SELECT 'E2E-DEVOPS', 1, 1, 1, 'Build and run with Docker', 'H2SD', 1, 90, 'Active' FROM dual UNION ALL
        SELECT 'E2E-LEGACY', 1, 1, 1, 'Chủ đề đã ngừng sử dụng', 'K6SD', 0, 30, 'Inactive' FROM dual
    ) topics ON topics.code = s.code
            AND topics.day_number = d.day_number
            AND topics.unit_order = u.sort_order
) source
ON (target.unit_id = source.unit_id AND target.sort_order = source.sort_order)
WHEN MATCHED THEN UPDATE SET
    target.name = source.name,
    target.output_standard = source.output_standard,
    target.is_online = source.is_online,
    target.duration_minutes = source.duration_minutes,
    target.status = source.status
WHEN NOT MATCHED THEN INSERT (
    id, unit_id, name, output_standard, is_online, duration_minutes, status, sort_order
) VALUES (
    syllabus_topics_seq.NEXTVAL, source.unit_id, source.name, source.output_standard,
    source.is_online, source.duration_minutes, source.status, source.sort_order
);

MERGE INTO material_files target
USING (
    SELECT t.id topic_id, materials.file_name, materials.file_url,
           materials.file_size, materials.content_type
    FROM syllabus_topics t
    JOIN syllabus_units u ON u.id = t.unit_id
    JOIN syllabus_days d ON d.id = u.day_id
    JOIN syllabuses s ON s.id = d.syllabus_id
    JOIN (
        SELECT 'E2E-JAVA-CORE' code, 1 day_number, 1 unit_order, 1 topic_order,
               'Java Coding Convention.pdf' file_name,
               'https://www.oracle.com/java/technologies/javase/codeconventions-contents.html' file_url,
               24576 file_size, 'application/pdf' content_type FROM dual UNION ALL
        SELECT 'E2E-SPRING-API', 1, 1, 1,
               'Spring REST Reference.pdf',
               'https://docs.spring.io/spring-framework/reference/web/webmvc.html',
               32768, 'application/pdf' FROM dual
    ) materials ON materials.code = s.code
               AND materials.day_number = d.day_number
               AND materials.unit_order = u.sort_order
               AND materials.topic_order = t.sort_order
) source
ON (target.topic_id = source.topic_id AND target.file_name = source.file_name)
WHEN MATCHED THEN UPDATE SET
    target.file_url = source.file_url,
    target.file_size = source.file_size,
    target.content_type = source.content_type
WHEN NOT MATCHED THEN INSERT (
    id, topic_id, file_name, file_url, file_size, content_type, uploaded_by, uploaded_at
) VALUES (
    material_files_seq.NEXTVAL, source.topic_id, source.file_name, source.file_url,
    source.file_size, source.content_type,
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local'), CURRENT_TIMESTAMP
);

-- =====================================================
-- 3. TRAINING PROGRAMS AND CLASSES
-- =====================================================
MERGE INTO training_programs target
USING (
    SELECT '[E2E] Java Backend Development' name, 'Active' status, '12 weeks' duration, 240 total_hours, 'v1.0' version FROM dual UNION ALL
    SELECT '[E2E] Full Stack Web Development', 'Active', '16 weeks', 320, 'v1.0' FROM dual UNION ALL
    SELECT '[E2E] Python Programming Fundamentals', 'Planning', '8 weeks', 160, 'v1.0' FROM dual UNION ALL
    SELECT '[E2E] Freshers DevOps Foundation', 'Planning', '6 weeks', 120, 'v1.0' FROM dual UNION ALL
    SELECT '[E2E] Legacy Orientation Archive', 'Inactive', NULL, 0, 'v0.9' FROM dual
) source
ON (LOWER(target.name) = LOWER(source.name))
WHEN MATCHED THEN UPDATE SET
    target.status = source.status,
    target.duration = source.duration,
    target.total_hours = source.total_hours,
    target.version = source.version,
    target.is_deleted = 0,
    target.deleted_at = NULL,
    target.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    id, name, status, duration, total_hours, version, is_deleted, version_no,
    created_at, updated_at, created_by, updated_by
) VALUES (
    training_programs_seq.NEXTVAL, source.name, source.status, source.duration,
    source.total_hours, source.version, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local'),
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local')
);

MERGE INTO training_program_syllabuses target
USING (
    SELECT p.id program_id, s.id syllabus_id, links.sort_order
    FROM training_programs p
    JOIN (
        SELECT '[E2E] Java Backend Development' program_name, 'E2E-JAVA-CORE' syllabus_code, 1 sort_order FROM dual UNION ALL
        SELECT '[E2E] Java Backend Development', 'E2E-SPRING-API', 2 FROM dual UNION ALL
        SELECT '[E2E] Full Stack Web Development', 'E2E-SPRING-API', 1 FROM dual
    ) links ON links.program_name = p.name
    JOIN syllabuses s ON s.code = links.syllabus_code
) source
ON (target.program_id = source.program_id AND target.syllabus_id = source.syllabus_id)
WHEN MATCHED THEN UPDATE SET target.sort_order = source.sort_order
WHEN NOT MATCHED THEN INSERT (program_id, syllabus_id, sort_order)
VALUES (source.program_id, source.syllabus_id, source.sort_order);

MERGE INTO classes target
USING (
    SELECT class_data.*, program.id training_program_id
    FROM (
    SELECT '[E2E] Java Backend 2026 - Cohort 01' name, 'E2E-JAVA-2026-01' class_code,
           '[E2E] Java Backend Development' program_name, 'Active' status,
           'Hồ Chí Minh' location, 'F-Town 3, Room A101' location_detail,
           'FHM' fsu, '08:30 - 12:00' class_time,
           TRUNC(SYSDATE) - 30 start_date, TRUNC(SYSDATE) + 120 end_date, '12 weeks' duration FROM dual UNION ALL
    SELECT '[E2E] Java Backend 2026 - Cohort 02', 'E2E-JAVA-2026-02',
           '[E2E] Java Backend Development', 'Planning',
           'Hà Nội', 'F-Ville 2, Room B202', 'FHN', '13:30 - 17:00',
           TRUNC(SYSDATE) + 30, TRUNC(SYSDATE) + 150, '12 weeks' FROM dual UNION ALL
    SELECT '[E2E] Full Stack Web 2026 - Cohort 01', 'E2E-FULLSTACK-2026-01',
           '[E2E] Full Stack Web Development', 'Active',
           'Đà Nẵng', 'F-Complex, Room C303', 'FDN', '18:00 - 21:00',
           TRUNC(SYSDATE) - 15, TRUNC(SYSDATE) + 150, '16 weeks' FROM dual UNION ALL
    SELECT '[E2E] Java Weekend Class', 'E2E-JAVA-WEEKEND',
           '[E2E] Java Backend Development', 'Active',
           'Online', NULL, 'FHM', 'Saturday 08:00 - 16:00',
           TRUNC(SYSDATE) - 7, TRUNC(SYSDATE) + 90, '10 weeks' FROM dual UNION ALL
    SELECT '[E2E] Java Alumni 2025', 'E2E-JAVA-2025-ALUMNI',
           '[E2E] Java Backend Development', 'Closed',
           'Hồ Chí Minh', NULL, 'FHM', '08:30 - 12:00',
           TRUNC(SYSDATE) - 240, TRUNC(SYSDATE) - 60, '12 weeks' FROM dual UNION ALL
    SELECT '[E2E] Lớp tên ngắn', 'E2E-X',
           '[E2E] Java Backend Development', 'Planning',
           NULL, NULL, NULL, NULL,
           TRUNC(SYSDATE) + 45, TRUNC(SYSDATE) + 90, NULL FROM dual
) class_data
    JOIN training_programs program ON program.name = class_data.program_name
) source
ON (target.class_code = source.class_code)
WHEN MATCHED THEN UPDATE SET
    target.name = source.name,
    target.training_program_id = source.training_program_id,
    target.status = source.status,
    target.location = source.location,
    target.location_detail = source.location_detail,
    target.fsu = source.fsu,
    target.class_time = source.class_time,
    target.start_date = source.start_date,
    target.end_date = source.end_date,
    target.duration = source.duration,
    target.is_deleted = 0,
    target.deleted_at = NULL,
    target.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    id, name, class_code, training_program_id, status, location, location_detail,
    fsu, class_time, start_date, end_date, duration, is_deleted, version_no,
    created_at, updated_at, created_by, updated_by
) VALUES (
    classes_seq.NEXTVAL, source.name, source.class_code, source.training_program_id, source.status,
    source.location, source.location_detail, source.fsu, source.class_time,
    source.start_date, source.end_date, source.duration, 0, 0,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local'),
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local')
);

MERGE INTO class_admins target
USING (
    SELECT c.id class_id, u.id user_id
    FROM classes c
    JOIN (
        SELECT 'E2E-JAVA-2026-01' class_code, 'e2e.classadmin01@fap.local' email FROM dual UNION ALL
        SELECT 'E2E-JAVA-2026-02', 'e2e.classadmin01@fap.local' FROM dual UNION ALL
        SELECT 'E2E-FULLSTACK-2026-01', 'e2e.classadmin02@fap.local' FROM dual UNION ALL
        SELECT 'E2E-JAVA-WEEKEND', 'e2e.classadmin02@fap.local' FROM dual UNION ALL
        SELECT 'E2E-JAVA-2025-ALUMNI', 'e2e.classadmin01@fap.local' FROM dual UNION ALL
        SELECT 'E2E-X', 'e2e.classadmin02@fap.local' FROM dual
    ) assignments ON assignments.class_code = c.class_code
    JOIN users u ON LOWER(u.email) = assignments.email
) source
ON (target.class_id = source.class_id AND target.user_id = source.user_id)
WHEN NOT MATCHED THEN INSERT (class_id, user_id)
VALUES (source.class_id, source.user_id);

MERGE INTO class_trainers target
USING (
    SELECT c.id class_id, u.id user_id, s.id syllabus_id
    FROM classes c
    JOIN (
        SELECT 'E2E-JAVA-2026-01' class_code, 'e2e.trainer01@fap.local' email, 'E2E-JAVA-CORE' syllabus_code FROM dual UNION ALL
        SELECT 'E2E-JAVA-2026-01', 'e2e.trainer02@fap.local', 'E2E-SPRING-API' FROM dual UNION ALL
        SELECT 'E2E-JAVA-2026-02', 'e2e.trainer01@fap.local', 'E2E-JAVA-CORE' FROM dual UNION ALL
        SELECT 'E2E-FULLSTACK-2026-01', 'e2e.trainer02@fap.local', 'E2E-SPRING-API' FROM dual UNION ALL
        SELECT 'E2E-JAVA-WEEKEND', 'e2e.trainer03@fap.local', 'E2E-JAVA-CORE' FROM dual UNION ALL
        SELECT 'E2E-JAVA-2025-ALUMNI', 'e2e.trainer01@fap.local', 'E2E-JAVA-CORE' FROM dual UNION ALL
        SELECT 'E2E-X', 'e2e.trainer03@fap.local', 'E2E-JAVA-CORE' FROM dual
    ) assignments ON assignments.class_code = c.class_code
    JOIN users u ON LOWER(u.email) = assignments.email
    JOIN syllabuses s ON s.code = assignments.syllabus_code
) source
ON (target.class_id = source.class_id
    AND target.user_id = source.user_id
    AND target.syllabus_id = source.syllabus_id)
WHEN NOT MATCHED THEN INSERT (id, class_id, user_id, syllabus_id)
VALUES (class_trainers_seq.NEXTVAL, source.class_id, source.user_id, source.syllabus_id);

-- =====================================================
-- 4. TRAINING SESSIONS, REGISTRATIONS, ATTENDANCE, FEEDBACK
-- =====================================================
MERGE INTO training_sessions target
USING (
    SELECT c.id class_id, u.id trainer_id,
           'E2E Java Kickoff Completed' title,
           'Completed session with attendance and feedback.' description,
           'A101' room, TRUNC(SYSDATE) - 14 session_date,
           CAST(TRUNC(SYSDATE) - 14 AS TIMESTAMP) + INTERVAL '8' HOUR start_time,
           CAST(TRUNC(SYSDATE) - 14 AS TIMESTAMP) + INTERVAL '11' HOUR end_time,
           'Offline' session_type, NULL meeting_link,
           20 capacity, 3 enrolled_count, 'Completed' status
    FROM classes c JOIN users u ON LOWER(u.email) = 'e2e.trainer01@fap.local'
    WHERE c.class_code = 'E2E-JAVA-2026-01' UNION ALL
    SELECT c.id, u.id, 'E2E Spring REST Workshop',
           'Small-capacity session used to verify registered and waitlist flows.',
           'B202', TRUNC(SYSDATE) + 7,
           CAST(TRUNC(SYSDATE) + 7 AS TIMESTAMP) + INTERVAL '9' HOUR,
           CAST(TRUNC(SYSDATE) + 7 AS TIMESTAMP) + INTERVAL '12' HOUR,
           'Hybrid', 'https://meet.example.com/e2e-spring-rest', 3, 3, 'Upcoming'
    FROM classes c JOIN users u ON LOWER(u.email) = 'e2e.trainer02@fap.local'
    WHERE c.class_code = 'E2E-JAVA-2026-01' UNION ALL
    SELECT c.id, u.id, 'E2E Java Security Lab',
           'Open session for manual registration and QR check-in tests.',
           NULL, TRUNC(SYSDATE) + 14,
           CAST(TRUNC(SYSDATE) + 14 AS TIMESTAMP) + INTERVAL '13' HOUR,
           CAST(TRUNC(SYSDATE) + 14 AS TIMESTAMP) + INTERVAL '16' HOUR,
           'Online', 'https://meet.example.com/e2e-java-security', 20, 1, 'Upcoming'
    FROM classes c JOIN users u ON LOWER(u.email) = 'e2e.trainer01@fap.local'
    WHERE c.class_code = 'E2E-JAVA-2026-01' UNION ALL
    SELECT c.id, u.id, 'E2E Full Stack React Lab',
           'Frontend consumes the backend API.',
           'C303', TRUNC(SYSDATE) + 10,
           CAST(TRUNC(SYSDATE) + 10 AS TIMESTAMP) + INTERVAL '18' HOUR,
           CAST(TRUNC(SYSDATE) + 10 AS TIMESTAMP) + INTERVAL '21' HOUR,
           'Offline', NULL, 10, 2, 'Upcoming'
    FROM classes c JOIN users u ON LOWER(u.email) = 'e2e.trainer02@fap.local'
    WHERE c.class_code = 'E2E-FULLSTACK-2026-01' UNION ALL
    SELECT c.id, u.id, 'E2E Hybrid Architecture Clinic',
           'Future hybrid session used for date-range filters.',
           'Virtual Room 01', TRUNC(SYSDATE) + 21,
           CAST(TRUNC(SYSDATE) + 21 AS TIMESTAMP) + INTERVAL '8' HOUR,
           CAST(TRUNC(SYSDATE) + 21 AS TIMESTAMP) + INTERVAL '10' HOUR,
           'Hybrid', 'https://meet.example.com/e2e-architecture', 15, 0, 'Upcoming'
    FROM classes c JOIN users u ON LOWER(u.email) = 'e2e.trainer03@fap.local'
    WHERE c.class_code = 'E2E-JAVA-WEEKEND' UNION ALL
    SELECT c.id, u.id, 'E2E REST API Integration Clinic',
           'Hands-on integration session for API pagination, filtering, and error handling.',
           'A104', TRUNC(SYSDATE) + 28,
           CAST(TRUNC(SYSDATE) + 28 AS TIMESTAMP) + INTERVAL '8' HOUR,
           CAST(TRUNC(SYSDATE) + 28 AS TIMESTAMP) + INTERVAL '11' HOUR,
           'Offline', NULL, 20, 0, 'Upcoming'
    FROM classes c JOIN users u ON LOWER(u.email) = 'e2e.trainer02@fap.local'
    WHERE c.class_code = 'E2E-JAVA-2026-01' UNION ALL
    SELECT c.id, u.id, 'E2E React State Management Workshop',
           'Frontend workshop for calling real backend APIs and handling loading states.',
           'C304', TRUNC(SYSDATE) + 35,
           CAST(TRUNC(SYSDATE) + 35 AS TIMESTAMP) + INTERVAL '18' HOUR,
           CAST(TRUNC(SYSDATE) + 35 AS TIMESTAMP) + INTERVAL '21' HOUR,
           'Offline', NULL, 10, 0, 'Upcoming'
    FROM classes c JOIN users u ON LOWER(u.email) = 'e2e.trainer02@fap.local'
    WHERE c.class_code = 'E2E-FULLSTACK-2026-01' UNION ALL
    SELECT c.id, u.id, 'E2E Weekend Code Review',
           'Online code review with an optional physical room left empty.',
           NULL, TRUNC(SYSDATE) + 42,
           CAST(TRUNC(SYSDATE) + 42 AS TIMESTAMP) + INTERVAL '8' HOUR,
           CAST(TRUNC(SYSDATE) + 42 AS TIMESTAMP) + INTERVAL '12' HOUR,
           'Online', 'https://meet.example.com/e2e-code-review', 15, 0, 'Upcoming'
    FROM classes c JOIN users u ON LOWER(u.email) = 'e2e.trainer03@fap.local'
    WHERE c.class_code = 'E2E-JAVA-WEEKEND' UNION ALL
    SELECT c.id, u.id, 'E2E Canceled Practice',
           'Canceled session used for status and exception-flow checks.',
           'A102', TRUNC(SYSDATE) + 3,
           CAST(TRUNC(SYSDATE) + 3 AS TIMESTAMP) + INTERVAL '13' HOUR,
           CAST(TRUNC(SYSDATE) + 3 AS TIMESTAMP) + INTERVAL '15' HOUR,
           'Offline', NULL, 20, 0, 'Canceled'
    FROM classes c JOIN users u ON LOWER(u.email) = 'e2e.trainer01@fap.local'
    WHERE c.class_code = 'E2E-JAVA-2026-01' UNION ALL
    SELECT c.id, u.id, 'E2E Alumni Retrospective',
           'Historical completed session for past-date filters.',
           'A103', TRUNC(SYSDATE) - 90,
           CAST(TRUNC(SYSDATE) - 90 AS TIMESTAMP) + INTERVAL '8' HOUR,
           CAST(TRUNC(SYSDATE) - 90 AS TIMESTAMP) + INTERVAL '10' HOUR,
           'Offline', NULL, 20, 2, 'Completed'
    FROM classes c JOIN users u ON LOWER(u.email) = 'e2e.trainer01@fap.local'
    WHERE c.class_code = 'E2E-JAVA-2025-ALUMNI'
) source
ON (target.class_id = source.class_id AND target.title = source.title)
WHEN MATCHED THEN UPDATE SET
    target.description = source.description,
    target.trainer_id = source.trainer_id,
    target.room = source.room,
    target.session_date = source.session_date,
    target.start_time = source.start_time,
    target.end_time = source.end_time,
    target.session_type = source.session_type,
    target.meeting_link = source.meeting_link,
    target.capacity = source.capacity,
    target.enrolled_count = source.enrolled_count,
    target.status = source.status,
    target.is_deleted = 0,
    target.deleted_at = NULL,
    target.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    id, class_id, title, description, trainer_id, room, session_date, start_time,
    end_time, session_type, meeting_link, capacity, enrolled_count, status,
    is_deleted, version_no, created_at, updated_at, created_by, updated_by
) VALUES (
    training_sessions_seq.NEXTVAL, source.class_id, source.title, source.description,
    source.trainer_id, source.room, source.session_date, source.start_time, source.end_time,
    source.session_type, source.meeting_link, source.capacity, source.enrolled_count,
    source.status, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local'),
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local')
);

MERGE INTO training_registrations target
USING (
    SELECT training.id training_id, trainee.id user_id, registrations.status,
           registrations.registered_at, registrations.cancelled_at, registrations.completed_at
    FROM training_sessions training
    JOIN (
        SELECT 'E2E Java Kickoff Completed' session_title, 'e2e.trainee01@fap.local' email, 'Completed' status,
               SYSTIMESTAMP - INTERVAL '16' DAY registered_at, NULL cancelled_at, SYSTIMESTAMP - INTERVAL '14' DAY completed_at FROM dual UNION ALL
        SELECT 'E2E Java Kickoff Completed', 'e2e.trainee02@fap.local', 'Completed', SYSTIMESTAMP - INTERVAL '16' DAY, NULL, SYSTIMESTAMP - INTERVAL '14' DAY FROM dual UNION ALL
        SELECT 'E2E Java Kickoff Completed', 'e2e.trainee03@fap.local', 'Completed', SYSTIMESTAMP - INTERVAL '16' DAY, NULL, SYSTIMESTAMP - INTERVAL '14' DAY FROM dual UNION ALL
        SELECT 'E2E Spring REST Workshop', 'e2e.trainee01@fap.local', 'Registered', SYSTIMESTAMP - INTERVAL '2' DAY, NULL, NULL FROM dual UNION ALL
        SELECT 'E2E Spring REST Workshop', 'e2e.trainee02@fap.local', 'Registered', SYSTIMESTAMP - INTERVAL '2' DAY, NULL, NULL FROM dual UNION ALL
        SELECT 'E2E Spring REST Workshop', 'e2e.trainee03@fap.local', 'Registered', SYSTIMESTAMP - INTERVAL '1' DAY, NULL, NULL FROM dual UNION ALL
        SELECT 'E2E Spring REST Workshop', 'e2e.trainee04@fap.local', 'Waitlist', SYSTIMESTAMP - INTERVAL '12' HOUR, NULL, NULL FROM dual UNION ALL
        SELECT 'E2E Spring REST Workshop', 'e2e.trainee05@fap.local', 'Waitlist', SYSTIMESTAMP - INTERVAL '6' HOUR, NULL, NULL FROM dual UNION ALL
        SELECT 'E2E Spring REST Workshop', 'e2e.trainee06@fap.local', 'Cancelled', SYSTIMESTAMP - INTERVAL '3' DAY, SYSTIMESTAMP - INTERVAL '1' DAY, NULL FROM dual UNION ALL
        SELECT 'E2E Java Security Lab', 'e2e.trainee02@fap.local', 'Registered', SYSTIMESTAMP - INTERVAL '1' DAY, NULL, NULL FROM dual UNION ALL
        SELECT 'E2E Full Stack React Lab', 'e2e.trainee07@fap.local', 'Registered', SYSTIMESTAMP - INTERVAL '1' DAY, NULL, NULL FROM dual UNION ALL
        SELECT 'E2E Full Stack React Lab', 'e2e.trainee08@fap.local', 'Registered', SYSTIMESTAMP - INTERVAL '1' DAY, NULL, NULL FROM dual UNION ALL
        SELECT 'E2E Alumni Retrospective', 'e2e.trainee09@fap.local', 'Completed', SYSTIMESTAMP - NUMTODSINTERVAL(100, 'DAY'), NULL, SYSTIMESTAMP - INTERVAL '90' DAY FROM dual UNION ALL
        SELECT 'E2E Alumni Retrospective', 'e2e.trainee10@fap.local', 'Completed', SYSTIMESTAMP - NUMTODSINTERVAL(100, 'DAY'), NULL, SYSTIMESTAMP - INTERVAL '90' DAY FROM dual
    ) registrations ON registrations.session_title = training.title
    JOIN users trainee ON LOWER(trainee.email) = registrations.email
) source
ON (target.training_id = source.training_id AND target.user_id = source.user_id)
WHEN MATCHED THEN UPDATE SET
    target.status = source.status,
    target.registered_at = source.registered_at,
    target.cancelled_at = source.cancelled_at,
    target.completed_at = source.completed_at
WHEN NOT MATCHED THEN INSERT (
    id, training_id, user_id, status, registered_at, cancelled_at, completed_at, version_no
) VALUES (
    training_registrations_seq.NEXTVAL, source.training_id, source.user_id, source.status,
    source.registered_at, source.cancelled_at, source.completed_at, 0
);

MERGE INTO attendance_records target
USING (
    SELECT training.id training_id, trainee.id user_id, attendance.status,
           attendance.checked_in_at, attendance.check_in_method, attendance.correction_reason
    FROM training_sessions training
    JOIN (
        SELECT 'E2E Java Kickoff Completed' session_title, 'e2e.trainee01@fap.local' email,
               'Present' status, SYSTIMESTAMP - INTERVAL '14' DAY - INTERVAL '5' MINUTE checked_in_at,
               'QR' check_in_method, NULL correction_reason FROM dual UNION ALL
        SELECT 'E2E Java Kickoff Completed', 'e2e.trainee02@fap.local',
               'Late', SYSTIMESTAMP - INTERVAL '14' DAY + INTERVAL '10' MINUTE,
               'Manual', 'Tắc đường, đã được giảng viên xác nhận.' FROM dual UNION ALL
        SELECT 'E2E Java Kickoff Completed', 'e2e.trainee03@fap.local',
               'Absent', NULL, 'Manual', NULL FROM dual UNION ALL
        SELECT 'E2E Alumni Retrospective', 'e2e.trainee09@fap.local',
               'Present', SYSTIMESTAMP - INTERVAL '90' DAY - INTERVAL '5' MINUTE,
               'Manual', NULL FROM dual UNION ALL
        SELECT 'E2E Alumni Retrospective', 'e2e.trainee10@fap.local',
               'Present', SYSTIMESTAMP - INTERVAL '90' DAY - INTERVAL '3' MINUTE,
               'QR', NULL FROM dual
    ) attendance ON attendance.session_title = training.title
    JOIN users trainee ON LOWER(trainee.email) = attendance.email
) source
ON (target.training_id = source.training_id AND target.user_id = source.user_id)
WHEN MATCHED THEN UPDATE SET
    target.status = source.status,
    target.checked_in_at = source.checked_in_at,
    target.check_in_method = source.check_in_method,
    target.correction_reason = source.correction_reason,
    target.updated_by = (SELECT id FROM users WHERE LOWER(email) = 'e2e.trainer01@fap.local'),
    target.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    id, training_id, user_id, status, checked_in_at, check_in_method, updated_by,
    correction_reason, created_at, updated_at
) VALUES (
    attendance_records_seq.NEXTVAL, source.training_id, source.user_id, source.status,
    source.checked_in_at, source.check_in_method,
    (SELECT id FROM users WHERE LOWER(email) = 'e2e.trainer01@fap.local'),
    source.correction_reason, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

MERGE INTO training_feedbacks target
USING (
    SELECT training.id training_id, trainee.id user_id,
           feedback.rating_content, feedback.rating_trainer, feedback.rating_organization,
           feedback.feedback_comment
    FROM training_sessions training
    JOIN (
        SELECT 'E2E Java Kickoff Completed' session_title, 'e2e.trainee01@fap.local' email,
               5 rating_content, 5 rating_trainer, 4 rating_organization,
               'Nội dung rõ ràng, ví dụ thực tế và dễ theo dõi.' feedback_comment FROM dual UNION ALL
        SELECT 'E2E Java Kickoff Completed', 'e2e.trainee02@fap.local',
               4, 5, 4, 'Buổi học tốt, cần thêm thời gian thực hành.' FROM dual UNION ALL
        SELECT 'E2E Alumni Retrospective', 'e2e.trainee09@fap.local',
               4, 4, 5, NULL FROM dual
    ) feedback ON feedback.session_title = training.title
    JOIN users trainee ON LOWER(trainee.email) = feedback.email
) source
ON (target.training_id = source.training_id AND target.user_id = source.user_id)
WHEN MATCHED THEN UPDATE SET
    target.rating_content = source.rating_content,
    target.rating_trainer = source.rating_trainer,
    target.rating_organization = source.rating_organization,
    target.feedback_comment = source.feedback_comment,
    target.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    id, training_id, user_id, rating_content, rating_trainer, rating_organization,
    feedback_comment, created_at, updated_at
) VALUES (
    training_feedbacks_seq.NEXTVAL, source.training_id, source.user_id,
    source.rating_content, source.rating_trainer, source.rating_organization,
    source.feedback_comment, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- =====================================================
-- 5. QUESTION BANK, QUIZZES, ASSIGNMENTS, ATTEMPTS
-- =====================================================
MERGE INTO questions target
USING (
    SELECT '[E2E] Which Java keyword creates an immutable variable reference?' content,
           'single' question_type, 'E2E Java' category, 'Easy' difficulty,
           '["final","static","const","sealed"]' options_json,
           '["final"]' correct_answers_json,
           'The final keyword prevents reassignment of the variable reference.' explanation FROM dual UNION ALL
    SELECT '[E2E] Which HTTP methods are normally idempotent?', 'multiple', 'E2E REST', 'Medium',
           '["GET","POST","PUT","DELETE"]', '["GET","PUT","DELETE"]',
           'GET, PUT, and DELETE are defined as idempotent methods.' FROM dual UNION ALL
    SELECT '[E2E] Which annotation validates a Spring MVC request body?', 'single', 'E2E Spring', 'Easy',
           '["@Valid","@Bean","@Value","@Profile"]', '["@Valid"]',
           '@Valid triggers Bean Validation for the request object.' FROM dual UNION ALL
    SELECT '[E2E] Which values should a JWT access token contain?', 'multiple', 'E2E Security', 'Hard',
           '["subject","expiration","roles","plain-text password"]', '["subject","expiration","roles"]',
           'Passwords must never be stored in a JWT.' FROM dual UNION ALL
    SELECT '[E2E] Which SQL statement completes a transaction?', 'single', 'E2E Database', 'Easy',
           '["COMMIT","SELECT","GRANT","DESCRIBE"]', '["COMMIT"]',
           'COMMIT makes the current transaction durable.' FROM dual UNION ALL
    SELECT '[E2E] Which Docker file describes how an image is built?', 'single', 'E2E DevOps', 'Medium',
           '["Dockerfile","pom.xml","package.json","README.md"]', '["Dockerfile"]',
           'A Dockerfile contains image build instructions.' FROM dual
) source
ON (DBMS_LOB.SUBSTR(target.content, 4000, 1) = source.content)
WHEN MATCHED THEN UPDATE SET
    target.question_type = source.question_type,
    target.category = source.category,
    target.difficulty = source.difficulty,
    target.options_json = source.options_json,
    target.correct_answers_json = source.correct_answers_json,
    target.explanation = source.explanation,
    target.is_deleted = 0,
    target.deleted_at = NULL,
    target.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    id, content, question_type, category, difficulty, options_json,
    correct_answers_json, explanation, is_deleted, created_at, updated_at,
    created_by, updated_by
) VALUES (
    questions_seq.NEXTVAL, source.content, source.question_type, source.category,
    source.difficulty, source.options_json, source.correct_answers_json,
    source.explanation, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local'),
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local')
);

MERGE INTO quizzes target
USING (
    SELECT '[E2E] Java Backend Readiness' title,
           'Published quiz assigned to the E2E Java class.' description,
           30 duration_minutes, 60 passing_score, 3 max_attempts, 1 randomize,
           'E2E Java' category, 'Published' status,
           TRUNC(SYSDATE) - 30 open_date, TRUNC(SYSDATE) + 90 close_date FROM dual UNION ALL
    SELECT '[E2E] Spring Security Draft', 'Draft quiz used to verify edit and publish guards.',
           20, 70, 2, 0, 'E2E Security', 'Draft',
           TRUNC(SYSDATE) + 10, TRUNC(SYSDATE) + 40 FROM dual UNION ALL
    SELECT '[E2E] Archived Fundamentals', 'Closed quiz used for status filtering.',
           15, 50, 1, 0, 'E2E Archive', 'Closed',
           TRUNC(SYSDATE) - 120, TRUNC(SYSDATE) - 60 FROM dual
) source
ON (target.title = source.title AND target.category = source.category)
WHEN MATCHED THEN UPDATE SET
    target.description = source.description,
    target.duration_minutes = source.duration_minutes,
    target.passing_score = source.passing_score,
    target.max_attempts = source.max_attempts,
    target.randomize = source.randomize,
    target.status = source.status,
    target.open_date = source.open_date,
    target.close_date = source.close_date,
    target.is_deleted = 0,
    target.deleted_at = NULL,
    target.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    id, title, description, duration_minutes, passing_score, max_attempts,
    randomize, category, status, open_date, close_date, is_deleted, version_no,
    created_at, updated_at, created_by, updated_by
) VALUES (
    quizzes_seq.NEXTVAL, source.title, source.description, source.duration_minutes,
    source.passing_score, source.max_attempts, source.randomize, source.category,
    source.status, source.open_date, source.close_date, 0, 0,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local'),
    (SELECT id FROM users WHERE LOWER(email) = 'admin@fap.local')
);

MERGE INTO quiz_questions target
USING (
    SELECT quiz.id quiz_id, question.id question_id, links.sort_order, links.points
    FROM quizzes quiz
    JOIN (
        SELECT '[E2E] Java Backend Readiness' quiz_title,
               '[E2E] Which Java keyword creates an immutable variable reference?' question_content,
               1 sort_order, 1.00 points FROM dual UNION ALL
        SELECT '[E2E] Java Backend Readiness', '[E2E] Which HTTP methods are normally idempotent?', 2, 2.00 FROM dual UNION ALL
        SELECT '[E2E] Java Backend Readiness', '[E2E] Which annotation validates a Spring MVC request body?', 3, 1.00 FROM dual UNION ALL
        SELECT '[E2E] Java Backend Readiness', '[E2E] Which values should a JWT access token contain?', 4, 2.00 FROM dual UNION ALL
        SELECT '[E2E] Java Backend Readiness', '[E2E] Which SQL statement completes a transaction?', 5, 1.00 FROM dual UNION ALL
        SELECT '[E2E] Spring Security Draft', '[E2E] Which values should a JWT access token contain?', 1, 1.00 FROM dual
    ) links ON links.quiz_title = quiz.title
    JOIN questions question ON DBMS_LOB.SUBSTR(question.content, 4000, 1) = links.question_content
) source
ON (target.quiz_id = source.quiz_id AND target.question_id = source.question_id)
WHEN MATCHED THEN UPDATE SET
    target.sort_order = source.sort_order,
    target.points = source.points
WHEN NOT MATCHED THEN INSERT (quiz_id, question_id, sort_order, points)
VALUES (source.quiz_id, source.question_id, source.sort_order, source.points);

MERGE INTO quiz_assignments target
USING (
    SELECT quiz.id quiz_id, class_record.id class_id, CAST(NULL AS NUMBER) training_session_id,
           admin_user.id assigned_by
    FROM quizzes quiz
    JOIN classes class_record ON class_record.class_code = 'E2E-JAVA-2026-01'
    JOIN users admin_user ON LOWER(admin_user.email) = 'admin@fap.local'
    WHERE quiz.title = '[E2E] Java Backend Readiness'
) source
ON (target.quiz_id = source.quiz_id AND target.class_id = source.class_id)
WHEN NOT MATCHED THEN INSERT (
    id, quiz_id, class_id, training_session_id, assigned_by, assigned_at
) VALUES (
    quiz_assignments_seq.NEXTVAL, source.quiz_id, source.class_id,
    source.training_session_id, source.assigned_by, CURRENT_TIMESTAMP
);

MERGE INTO quiz_attempts target
USING (
    SELECT quiz.id quiz_id, trainee.id user_id, attempts.attempt_number,
           attempts.status, attempts.answers_json, attempts.score,
           attempts.correct_count, attempts.total_questions, attempts.passed,
           attempts.time_taken_seconds, attempts.started_at, attempts.submitted_at
    FROM quizzes quiz
    JOIN (
        SELECT 'e2e.trainee02@fap.local' email, 1 attempt_number, 'Submitted' status,
               '[]' answers_json,
               100 score, 5 correct_count, 5 total_questions, 1 passed,
               1200 time_taken_seconds, SYSTIMESTAMP - INTERVAL '2' HOUR started_at,
               SYSTIMESTAMP - NUMTODSINTERVAL(100, 'MINUTE') submitted_at FROM dual UNION ALL
        SELECT 'e2e.trainee03@fap.local', 1, 'Submitted', '[]',
               0, 0, 5, 0, 1500,
               SYSTIMESTAMP - INTERVAL '3' HOUR,
               SYSTIMESTAMP - NUMTODSINTERVAL(150, 'MINUTE') FROM dual UNION ALL
        SELECT 'e2e.trainee03@fap.local', 2, 'InProgress', '[]',
               NULL, NULL, NULL, NULL, NULL,
               SYSTIMESTAMP - INTERVAL '10' MINUTE, NULL FROM dual
    ) attempts ON 1 = 1
    JOIN users trainee ON LOWER(trainee.email) = attempts.email
    WHERE quiz.title = '[E2E] Java Backend Readiness'
) source
ON (target.quiz_id = source.quiz_id
    AND target.user_id = source.user_id
    AND target.attempt_number = source.attempt_number)
WHEN MATCHED THEN UPDATE SET
    target.status = source.status,
    target.answers_json = source.answers_json,
    target.score = source.score,
    target.correct_count = source.correct_count,
    target.total_questions = source.total_questions,
    target.passed = source.passed,
    target.time_taken_seconds = source.time_taken_seconds,
    target.started_at = source.started_at,
    target.submitted_at = source.submitted_at
WHEN NOT MATCHED THEN INSERT (
    id, quiz_id, user_id, attempt_number, status, answers_json, score,
    correct_count, total_questions, passed, time_taken_seconds,
    started_at, submitted_at, version_no
) VALUES (
    quiz_attempts_seq.NEXTVAL, source.quiz_id, source.user_id, source.attempt_number,
    source.status, source.answers_json, source.score, source.correct_count,
    source.total_questions, source.passed, source.time_taken_seconds,
    source.started_at, source.submitted_at, 0
);

UPDATE quiz_attempts attempt
SET answers_json = (
    SELECT '[' || LISTAGG(
        '{"questionId":' || question.id
        || ',"selectedAnswersJson":'
        || DBMS_LOB.SUBSTR(question.correct_answers_json, 4000, 1) || '}',
        ','
    ) WITHIN GROUP (ORDER BY quiz_question.sort_order) || ']'
    FROM quiz_questions quiz_question
    JOIN questions question ON question.id = quiz_question.question_id
    WHERE quiz_question.quiz_id = attempt.quiz_id
)
WHERE attempt.quiz_id = (
        SELECT id FROM quizzes WHERE title = '[E2E] Java Backend Readiness'
    )
  AND attempt.user_id = (
        SELECT id FROM users WHERE LOWER(email) = 'e2e.trainee02@fap.local'
    )
  AND attempt.attempt_number = 1;

-- =====================================================
-- 6. NOTIFICATIONS, SETTINGS, AND RESET SENTINEL
-- =====================================================
MERGE INTO notifications target
USING (
    SELECT u.id user_id, messages.title, messages.message, messages.is_read
    FROM users u
    JOIN (
        SELECT 'e2e.trainee01@fap.local' email, 'E2E training reminder' title,
               'Spring REST Workshop starts in seven days.' message, 0 is_read FROM dual UNION ALL
        SELECT 'e2e.trainee02@fap.local', 'E2E quiz result available',
               'Your Java Backend Readiness result is now available.', 1 FROM dual UNION ALL
        SELECT 'e2e.trainer01@fap.local', 'E2E attendance pending',
               'Please review attendance for the upcoming Java sessions.', 0 FROM dual
    ) messages ON LOWER(u.email) = messages.email
) source
ON (target.user_id = source.user_id AND target.title = source.title)
WHEN MATCHED THEN UPDATE SET
    target.message = source.message,
    target.is_read = source.is_read
WHEN NOT MATCHED THEN INSERT (
    id, user_id, title, message, is_read, created_at
) VALUES (
    notifications_seq.NEXTVAL, source.user_id, source.title,
    source.message, source.is_read, CURRENT_TIMESTAMP
);

MERGE INTO system_settings target
USING (
    SELECT 'DEV_SEED' category, 'dataset_version' setting_key, 'e2e-v1' setting_value FROM dual UNION ALL
    SELECT 'DEV_SEED', 'display_organization_name', 'FAP Local Training Center' FROM dual UNION ALL
    SELECT 'DEV_SEED', 'display_default_capacity', '30' FROM dual UNION ALL
    SELECT 'DEV_SEED', 'display_email_enabled', 'false' FROM dual
) source
ON (target.category = source.category AND target.setting_key = source.setting_key)
WHEN MATCHED THEN UPDATE SET
    target.setting_value = source.setting_value,
    target.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    id, category, setting_key, setting_value, created_at, updated_at
) VALUES (
    system_settings_seq.NEXTVAL, source.category, source.setting_key,
    source.setting_value, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

COMMIT;
