-- V22: Seed comprehensive sample data for FAP Backend
-- At least 20 records per major table
-- Note: created_by is left NULL to avoid FK constraint issues

-- =====================================================
-- 1. USERS (20 users: mix of all roles)
-- Password: 'password123' = BCrypt hash
-- =====================================================
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (2, 'Nguyen Van Trainer1', 'trainer1@fap.edu.vn', '0901000001', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '1985-03-15', 'Male', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (3, 'Tran Thi Trainer2', 'trainer2@fap.edu.vn', '0901000002', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '1988-07-22', 'Female', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (4, 'Le Van Trainer3', 'trainer3@fap.edu.vn', '0901000003', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '1990-01-10', 'Male', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (5, 'Pham Thi ClassAdmin1', 'classadmin1@fap.edu.vn', '0901000004', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '1992-05-18', 'Female', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (6, 'Hoang Van ClassAdmin2', 'classadmin2@fap.edu.vn', '0901000005', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '1991-09-25', 'Male', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (7, 'Nguyen Trainee01', 'trainee01@fap.edu.vn', '0901000006', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2000-02-14', 'Male', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (8, 'Tran Trainee02', 'trainee02@fap.edu.vn', '0901000007', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2001-04-20', 'Female', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (9, 'Le Trainee03', 'trainee03@fap.edu.vn', '0901000008', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2000-06-30', 'Male', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (10, 'Pham Trainee04', 'trainee04@fap.edu.vn', '0901000009', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2001-08-12', 'Female', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (11, 'Hoang Trainee05', 'trainee05@fap.edu.vn', '0901000010', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2000-10-05', 'Male', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (12, 'Vu Trainee06', 'trainee06@fap.edu.vn', '0901000011', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2001-12-18', 'Female', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (13, 'Dao Trainee07', 'trainee07@fap.edu.vn', '0901000012', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2000-03-22', 'Male', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (14, 'Bui Trainee08', 'trainee08@fap.edu.vn', '0901000013', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2001-05-08', 'Female', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (15, 'Dinh Trainee09', 'trainee09@fap.edu.vn', '0901000014', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2000-07-15', 'Male', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (16, 'Duong Trainee10', 'trainee10@fap.edu.vn', '0901000015', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2001-09-28', 'Female', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (17, 'Ly Trainee11', 'trainee11@fap.edu.vn', '0901000016', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2000-11-11', 'Male', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (18, 'Ngo Trainee12', 'trainee12@fap.edu.vn', '0901000017', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2001-01-25', 'Female', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (19, 'Mai Trainee13', 'trainee13@fap.edu.vn', '0901000018', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2000-04-03', 'Male', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (20, 'Truong Trainee14', 'trainee14@fap.edu.vn', '0901000019', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2001-06-17', 'Female', 'Active');
INSERT INTO users (id, full_name, email, phone, password_hash, date_of_birth, gender, status)
VALUES (21, 'Vo Trainee15', 'trainee15@fap.edu.vn', '0901000020', '$2a$10$N9qo8uLOickgx2ZMRZoMyed1XOQp15DjKnDzJj0z3Zk5Q5k5Q5k5Q', DATE '2000-08-29', 'Male', 'Active');

-- =====================================================
-- 2. USER_ROLES (assign roles to users)
-- Role IDs from V2: 1000=Super Admin, 1001=Class Admin, 1002=Trainer, 1003=Trainee
-- =====================================================
-- Trainers (role_id = 1002)
INSERT INTO user_roles (user_id, role_id) VALUES (2, 1002);
INSERT INTO user_roles (user_id, role_id) VALUES (3, 1002);
INSERT INTO user_roles (user_id, role_id) VALUES (4, 1002);
-- Class Admins (role_id = 1001)
INSERT INTO user_roles (user_id, role_id) VALUES (5, 1001);
INSERT INTO user_roles (user_id, role_id) VALUES (6, 1001);
-- Trainees (role_id = 1003)
INSERT INTO user_roles (user_id, role_id) VALUES (7, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (8, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (9, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (10, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (11, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (12, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (13, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (14, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (15, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (16, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (17, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (18, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (19, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (20, 1003);
INSERT INTO user_roles (user_id, role_id) VALUES (21, 1003);

-- =====================================================
-- 3. SYLLABUSES (20 syllabuses)
-- =====================================================
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (1, 'Java Fundamentals', 'JAVA-F01', 'v1.0', 'Active', 'Beginner', 30, '40 hours', 'Learn Java basics including OOP concepts');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (2, 'Spring Boot Essentials', 'SPRING-01', 'v1.0', 'Active', 'Intermediate', 25, '60 hours', 'Build REST APIs with Spring Boot');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (3, 'React Development', 'REACT-01', 'v1.0', 'Active', 'Intermediate', 30, '50 hours', 'Build modern web apps with React');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (4, 'Database Design', 'DB-F01', 'v1.0', 'Active', 'Beginner', 35, '30 hours', 'Design efficient database schemas');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (5, 'Microservices Architecture', 'MICRO-01', 'v1.0', 'Active', 'Advanced', 20, '80 hours', 'Design and implement microservices');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (6, 'DevOps Practices', 'DEVOPS-01', 'v1.0', 'Active', 'Intermediate', 25, '45 hours', 'CI/CD pipelines and containerization');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (7, 'Python for Data Science', 'PY-DS01', 'v1.0', 'Active', 'Beginner', 30, '55 hours', 'Data analysis with Python');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (8, 'Angular Framework', 'ANG-01', 'v1.0', 'Active', 'Intermediate', 25, '50 hours', 'Build enterprise apps with Angular');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (9, 'AWS Cloud Fundamentals', 'AWS-F01', 'v1.0', 'Active', 'Beginner', 30, '35 hours', 'Cloud computing with AWS');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (10, 'Kubernetes Administration', 'K8S-01', 'v1.0', 'Active', 'Advanced', 20, '70 hours', 'Container orchestration with K8s');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (11, 'Node.js Backend', 'NODE-01', 'v1.0', 'Active', 'Intermediate', 25, '45 hours', 'Server-side JS with Node.js');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (12, 'TypeScript Mastery', 'TS-01', 'v1.0', 'Pending', 'Intermediate', 30, '30 hours', 'Type-safe JavaScript development');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (13, 'Git Version Control', 'GIT-01', 'v1.0', 'Active', 'Beginner', 40, '15 hours', 'Source control with Git');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (14, 'Agile Project Management', 'AGILE-01', 'v1.0', 'Active', 'All levels', 35, '20 hours', 'Scrum and Agile methodologies');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (15, 'Software Testing', 'TEST-01', 'v1.0', 'Active', 'Intermediate', 30, '40 hours', 'Testing strategies and automation');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (16, 'Docker Containers', 'DOCKER-01', 'v1.0', 'Active', 'Beginner', 30, '25 hours', 'Containerization with Docker');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (17, 'GraphQL APIs', 'GRAPHQL-01', 'v1.0', 'Drafting', 'Intermediate', 25, '35 hours', 'Build GraphQL APIs');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (18, 'Machine Learning Basics', 'ML-01', 'v1.0', 'Pending', 'Intermediate', 25, '60 hours', 'Introduction to ML algorithms');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (19, 'Security Best Practices', 'SEC-01', 'v1.0', 'Active', 'Advanced', 25, '40 hours', 'Application security fundamentals');
INSERT INTO syllabuses (id, name, code, version, status, level_name, attendees, duration, course_objectives)
VALUES (20, 'Vue.js Framework', 'VUE-01', 'v1.0', 'Active', 'Intermediate', 30, '45 hours', 'Frontend development with Vue');

-- =====================================================
-- 4. SYLLABUS_OUTPUT_STANDARDS
-- =====================================================
INSERT INTO syllabus_output_standards (syllabus_id, standard_code) VALUES (1, 'H4SD');
INSERT INTO syllabus_output_standards (syllabus_id, standard_code) VALUES (1, 'K6SD');
INSERT INTO syllabus_output_standards (syllabus_id, standard_code) VALUES (2, 'H4SD');
INSERT INTO syllabus_output_standards (syllabus_id, standard_code) VALUES (2, 'H1SD');
INSERT INTO syllabus_output_standards (syllabus_id, standard_code) VALUES (3, 'H4SD');
INSERT INTO syllabus_output_standards (syllabus_id, standard_code) VALUES (3, 'C3SD');
INSERT INTO syllabus_output_standards (syllabus_id, standard_code) VALUES (4, 'K6SD');
INSERT INTO syllabus_output_standards (syllabus_id, standard_code) VALUES (5, 'H4SD');
INSERT INTO syllabus_output_standards (syllabus_id, standard_code) VALUES (5, 'H2SD');

-- =====================================================
-- 5. SYLLABUS_DAYS, SYLLABUS_UNITS, SYLLABUS_TOPICS
-- =====================================================
INSERT INTO syllabus_days (id, syllabus_id, day_number, sort_order) VALUES (1, 1, 1, 1);
INSERT INTO syllabus_days (id, syllabus_id, day_number, sort_order) VALUES (2, 1, 2, 2);
INSERT INTO syllabus_days (id, syllabus_id, day_number, sort_order) VALUES (3, 1, 3, 3);
INSERT INTO syllabus_days (id, syllabus_id, day_number, sort_order) VALUES (4, 2, 1, 1);
INSERT INTO syllabus_days (id, syllabus_id, day_number, sort_order) VALUES (5, 2, 2, 2);
INSERT INTO syllabus_days (id, syllabus_id, day_number, sort_order) VALUES (6, 3, 1, 1);

INSERT INTO syllabus_units (id, day_id, name, sort_order) VALUES (1, 1, 'Introduction to Java', 1);
INSERT INTO syllabus_units (id, day_id, name, sort_order) VALUES (2, 1, 'Variables and Data Types', 2);
INSERT INTO syllabus_units (id, day_id, name, sort_order) VALUES (3, 2, 'Control Flow', 1);
INSERT INTO syllabus_units (id, day_id, name, sort_order) VALUES (4, 2, 'Methods and Functions', 2);
INSERT INTO syllabus_units (id, day_id, name, sort_order) VALUES (5, 3, 'OOP Concepts', 1);
INSERT INTO syllabus_units (id, day_id, name, sort_order) VALUES (6, 4, 'Spring Boot Intro', 1);
INSERT INTO syllabus_units (id, day_id, name, sort_order) VALUES (7, 5, 'REST Controllers', 1);
INSERT INTO syllabus_units (id, day_id, name, sort_order) VALUES (8, 6, 'React Basics', 1);

INSERT INTO syllabus_topics (id, unit_id, name, output_standard, is_online, duration_minutes, sort_order) VALUES (1, 1, 'JDK Installation', 'H4SD', 0, 30, 1);
INSERT INTO syllabus_topics (id, unit_id, name, output_standard, is_online, duration_minutes, sort_order) VALUES (2, 1, 'Hello World Program', 'H4SD', 0, 45, 2);
INSERT INTO syllabus_topics (id, unit_id, name, output_standard, is_online, duration_minutes, sort_order) VALUES (3, 2, 'Primitive Types', 'K6SD', 1, 60, 1);
INSERT INTO syllabus_topics (id, unit_id, name, output_standard, is_online, duration_minutes, sort_order) VALUES (4, 3, 'If-Else Statements', 'H4SD', 1, 45, 1);
INSERT INTO syllabus_topics (id, unit_id, name, output_standard, is_online, duration_minutes, sort_order) VALUES (5, 3, 'Loops', 'H4SD', 1, 60, 2);
INSERT INTO syllabus_topics (id, unit_id, name, output_standard, is_online, duration_minutes, sort_order) VALUES (6, 4, 'Method Declaration', 'K6SD', 0, 45, 1);
INSERT INTO syllabus_topics (id, unit_id, name, output_standard, is_online, duration_minutes, sort_order) VALUES (7, 5, 'Classes and Objects', 'H4SD', 0, 90, 1);
INSERT INTO syllabus_topics (id, unit_id, name, output_standard, is_online, duration_minutes, sort_order) VALUES (8, 6, 'Spring Initializr', 'H4SD', 1, 30, 1);
INSERT INTO syllabus_topics (id, unit_id, name, output_standard, is_online, duration_minutes, sort_order) VALUES (9, 6, 'Project Structure', 'H1SD', 1, 45, 2);
INSERT INTO syllabus_topics (id, unit_id, name, output_standard, is_online, duration_minutes, sort_order) VALUES (10, 7, 'GET/POST Endpoints', 'H4SD', 0, 90, 1);
INSERT INTO syllabus_topics (id, unit_id, name, output_standard, is_online, duration_minutes, sort_order) VALUES (11, 8, 'JSX Syntax', 'H4SD', 1, 60, 1);
INSERT INTO syllabus_topics (id, unit_id, name, output_standard, is_online, duration_minutes, sort_order) VALUES (12, 8, 'Components', 'C3SD', 1, 90, 2);

-- =====================================================
-- 6. TRAINING_PROGRAMS (20 programs)
-- =====================================================
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (1, 'Full-Stack Java Developer', 'Active', '6 months', 480, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (2, 'Frontend Developer Path', 'Active', '4 months', 320, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (3, 'DevOps Engineer Track', 'Active', '5 months', 400, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (4, 'Data Science Bootcamp', 'Active', '6 months', 500, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (5, 'Cloud Architecture', 'Active', '4 months', 350, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (6, 'Mobile Development', 'Planning', '5 months', 420, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (7, 'QA Automation Engineer', 'Active', '3 months', 240, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (8, 'Backend Specialist', 'Active', '4 months', 350, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (9, 'Security Analyst', 'Planning', '4 months', 320, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (10, 'AI/ML Fundamentals', 'Active', '5 months', 400, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (11, 'Project Management', 'Active', '2 months', 160, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (12, 'Database Administrator', 'Active', '3 months', 240, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (13, 'Node.js Developer', 'Active', '3 months', 250, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (14, 'React Native Mobile', 'Planning', '4 months', 320, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (15, 'Microservices Expert', 'Active', '5 months', 400, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (16, 'Fresher Training Program', 'Active', '3 months', 240, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (17, 'Leadership Skills', 'Active', '1 month', 80, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (18, 'Azure Cloud Path', 'Planning', '4 months', 320, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (19, 'Blockchain Developer', 'Inactive', '4 months', 300, 'v1.0');
INSERT INTO training_programs (id, name, status, duration, total_hours, version)
VALUES (20, 'System Design', 'Active', '2 months', 160, 'v1.0');

-- =====================================================
-- 7. TRAINING_PROGRAM_SYLLABUSES
-- =====================================================
INSERT INTO training_program_syllabuses (program_id, syllabus_id, sort_order) VALUES (1, 1, 1);
INSERT INTO training_program_syllabuses (program_id, syllabus_id, sort_order) VALUES (1, 2, 2);
INSERT INTO training_program_syllabuses (program_id, syllabus_id, sort_order) VALUES (1, 4, 3);
INSERT INTO training_program_syllabuses (program_id, syllabus_id, sort_order) VALUES (2, 3, 1);
INSERT INTO training_program_syllabuses (program_id, syllabus_id, sort_order) VALUES (2, 8, 2);
INSERT INTO training_program_syllabuses (program_id, syllabus_id, sort_order) VALUES (3, 6, 1);
INSERT INTO training_program_syllabuses (program_id, syllabus_id, sort_order) VALUES (3, 16, 2);
INSERT INTO training_program_syllabuses (program_id, syllabus_id, sort_order) VALUES (3, 10, 3);
INSERT INTO training_program_syllabuses (program_id, syllabus_id, sort_order) VALUES (4, 7, 1);
INSERT INTO training_program_syllabuses (program_id, syllabus_id, sort_order) VALUES (5, 9, 1);

-- =====================================================
-- 8. CLASSES (20 classes)
-- =====================================================
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (1, 'Java Batch 2026-01', 'JAVA-2026-01', 1, 'Active', 'HCM', 'FSU-HCM1', 'Morning', DATE '2026-01-15', DATE '2026-07-15', '6 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (2, 'Java Batch 2026-02', 'JAVA-2026-02', 1, 'Planning', 'HN', 'FSU-HN1', 'Afternoon', DATE '2026-03-01', DATE '2026-09-01', '6 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (3, 'Frontend Batch 01', 'FE-2026-01', 2, 'Active', 'HCM', 'FSU-HCM2', 'Morning', DATE '2026-02-01', DATE '2026-06-01', '4 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (4, 'DevOps Batch 01', 'DEVOPS-2026-01', 3, 'Active', 'DN', 'FSU-DN1', 'Evening', DATE '2026-01-20', DATE '2026-06-20', '5 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (5, 'Data Science Batch 01', 'DS-2026-01', 4, 'Active', 'HCM', 'FSU-HCM1', 'Morning', DATE '2026-02-15', DATE '2026-08-15', '6 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (6, 'Cloud Batch 01', 'CLOUD-2026-01', 5, 'Planning', 'HN', 'FSU-HN2', 'Afternoon', DATE '2026-04-01', DATE '2026-08-01', '4 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (7, 'QA Batch 01', 'QA-2026-01', 7, 'Active', 'HCM', 'FSU-HCM3', 'Morning', DATE '2026-03-01', DATE '2026-06-01', '3 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (8, 'Backend Batch 01', 'BE-2026-01', 8, 'Active', 'DN', 'FSU-DN1', 'Afternoon', DATE '2026-02-20', DATE '2026-06-20', '4 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (9, 'AI/ML Batch 01', 'AIML-2026-01', 10, 'Planning', 'HCM', 'FSU-HCM1', 'Morning', DATE '2026-05-01', DATE '2026-10-01', '5 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (10, 'PM Batch 01', 'PM-2026-01', 11, 'Active', 'HN', 'FSU-HN1', 'Evening', DATE '2026-03-15', DATE '2026-05-15', '2 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (11, 'DBA Batch 01', 'DBA-2026-01', 12, 'Active', 'HCM', 'FSU-HCM2', 'Afternoon', DATE '2026-04-01', DATE '2026-07-01', '3 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (12, 'Node Batch 01', 'NODE-2026-01', 13, 'Active', 'DN', 'FSU-DN2', 'Morning', DATE '2026-03-01', DATE '2026-06-01', '3 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (13, 'Fresher Batch 2026-Q1', 'FR-2026-Q1', 16, 'Active', 'HCM', 'FSU-HCM1', 'Morning', DATE '2026-01-05', DATE '2026-04-05', '3 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (14, 'Fresher Batch 2026-Q2', 'FR-2026-Q2', 16, 'Planning', 'HN', 'FSU-HN1', 'Afternoon', DATE '2026-04-05', DATE '2026-07-05', '3 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (15, 'Microservices Batch 01', 'MICRO-2026-01', 15, 'Active', 'HCM', 'FSU-HCM3', 'Evening', DATE '2026-02-01', DATE '2026-07-01', '5 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (16, 'Leadership Batch 01', 'LEAD-2026-01', 17, 'Closed', 'HCM', 'FSU-HCM1', 'Morning', DATE '2026-01-01', DATE '2026-02-01', '1 month');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (17, 'System Design Batch 01', 'SD-2026-01', 20, 'Active', 'HN', 'FSU-HN2', 'Afternoon', DATE '2026-03-20', DATE '2026-05-20', '2 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (18, 'Java Batch 2025-04', 'JAVA-2025-04', 1, 'Closed', 'HCM', 'FSU-HCM1', 'Morning', DATE '2025-07-15', DATE '2026-01-15', '6 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (19, 'Frontend Batch 2025-02', 'FE-2025-02', 2, 'Closed', 'DN', 'FSU-DN1', 'Afternoon', DATE '2025-08-01', DATE '2025-12-01', '4 months');
INSERT INTO classes (id, name, class_code, training_program_id, status, location, fsu, class_time, start_date, end_date, duration)
VALUES (20, 'DevOps Batch 2025-01', 'DEVOPS-2025-01', 3, 'Closed', 'HN', 'FSU-HN1', 'Evening', DATE '2025-06-01', DATE '2025-11-01', '5 months');

-- =====================================================
-- 9. CLASS_TRAINERS
-- =====================================================
INSERT INTO class_trainers (id, class_id, user_id, syllabus_id) VALUES (1, 1, 2, 1);
INSERT INTO class_trainers (id, class_id, user_id, syllabus_id) VALUES (2, 1, 3, 2);
INSERT INTO class_trainers (id, class_id, user_id, syllabus_id) VALUES (3, 2, 2, NULL);
INSERT INTO class_trainers (id, class_id, user_id, syllabus_id) VALUES (4, 3, 3, 3);
INSERT INTO class_trainers (id, class_id, user_id, syllabus_id) VALUES (5, 4, 4, NULL);
INSERT INTO class_trainers (id, class_id, user_id, syllabus_id) VALUES (6, 5, 2, NULL);
INSERT INTO class_trainers (id, class_id, user_id, syllabus_id) VALUES (7, 7, 3, NULL);
INSERT INTO class_trainers (id, class_id, user_id, syllabus_id) VALUES (8, 8, 4, NULL);
INSERT INTO class_trainers (id, class_id, user_id, syllabus_id) VALUES (9, 10, 2, NULL);
INSERT INTO class_trainers (id, class_id, user_id, syllabus_id) VALUES (10, 12, 4, NULL);

-- =====================================================
-- 10. CLASS_ADMINS
-- =====================================================
INSERT INTO class_admins (class_id, user_id) VALUES (1, 5);
INSERT INTO class_admins (class_id, user_id) VALUES (2, 5);
INSERT INTO class_admins (class_id, user_id) VALUES (3, 5);
INSERT INTO class_admins (class_id, user_id) VALUES (4, 6);
INSERT INTO class_admins (class_id, user_id) VALUES (5, 5);
INSERT INTO class_admins (class_id, user_id) VALUES (6, 6);
INSERT INTO class_admins (class_id, user_id) VALUES (7, 5);
INSERT INTO class_admins (class_id, user_id) VALUES (8, 6);
INSERT INTO class_admins (class_id, user_id) VALUES (9, 5);
INSERT INTO class_admins (class_id, user_id) VALUES (10, 6);

-- =====================================================
-- 11. QUESTIONS (25 questions)
-- =====================================================
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (1, 'What is the main feature of Java?', 'single', 'Java', 'Easy', '["Platform dependent","Platform independent","Slow performance","Limited library"]', '["Platform independent"]', 'Java is platform independent due to JVM');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (2, 'Which keyword is used to inherit a class in Java?', 'single', 'Java', 'Easy', '["implements","interface","extends","inherits"]', '["extends"]', 'The extends keyword is used for class inheritance');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (3, 'Which are valid primitive types in Java?', 'multiple', 'Java', 'Medium', '["int","String","boolean","double","Integer"]', '["int","boolean","double"]', 'String and Integer are objects, not primitives');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (4, 'What is Spring Boot?', 'single', 'Spring', 'Easy', '["A JavaScript framework","A Java framework for building apps","A database","A web server"]', '["A Java framework for building apps"]', 'Spring Boot simplifies Java application development');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (5, 'Which annotation starts a Spring Boot application?', 'single', 'Spring', 'Easy', '["@Component","@SpringBootApplication","@Service","@Controller"]', '["@SpringBootApplication"]', '@SpringBootApplication combines multiple annotations');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (6, 'What is React?', 'single', 'React', 'Easy', '["A backend framework","A CSS library","A JavaScript library for UI","A database"]', '["A JavaScript library for UI"]', 'React is a JavaScript library for building user interfaces');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (7, 'What is JSX?', 'single', 'React', 'Easy', '["JavaScript XML","Java Server Extension","JSON syntax","JavaScript Extension"]', '["JavaScript XML"]', 'JSX allows writing HTML-like code in JavaScript');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (8, 'Which React hooks are used for state management?', 'multiple', 'React', 'Medium', '["useState","useEffect","useReducer","useContext","useRef"]', '["useState","useReducer","useContext"]', 'useState, useReducer, and useContext handle state');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (9, 'What does SQL stand for?', 'single', 'Database', 'Easy', '["Structured Query Language","Simple Query Language","Standard Query Logic","System Query Language"]', '["Structured Query Language"]', 'SQL stands for Structured Query Language');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (10, 'Which SQL command is used to retrieve data?', 'single', 'Database', 'Easy', '["INSERT","UPDATE","SELECT","DELETE"]', '["SELECT"]', 'SELECT is used to query data from tables');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (11, 'What is normalization in databases?', 'single', 'Database', 'Medium', '["Adding redundancy","Removing redundancy","Creating indexes","Backing up data"]', '["Removing redundancy"]', 'Normalization removes data redundancy');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (12, 'Which are ACID properties?', 'multiple', 'Database', 'Hard', '["Atomicity","Consistency","Isolation","Durability","Availability"]', '["Atomicity","Consistency","Isolation","Durability"]', 'ACID: Atomicity, Consistency, Isolation, Durability');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (13, 'What is Docker?', 'single', 'DevOps', 'Easy', '["A programming language","A containerization platform","A cloud service","A database"]', '["A containerization platform"]', 'Docker containers applications for portability');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (14, 'What is CI/CD?', 'single', 'DevOps', 'Medium', '["Code Integration/Code Delivery","Continuous Integration/Continuous Delivery","Cloud Infrastructure/Cloud Deployment","Custom Integration/Custom Delivery"]', '["Continuous Integration/Continuous Delivery"]', 'CI/CD automates integration and deployment');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (15, 'Which Git command creates a new branch?', 'single', 'Git', 'Easy', '["git merge","git branch","git commit","git push"]', '["git branch"]', 'git branch creates a new branch');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (16, 'What is a REST API?', 'single', 'API', 'Easy', '["A programming language","An architectural style for APIs","A database type","A testing framework"]', '["An architectural style for APIs"]', 'REST defines conventions for web service APIs');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (17, 'Which HTTP methods are idempotent?', 'multiple', 'API', 'Medium', '["GET","POST","PUT","DELETE","PATCH"]', '["GET","PUT","DELETE"]', 'GET, PUT, DELETE produce same result on repeated calls');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (18, 'What is Kubernetes?', 'single', 'DevOps', 'Medium', '["A programming language","A container orchestration platform","A database","A web server"]', '["A container orchestration platform"]', 'Kubernetes manages containerized workloads');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (19, 'What are microservices?', 'single', 'Architecture', 'Medium', '["Large monolithic apps","Small independent services","Database systems","Testing tools"]', '["Small independent services"]', 'Microservices break apps into small services');
INSERT INTO questions (id, content, question_type, category, difficulty, options_json, correct_answers_json, explanation)
VALUES (20, 'Which AWS service provides compute?', 'single', 'Cloud', 'Easy', '["S3","EC2","RDS","Lambda"]', '["EC2"]', 'EC2 provides scalable compute capacity');

-- =====================================================
-- 12. QUIZZES (20 quizzes)
-- =====================================================
INSERT INTO quizzes (id, title, description, duration_minutes, passing_score, max_attempts, randomize, category, status, open_date, close_date)
VALUES (1, 'Java Basics Quiz', 'Test your Java fundamentals', 30, 60, 3, 1, 'Java', 'Published', DATE '2026-01-01', DATE '2026-12-31');
INSERT INTO quizzes (id, title, description, duration_minutes, passing_score, max_attempts, randomize, category, status, open_date, close_date)
VALUES (2, 'Spring Boot Quiz', 'Spring Boot knowledge check', 45, 70, 2, 1, 'Spring', 'Published', DATE '2026-01-01', DATE '2026-12-31');
INSERT INTO quizzes (id, title, description, duration_minutes, passing_score, max_attempts, randomize, category, status, open_date, close_date)
VALUES (3, 'React Fundamentals', 'React basics assessment', 30, 60, 3, 1, 'React', 'Published', DATE '2026-01-01', DATE '2026-12-31');
INSERT INTO quizzes (id, title, description, duration_minutes, passing_score, max_attempts, randomize, category, status, open_date, close_date)
VALUES (4, 'Database Design Quiz', 'SQL and database concepts', 40, 65, 2, 0, 'Database', 'Published', DATE '2026-01-01', DATE '2026-12-31');
INSERT INTO quizzes (id, title, description, duration_minutes, passing_score, max_attempts, randomize, category, status, open_date, close_date)
VALUES (5, 'DevOps Essentials', 'CI/CD and containers', 35, 60, 3, 1, 'DevOps', 'Published', DATE '2026-01-01', DATE '2026-12-31');
INSERT INTO quizzes (id, title, description, duration_minutes, passing_score, max_attempts, randomize, category, status, open_date, close_date)
VALUES (6, 'Git Version Control', 'Git commands and workflow', 20, 70, 3, 1, 'Git', 'Published', DATE '2026-01-01', DATE '2026-12-31');
INSERT INTO quizzes (id, title, description, duration_minutes, passing_score, max_attempts, randomize, category, status, open_date, close_date)
VALUES (7, 'REST API Quiz', 'RESTful API concepts', 25, 65, 2, 1, 'API', 'Published', DATE '2026-01-01', DATE '2026-12-31');
INSERT INTO quizzes (id, title, description, duration_minutes, passing_score, max_attempts, randomize, category, status, open_date, close_date)
VALUES (8, 'Cloud Computing Quiz', 'AWS fundamentals', 30, 60, 3, 1, 'Cloud', 'Published', DATE '2026-01-01', DATE '2026-12-31');
INSERT INTO quizzes (id, title, description, duration_minutes, passing_score, max_attempts, randomize, category, status, open_date, close_date)
VALUES (9, 'TypeScript Quiz', 'TypeScript basics', 25, 65, 2, 1, 'JavaScript', 'Published', DATE '2026-01-01', DATE '2026-12-31');
INSERT INTO quizzes (id, title, description, duration_minutes, passing_score, max_attempts, randomize, category, status, open_date, close_date)
VALUES (10, 'Software Testing Quiz', 'Testing methodologies', 30, 60, 3, 0, 'Testing', 'Published', DATE '2026-01-01', DATE '2026-12-31');

-- =====================================================
-- 13. QUIZ_QUESTIONS
-- =====================================================
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (1, 1, 1);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (1, 2, 2);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (1, 3, 3);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (2, 4, 1);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (2, 5, 2);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (3, 6, 1);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (3, 7, 2);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (3, 8, 3);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (4, 9, 1);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (4, 10, 2);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (4, 11, 3);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (4, 12, 4);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (5, 13, 1);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (5, 14, 2);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (5, 18, 3);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (6, 15, 1);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (7, 16, 1);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (7, 17, 2);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (8, 20, 1);
INSERT INTO quiz_questions (quiz_id, question_id, sort_order) VALUES (9, 19, 1);

-- =====================================================
-- 14. TRAINING_SESSIONS (20 sessions)
-- =====================================================
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (1, 1, 'Java Day 1 - Introduction', 'Introduction to Java programming', 2, 'Room A101', DATE '2026-01-15', TIMESTAMP '2026-01-15 08:00:00', TIMESTAMP '2026-01-15 12:00:00', 'Offline', 30, 10, 'Completed');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (2, 1, 'Java Day 2 - Variables', 'Variables and data types', 2, 'Room A101', DATE '2026-01-16', TIMESTAMP '2026-01-16 08:00:00', TIMESTAMP '2026-01-16 12:00:00', 'Offline', 30, 10, 'Completed');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (3, 1, 'Java Day 3 - Control Flow', 'If-else and loops', 2, 'Room A101', DATE '2026-01-17', TIMESTAMP '2026-01-17 08:00:00', TIMESTAMP '2026-01-17 12:00:00', 'Hybrid', 30, 10, 'Completed');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (4, 1, 'Java Day 4 - OOP Part 1', 'Classes and Objects', 2, 'Room A101', DATE '2026-01-20', TIMESTAMP '2026-01-20 08:00:00', TIMESTAMP '2026-01-20 12:00:00', 'Offline', 30, 10, 'Upcoming');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (5, 1, 'Java Day 5 - OOP Part 2', 'Inheritance and Polymorphism', 2, NULL, DATE '2026-01-21', TIMESTAMP '2026-01-21 08:00:00', TIMESTAMP '2026-01-21 12:00:00', 'Online', 30, 8, 'Upcoming');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (6, 3, 'React Day 1 - JSX', 'Introduction to JSX', 3, 'Room B201', DATE '2026-02-01', TIMESTAMP '2026-02-01 08:00:00', TIMESTAMP '2026-02-01 12:00:00', 'Offline', 25, 12, 'Completed');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (7, 3, 'React Day 2 - Components', 'Creating React components', 3, 'Room B201', DATE '2026-02-02', TIMESTAMP '2026-02-02 08:00:00', TIMESTAMP '2026-02-02 12:00:00', 'Offline', 25, 12, 'Completed');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (8, 3, 'React Day 3 - State', 'State management with hooks', 3, NULL, DATE '2026-02-03', TIMESTAMP '2026-02-03 08:00:00', TIMESTAMP '2026-02-03 12:00:00', 'Online', 25, 11, 'Upcoming');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (9, 4, 'DevOps Day 1 - Docker Intro', 'Introduction to Docker', 4, 'Room C301', DATE '2026-01-20', TIMESTAMP '2026-01-20 18:00:00', TIMESTAMP '2026-01-20 21:00:00', 'Offline', 25, 15, 'Completed');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (10, 4, 'DevOps Day 2 - Dockerfile', 'Writing Dockerfiles', 4, 'Room C301', DATE '2026-01-22', TIMESTAMP '2026-01-22 18:00:00', TIMESTAMP '2026-01-22 21:00:00', 'Hybrid', 25, 14, 'Completed');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (11, 5, 'Data Science Day 1', 'Python basics for DS', 2, 'Room A102', DATE '2026-02-15', TIMESTAMP '2026-02-15 08:00:00', TIMESTAMP '2026-02-15 12:00:00', 'Offline', 30, 20, 'Upcoming');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (12, 7, 'QA Day 1 - Testing Basics', 'Introduction to testing', 3, 'Room B202', DATE '2026-03-01', TIMESTAMP '2026-03-01 08:00:00', TIMESTAMP '2026-03-01 12:00:00', 'Offline', 30, 18, 'Upcoming');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (13, 8, 'Backend Day 1', 'API design principles', 4, 'Room C302', DATE '2026-02-20', TIMESTAMP '2026-02-20 13:00:00', TIMESTAMP '2026-02-20 17:00:00', 'Hybrid', 25, 15, 'Upcoming');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (14, 10, 'PM Day 1 - Agile', 'Agile methodology', 2, 'Room D401', DATE '2026-03-15', TIMESTAMP '2026-03-15 18:00:00', TIMESTAMP '2026-03-15 21:00:00', 'Online', 35, 25, 'Upcoming');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (15, 12, 'Node Day 1', 'Node.js basics', 4, 'Room E501', DATE '2026-03-01', TIMESTAMP '2026-03-01 08:00:00', TIMESTAMP '2026-03-01 12:00:00', 'Offline', 25, 18, 'Upcoming');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (16, 13, 'Fresher Orientation', 'Program overview', 2, 'Auditorium', DATE '2026-01-05', TIMESTAMP '2026-01-05 08:00:00', TIMESTAMP '2026-01-05 17:00:00', 'Offline', 50, 35, 'Completed');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (17, 13, 'Fresher Day 2', 'Basic programming', 3, 'Room A101', DATE '2026-01-06', TIMESTAMP '2026-01-06 08:00:00', TIMESTAMP '2026-01-06 12:00:00', 'Offline', 40, 35, 'Completed');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (18, 15, 'Microservices Day 1', 'Architecture overview', 4, 'Room F601', DATE '2026-02-01', TIMESTAMP '2026-02-01 18:00:00', TIMESTAMP '2026-02-01 21:00:00', 'Hybrid', 20, 15, 'Completed');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (19, 17, 'System Design Day 1', 'Design principles', 2, 'Room G701', DATE '2026-03-20', TIMESTAMP '2026-03-20 13:00:00', TIMESTAMP '2026-03-20 17:00:00', 'Online', 30, 22, 'Upcoming');
INSERT INTO training_sessions (id, class_id, title, description, trainer_id, room, session_date, start_time, end_time, session_type, capacity, enrolled_count, status)
VALUES (20, 1, 'Java Quiz Session', 'Mid-term quiz', 2, 'Room A101', DATE '2026-02-15', TIMESTAMP '2026-02-15 08:00:00', TIMESTAMP '2026-02-15 10:00:00', 'Offline', 30, 10, 'Upcoming');

-- =====================================================
-- 15. QUIZ_ASSIGNMENTS
-- =====================================================
INSERT INTO quiz_assignments (id, quiz_id, class_id, training_session_id, assigned_by) VALUES (1, 1, 1, NULL, 2);
INSERT INTO quiz_assignments (id, quiz_id, class_id, training_session_id, assigned_by) VALUES (2, 2, 1, NULL, 2);
INSERT INTO quiz_assignments (id, quiz_id, class_id, training_session_id, assigned_by) VALUES (3, 3, 3, NULL, 3);
INSERT INTO quiz_assignments (id, quiz_id, class_id, training_session_id, assigned_by) VALUES (4, 5, 4, NULL, 4);
INSERT INTO quiz_assignments (id, quiz_id, class_id, training_session_id, assigned_by) VALUES (5, 4, 5, NULL, 2);
INSERT INTO quiz_assignments (id, quiz_id, class_id, training_session_id, assigned_by) VALUES (6, 6, 4, NULL, 4);
INSERT INTO quiz_assignments (id, quiz_id, class_id, training_session_id, assigned_by) VALUES (7, 7, 8, NULL, 4);
INSERT INTO quiz_assignments (id, quiz_id, class_id, training_session_id, assigned_by) VALUES (8, 8, NULL, 11, 2);
INSERT INTO quiz_assignments (id, quiz_id, class_id, training_session_id, assigned_by) VALUES (9, 9, NULL, 18, 4);
INSERT INTO quiz_assignments (id, quiz_id, class_id, training_session_id, assigned_by) VALUES (10, 10, 7, NULL, 3);

-- =====================================================
-- 16. TRAINING_REGISTRATIONS
-- =====================================================
INSERT INTO training_registrations (id, training_id, user_id, status, completed_at) VALUES (1, 1, 7, 'Completed', TIMESTAMP '2026-01-15 12:00:00');
INSERT INTO training_registrations (id, training_id, user_id, status, completed_at) VALUES (2, 1, 8, 'Completed', TIMESTAMP '2026-01-15 12:00:00');
INSERT INTO training_registrations (id, training_id, user_id, status, completed_at) VALUES (3, 1, 9, 'Completed', TIMESTAMP '2026-01-15 12:00:00');
INSERT INTO training_registrations (id, training_id, user_id, status, completed_at) VALUES (4, 2, 7, 'Completed', TIMESTAMP '2026-01-16 12:00:00');
INSERT INTO training_registrations (id, training_id, user_id, status, completed_at) VALUES (5, 2, 8, 'Completed', TIMESTAMP '2026-01-16 12:00:00');
INSERT INTO training_registrations (id, training_id, user_id, status, completed_at) VALUES (6, 2, 9, 'Completed', TIMESTAMP '2026-01-16 12:00:00');
INSERT INTO training_registrations (id, training_id, user_id, status) VALUES (7, 4, 7, 'Registered');
INSERT INTO training_registrations (id, training_id, user_id, status) VALUES (8, 4, 8, 'Registered');
INSERT INTO training_registrations (id, training_id, user_id, status) VALUES (9, 4, 9, 'Registered');
INSERT INTO training_registrations (id, training_id, user_id, status) VALUES (10, 5, 7, 'Registered');
INSERT INTO training_registrations (id, training_id, user_id, status) VALUES (11, 5, 8, 'Registered');
INSERT INTO training_registrations (id, training_id, user_id, status, completed_at) VALUES (12, 6, 10, 'Completed', TIMESTAMP '2026-02-01 12:00:00');
INSERT INTO training_registrations (id, training_id, user_id, status, completed_at) VALUES (13, 6, 11, 'Completed', TIMESTAMP '2026-02-01 12:00:00');
INSERT INTO training_registrations (id, training_id, user_id, status, completed_at) VALUES (14, 6, 12, 'Completed', TIMESTAMP '2026-02-01 12:00:00');
INSERT INTO training_registrations (id, training_id, user_id, status) VALUES (15, 8, 10, 'Registered');
INSERT INTO training_registrations (id, training_id, user_id, status) VALUES (16, 8, 11, 'Registered');
INSERT INTO training_registrations (id, training_id, user_id, status, completed_at) VALUES (17, 9, 13, 'Completed', TIMESTAMP '2026-01-20 21:00:00');
INSERT INTO training_registrations (id, training_id, user_id, status, completed_at) VALUES (18, 9, 14, 'Completed', TIMESTAMP '2026-01-20 21:00:00');
INSERT INTO training_registrations (id, training_id, user_id, status, cancelled_at) VALUES (19, 11, 15, 'Cancelled', TIMESTAMP '2026-02-10 10:00:00');
INSERT INTO training_registrations (id, training_id, user_id, status) VALUES (20, 11, 16, 'Registered');

-- =====================================================
-- 17. ATTENDANCE_RECORDS
-- =====================================================
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method) VALUES (1, 1, 7, 'Present', TIMESTAMP '2026-01-15 07:55:00', 'QR');
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method) VALUES (2, 1, 8, 'Present', TIMESTAMP '2026-01-15 07:58:00', 'QR');
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method, updated_by) VALUES (3, 1, 9, 'Late', TIMESTAMP '2026-01-15 08:15:00', 'Manual', 2);
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method) VALUES (4, 2, 7, 'Present', TIMESTAMP '2026-01-16 07:50:00', 'QR');
INSERT INTO attendance_records (id, training_id, user_id, status, check_in_method, updated_by) VALUES (5, 2, 8, 'Absent', 'Manual', 2);
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method) VALUES (6, 2, 9, 'Present', TIMESTAMP '2026-01-16 07:59:00', 'QR');
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method) VALUES (7, 3, 7, 'Present', TIMESTAMP '2026-01-17 07:45:00', 'QR');
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method, updated_by) VALUES (8, 3, 8, 'Present', TIMESTAMP '2026-01-17 07:55:00', 'Manual', 2);
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method) VALUES (9, 3, 9, 'Present', TIMESTAMP '2026-01-17 07:58:00', 'QR');
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method) VALUES (10, 6, 10, 'Present', TIMESTAMP '2026-02-01 07:50:00', 'QR');
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method, updated_by) VALUES (11, 6, 11, 'Late', TIMESTAMP '2026-02-01 08:20:00', 'Manual', 3);
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method) VALUES (12, 6, 12, 'Present', TIMESTAMP '2026-02-01 07:55:00', 'QR');
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method) VALUES (13, 7, 10, 'Present', TIMESTAMP '2026-02-02 07:58:00', 'QR');
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method) VALUES (14, 7, 11, 'Present', TIMESTAMP '2026-02-02 07:55:00', 'QR');
INSERT INTO attendance_records (id, training_id, user_id, status, check_in_method, updated_by) VALUES (15, 7, 12, 'Absent', 'Manual', 3);
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method, updated_by) VALUES (16, 9, 13, 'Present', TIMESTAMP '2026-01-20 17:55:00', 'Manual', 4);
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method, updated_by) VALUES (17, 9, 14, 'Present', TIMESTAMP '2026-01-20 17:58:00', 'Manual', 4);
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method) VALUES (18, 10, 13, 'Present', TIMESTAMP '2026-01-22 17:50:00', 'QR');
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method, updated_by) VALUES (19, 10, 14, 'Late', TIMESTAMP '2026-01-22 18:30:00', 'Manual', 4);
INSERT INTO attendance_records (id, training_id, user_id, status, checked_in_at, check_in_method) VALUES (20, 16, 19, 'Present', TIMESTAMP '2026-01-05 07:45:00', 'Manual');

-- =====================================================
-- 18. QUIZ_ATTEMPTS
-- =====================================================
INSERT INTO quiz_attempts (id, quiz_id, user_id, attempt_number, answers_json, score, correct_count, total_questions, passed, time_taken_seconds)
VALUES (1, 1, 7, 1, '[{"questionId":1,"answer":["Platform independent"]},{"questionId":2,"answer":["extends"]},{"questionId":3,"answer":["int","boolean","double"]}]', 100, 3, 3, 1, 1200);
INSERT INTO quiz_attempts (id, quiz_id, user_id, attempt_number, answers_json, score, correct_count, total_questions, passed, time_taken_seconds)
VALUES (2, 1, 8, 1, '[{"questionId":1,"answer":["Platform dependent"]},{"questionId":2,"answer":["extends"]},{"questionId":3,"answer":["int","boolean"]}]', 50, 1, 3, 0, 1500);
INSERT INTO quiz_attempts (id, quiz_id, user_id, attempt_number, answers_json, score, correct_count, total_questions, passed, time_taken_seconds)
VALUES (3, 1, 8, 2, '[{"questionId":1,"answer":["Platform independent"]},{"questionId":2,"answer":["extends"]},{"questionId":3,"answer":["int","boolean","double"]}]', 100, 3, 3, 1, 900);
INSERT INTO quiz_attempts (id, quiz_id, user_id, attempt_number, answers_json, score, correct_count, total_questions, passed, time_taken_seconds)
VALUES (4, 1, 9, 1, '[{"questionId":1,"answer":["Platform independent"]},{"questionId":2,"answer":["implements"]},{"questionId":3,"answer":["int","double"]}]', 33, 1, 3, 0, 1800);
INSERT INTO quiz_attempts (id, quiz_id, user_id, attempt_number, answers_json, score, correct_count, total_questions, passed, time_taken_seconds)
VALUES (5, 3, 10, 1, '[{"questionId":6,"answer":["A JavaScript library for UI"]},{"questionId":7,"answer":["JavaScript XML"]},{"questionId":8,"answer":["useState","useReducer","useContext"]}]', 100, 3, 3, 1, 1100);
INSERT INTO quiz_attempts (id, quiz_id, user_id, attempt_number, answers_json, score, correct_count, total_questions, passed, time_taken_seconds)
VALUES (6, 3, 11, 1, '[{"questionId":6,"answer":["A JavaScript library for UI"]},{"questionId":7,"answer":["JSON syntax"]},{"questionId":8,"answer":["useState"]}]', 33, 1, 3, 0, 1400);
INSERT INTO quiz_attempts (id, quiz_id, user_id, attempt_number, answers_json, score, correct_count, total_questions, passed, time_taken_seconds)
VALUES (7, 5, 13, 1, '[{"questionId":13,"answer":["A containerization platform"]},{"questionId":14,"answer":["Continuous Integration/Continuous Delivery"]},{"questionId":18,"answer":["A container orchestration platform"]}]', 100, 3, 3, 1, 1600);
INSERT INTO quiz_attempts (id, quiz_id, user_id, attempt_number, answers_json, score, correct_count, total_questions, passed, time_taken_seconds)
VALUES (8, 5, 14, 1, '[{"questionId":13,"answer":["A containerization platform"]},{"questionId":14,"answer":["Code Integration/Code Delivery"]},{"questionId":18,"answer":["A programming language"]}]', 33, 1, 3, 0, 2000);
INSERT INTO quiz_attempts (id, quiz_id, user_id, attempt_number, answers_json, score, correct_count, total_questions, passed, time_taken_seconds)
VALUES (9, 2, 7, 1, '[{"questionId":4,"answer":["A Java framework for building apps"]},{"questionId":5,"answer":["@SpringBootApplication"]}]', 100, 2, 2, 1, 2200);
INSERT INTO quiz_attempts (id, quiz_id, user_id, attempt_number, answers_json, score, correct_count, total_questions, passed, time_taken_seconds)
VALUES (10, 4, 15, 1, '[{"questionId":9,"answer":["Structured Query Language"]},{"questionId":10,"answer":["SELECT"]},{"questionId":11,"answer":["Removing redundancy"]},{"questionId":12,"answer":["Atomicity","Consistency","Isolation","Durability"]}]', 100, 4, 4, 1, 1900);

-- =====================================================
-- 19. NOTIFICATIONS
-- =====================================================
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (1, 7, 'Welcome to FAP', 'Welcome to the FPT Academy Portal. Start your learning journey today!', 1);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (2, 7, 'New Quiz Available', 'A new quiz "Java Basics Quiz" has been assigned to your class.', 1);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (3, 7, 'Session Reminder', 'Reminder: Java Day 4 - OOP Part 1 starts tomorrow at 8:00 AM.', 0);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (4, 8, 'Welcome to FAP', 'Welcome to the FPT Academy Portal. Start your learning journey today!', 1);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (5, 8, 'Quiz Result', 'You have passed the Java Basics Quiz with score 100%.', 1);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (6, 9, 'Welcome to FAP', 'Welcome to the FPT Academy Portal.', 1);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (7, 9, 'Attendance Warning', 'You were marked late for Java Day 1 session.', 0);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (8, 10, 'Welcome to FAP', 'Welcome to the FPT Academy Portal.', 1);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (9, 10, 'Quiz Available', 'React Fundamentals quiz is now available.', 0);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (10, 2, 'New Class Assignment', 'You have been assigned as trainer for Java Batch 2026-01.', 1);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (11, 2, 'Session Created', 'Your training session "Java Day 4 - OOP Part 1" has been scheduled.', 1);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (12, 3, 'New Class Assignment', 'You have been assigned as trainer for Frontend Batch 01.', 1);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (13, 5, 'Class Admin Assignment', 'You are now admin for Java Batch 2026-01.', 1);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (14, 5, 'Attendance Report', 'Daily attendance report for Java Batch 2026-01 is ready.', 0);
INSERT INTO notifications (id, user_id, title, message, is_read) VALUES (15, 11, 'Quiz Failed', 'You did not pass React Fundamentals. You have 2 attempts remaining.', 0);

-- =====================================================
-- 20. AUDIT_LOGS
-- =====================================================
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, ip_address) VALUES (1, 2, 'User login', 'User', 2, '192.168.1.101');
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, ip_address) VALUES (2, 3, 'User login', 'User', 3, '192.168.1.102');
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, ip_address) VALUES (3, 7, 'User login', 'User', 7, '192.168.1.150');
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, ip_address) VALUES (4, 7, 'Quiz attempt submitted: Java Basics Quiz', 'QuizAttempt', 1, '192.168.1.150');
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, ip_address) VALUES (5, 8, 'User login', 'User', 8, '192.168.1.151');
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, ip_address) VALUES (6, 8, 'Quiz attempt submitted: Java Basics Quiz (Attempt 1)', 'QuizAttempt', 2, '192.168.1.151');
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, ip_address) VALUES (7, 8, 'Quiz attempt submitted: Java Basics Quiz (Attempt 2)', 'QuizAttempt', 3, '192.168.1.151');
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, ip_address) VALUES (8, 2, 'Attendance recorded: Present for user 7', 'AttendanceRecord', 1, '192.168.1.101');
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, ip_address) VALUES (9, 2, 'Attendance updated: Late for user 9', 'AttendanceRecord', 3, '192.168.1.101');
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, ip_address) VALUES (10, 3, 'Training session completed: React Day 1', 'TrainingSession', 6, '192.168.1.102');

-- =====================================================
-- 21. SYSTEM_SETTINGS
-- =====================================================
INSERT INTO system_settings (id, category, setting_key, setting_value) VALUES (1, 'general', 'site_name', 'FPT Academy Portal');
INSERT INTO system_settings (id, category, setting_key, setting_value) VALUES (2, 'general', 'maintenance_mode', 'false');
INSERT INTO system_settings (id, category, setting_key, setting_value) VALUES (3, 'email', 'smtp_host', 'smtp.fap.edu.vn');
INSERT INTO system_settings (id, category, setting_key, setting_value) VALUES (4, 'email', 'smtp_port', '587');
INSERT INTO system_settings (id, category, setting_key, setting_value) VALUES (5, 'attendance', 'late_threshold_minutes', '15');
INSERT INTO system_settings (id, category, setting_key, setting_value) VALUES (6, 'attendance', 'qr_expiry_seconds', '300');
INSERT INTO system_settings (id, category, setting_key, setting_value) VALUES (7, 'quiz', 'default_passing_score', '60');
INSERT INTO system_settings (id, category, setting_key, setting_value) VALUES (8, 'quiz', 'max_attempts_default', '3');
INSERT INTO system_settings (id, category, setting_key, setting_value) VALUES (9, 'notification', 'email_enabled', 'true');
INSERT INTO system_settings (id, category, setting_key, setting_value) VALUES (10, 'notification', 'push_enabled', 'false');

-- =====================================================
-- 22. MATERIAL_FILES
-- =====================================================
INSERT INTO material_files (id, topic_id, file_name, file_url, file_size, content_type, uploaded_by)
VALUES (1, 1, 'JDK_Installation_Guide.pdf', '/materials/java/jdk-install-guide.pdf', 2048000, 'application/pdf', 2);
INSERT INTO material_files (id, topic_id, file_name, file_url, file_size, content_type, uploaded_by)
VALUES (2, 2, 'HelloWorld_Example.java', '/materials/java/hello-world.java', 1024, 'text/x-java-source', 2);
INSERT INTO material_files (id, topic_id, file_name, file_url, file_size, content_type, uploaded_by)
VALUES (3, 3, 'Primitive_Types_Cheatsheet.pdf', '/materials/java/primitives-cheatsheet.pdf', 512000, 'application/pdf', 2);
INSERT INTO material_files (id, topic_id, file_name, file_url, file_size, content_type, uploaded_by)
VALUES (4, 4, 'Control_Flow_Slides.pptx', '/materials/java/control-flow-slides.pptx', 3072000, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 2);
INSERT INTO material_files (id, topic_id, file_name, file_url, file_size, content_type, uploaded_by)
VALUES (5, 5, 'Loops_Practice_Exercises.pdf', '/materials/java/loops-exercises.pdf', 768000, 'application/pdf', 2);
INSERT INTO material_files (id, topic_id, file_name, file_url, file_size, content_type, uploaded_by)
VALUES (6, 7, 'OOP_Fundamentals.pdf', '/materials/java/oop-fundamentals.pdf', 4096000, 'application/pdf', 2);
INSERT INTO material_files (id, topic_id, file_name, file_url, file_size, content_type, uploaded_by)
VALUES (7, 8, 'Spring_Initializr_Tutorial.mp4', '/materials/spring/spring-initializr.mp4', 52428800, 'video/mp4', 3);
INSERT INTO material_files (id, topic_id, file_name, file_url, file_size, content_type, uploaded_by)
VALUES (8, 9, 'Project_Structure_Guide.pdf', '/materials/spring/project-structure.pdf', 1536000, 'application/pdf', 3);
INSERT INTO material_files (id, topic_id, file_name, file_url, file_size, content_type, uploaded_by)
VALUES (9, 10, 'REST_API_Examples.zip', '/materials/spring/rest-api-examples.zip', 10485760, 'application/zip', 3);
INSERT INTO material_files (id, topic_id, file_name, file_url, file_size, content_type, uploaded_by)
VALUES (10, 11, 'JSX_Introduction.pdf', '/materials/react/jsx-intro.pdf', 1024000, 'application/pdf', 3);

COMMIT;

