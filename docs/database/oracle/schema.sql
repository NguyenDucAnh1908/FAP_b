-- FAP Backend v1 Oracle schema
-- Target: Oracle Database 19c+
-- Source: docs/01_database_schema.md + docs/07_scope_freeze.md

CREATE TABLE users (
                       id NUMBER(19) PRIMARY KEY,
                       full_name VARCHAR2(255) NOT NULL,
                       email VARCHAR2(255) NOT NULL,
                       phone VARCHAR2(20),
                       password_hash VARCHAR2(255) NOT NULL,
                       date_of_birth DATE,
                       gender VARCHAR2(10) DEFAULT 'Male' NOT NULL,
                       avatar_url VARCHAR2(512),
                       status VARCHAR2(20) DEFAULT 'Active' NOT NULL,
                       is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
                       deleted_at TIMESTAMP,
                       version_no NUMBER(19) DEFAULT 0 NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       created_by NUMBER(19),
                       updated_by NUMBER(19),
                       CONSTRAINT uk_users_email UNIQUE (email),
                       CONSTRAINT ck_users_gender CHECK (gender IN ('Male', 'Female')),
                       CONSTRAINT ck_users_status CHECK (status IN ('Active', 'Inactive')),
                       CONSTRAINT ck_users_deleted CHECK (is_deleted IN (0, 1))
);

ALTER TABLE users ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE users ADD CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users(id);

CREATE TABLE roles (
                       id NUMBER(19) PRIMARY KEY,
                       name VARCHAR2(50) NOT NULL,
                       description VARCHAR2(255),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE user_roles (
                            user_id NUMBER(19) NOT NULL,
                            role_id NUMBER(19) NOT NULL,
                            CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
                            CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE permissions (
                             id NUMBER(19) PRIMARY KEY,
                             role_id NUMBER(19) NOT NULL,
                             resource_name VARCHAR2(50) NOT NULL,
                             permission_level VARCHAR2(30) NOT NULL,
                             CONSTRAINT fk_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id),
                             CONSTRAINT uk_permissions_role_res UNIQUE (role_id, resource_name),
                             CONSTRAINT ck_permissions_resource CHECK (resource_name IN ('syllabus', 'training_program', 'class', 'learning_material', 'user', 'quiz')),
                             CONSTRAINT ck_permissions_level CHECK (permission_level IN ('access_denied', 'view', 'create', 'modify', 'full_access'))
);

CREATE TABLE syllabuses (
                            id NUMBER(19) PRIMARY KEY,
                            name VARCHAR2(255) NOT NULL,
                            code VARCHAR2(50) NOT NULL,
                            version VARCHAR2(20) DEFAULT 'v1.0' NOT NULL,
                            status VARCHAR2(20) DEFAULT 'Drafting' NOT NULL,
                            level_name VARCHAR2(30) DEFAULT 'All levels' NOT NULL,
                            attendees NUMBER(10) DEFAULT 30 NOT NULL,
                            duration VARCHAR2(50),
                            technical_requirements CLOB,
                            course_objectives CLOB,
                            rules CLOB,
                            time_alloc_assignment_lab NUMBER(3) DEFAULT 50 NOT NULL,
                            time_alloc_concept_lecture NUMBER(3) DEFAULT 30 NOT NULL,
                            time_alloc_guide_review NUMBER(3) DEFAULT 10 NOT NULL,
                            time_alloc_test_quiz NUMBER(3) DEFAULT 10 NOT NULL,
                            assess_quiz_pct NUMBER(3) DEFAULT 15 NOT NULL,
                            assess_assignment_pct NUMBER(3) DEFAULT 15 NOT NULL,
                            assess_final_pct NUMBER(3) DEFAULT 70 NOT NULL,
                            assessment_text CLOB,
                            is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
                            deleted_at TIMESTAMP,
                            version_no NUMBER(19) DEFAULT 0 NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                            created_by NUMBER(19),
                            updated_by NUMBER(19),
                            CONSTRAINT uk_syllabuses_code UNIQUE (code),
                            CONSTRAINT fk_syll_created_by FOREIGN KEY (created_by) REFERENCES users(id),
                            CONSTRAINT fk_syll_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
                            CONSTRAINT ck_syll_status CHECK (status IN ('Drafting', 'Pending', 'Active', 'Inactive')),
                            CONSTRAINT ck_syll_level CHECK (level_name IN ('Beginner', 'Intermediate', 'Advanced', 'All levels')),
                            CONSTRAINT ck_syll_deleted CHECK (is_deleted IN (0, 1)),
                            CONSTRAINT ck_syll_attendees CHECK (attendees > 0),
                            CONSTRAINT ck_syll_time_total CHECK (
                                time_alloc_assignment_lab + time_alloc_concept_lecture + time_alloc_guide_review + time_alloc_test_quiz = 100
                                ),
                            CONSTRAINT ck_syll_assess_total CHECK (assess_quiz_pct + assess_assignment_pct + assess_final_pct = 100)
);

CREATE TABLE syllabus_output_standards (
                                           syllabus_id NUMBER(19) NOT NULL,
                                           standard_code VARCHAR2(10) NOT NULL,
                                           CONSTRAINT pk_syll_output PRIMARY KEY (syllabus_id, standard_code),
                                           CONSTRAINT fk_syll_output_syll FOREIGN KEY (syllabus_id) REFERENCES syllabuses(id) ON DELETE CASCADE,
                                           CONSTRAINT ck_syll_output_code CHECK (standard_code IN ('H4SD', 'K6SD', 'H1SD', 'C3SD', 'H2SD'))
);

CREATE TABLE syllabus_days (
                               id NUMBER(19) PRIMARY KEY,
                               syllabus_id NUMBER(19) NOT NULL,
                               day_number NUMBER(10) NOT NULL,
                               sort_order NUMBER(10) NOT NULL,
                               CONSTRAINT fk_syll_days_syll FOREIGN KEY (syllabus_id) REFERENCES syllabuses(id) ON DELETE CASCADE,
                               CONSTRAINT uk_syll_days_day UNIQUE (syllabus_id, day_number),
                               CONSTRAINT uk_syll_days_sort UNIQUE (syllabus_id, sort_order),
                               CONSTRAINT ck_syll_days_num CHECK (day_number > 0),
                               CONSTRAINT ck_syll_days_sort CHECK (sort_order > 0)
);

CREATE TABLE syllabus_units (
                                id NUMBER(19) PRIMARY KEY,
                                day_id NUMBER(19) NOT NULL,
                                name VARCHAR2(255) NOT NULL,
                                sort_order NUMBER(10) NOT NULL,
                                CONSTRAINT fk_syll_units_day FOREIGN KEY (day_id) REFERENCES syllabus_days(id) ON DELETE CASCADE,
                                CONSTRAINT uk_syll_units_sort UNIQUE (day_id, sort_order),
                                CONSTRAINT ck_syll_units_sort CHECK (sort_order > 0)
);

CREATE TABLE syllabus_topics (
                                 id NUMBER(19) PRIMARY KEY,
                                 unit_id NUMBER(19) NOT NULL,
                                 name VARCHAR2(255) NOT NULL,
                                 output_standard VARCHAR2(10) NOT NULL,
                                 is_online NUMBER(1) DEFAULT 1 NOT NULL,
                                 duration_minutes NUMBER(10) DEFAULT 30 NOT NULL,
                                 status VARCHAR2(20) DEFAULT 'Active' NOT NULL,
                                 sort_order NUMBER(10) NOT NULL,
                                 CONSTRAINT fk_syll_topics_unit FOREIGN KEY (unit_id) REFERENCES syllabus_units(id) ON DELETE CASCADE,
                                 CONSTRAINT uk_syll_topics_sort UNIQUE (unit_id, sort_order),
                                 CONSTRAINT ck_syll_topics_online CHECK (is_online IN (0, 1)),
                                 CONSTRAINT ck_syll_topics_duration CHECK (duration_minutes > 0),
                                 CONSTRAINT ck_syll_topics_status CHECK (status IN ('Active', 'Inactive')),
                                 CONSTRAINT ck_syll_topics_output CHECK (output_standard IN ('H4SD', 'K6SD', 'H1SD', 'C3SD', 'H2SD'))
);

CREATE TABLE material_files (
                                id NUMBER(19) PRIMARY KEY,
                                topic_id NUMBER(19) NOT NULL,
                                file_name VARCHAR2(255) NOT NULL,
                                file_url VARCHAR2(512) NOT NULL,
                                file_size NUMBER(19),
                                content_type VARCHAR2(100),
                                uploaded_by NUMBER(19),
                                uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                CONSTRAINT fk_material_topic FOREIGN KEY (topic_id) REFERENCES syllabus_topics(id) ON DELETE CASCADE,
                                CONSTRAINT fk_material_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id),
                                CONSTRAINT ck_material_size CHECK (file_size IS NULL OR file_size > 0)
);

CREATE TABLE training_programs (
                                   id NUMBER(19) PRIMARY KEY,
                                   name VARCHAR2(255) NOT NULL,
                                   status VARCHAR2(20) DEFAULT 'Planning' NOT NULL,
                                   duration VARCHAR2(50),
                                   total_hours NUMBER(10),
                                   version VARCHAR2(20) DEFAULT 'v1.0',
                                   is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
                                   deleted_at TIMESTAMP,
                                   version_no NUMBER(19) DEFAULT 0 NOT NULL,
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                   created_by NUMBER(19),
                                   updated_by NUMBER(19),
                                   CONSTRAINT fk_tp_created_by FOREIGN KEY (created_by) REFERENCES users(id),
                                   CONSTRAINT fk_tp_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
                                   CONSTRAINT ck_tp_status CHECK (status IN ('Planning', 'Active', 'Inactive')),
                                   CONSTRAINT ck_tp_deleted CHECK (is_deleted IN (0, 1)),
                                   CONSTRAINT ck_tp_hours CHECK (total_hours IS NULL OR total_hours >= 0)
);

CREATE TABLE training_program_syllabuses (
                                             program_id NUMBER(19) NOT NULL,
                                             syllabus_id NUMBER(19) NOT NULL,
                                             sort_order NUMBER(10) NOT NULL,
                                             CONSTRAINT pk_tp_syll PRIMARY KEY (program_id, syllabus_id),
                                             CONSTRAINT fk_tp_syll_program FOREIGN KEY (program_id) REFERENCES training_programs(id) ON DELETE CASCADE,
                                             CONSTRAINT fk_tp_syll_syllabus FOREIGN KEY (syllabus_id) REFERENCES syllabuses(id),
                                             CONSTRAINT uk_tp_syll_sort UNIQUE (program_id, sort_order),
                                             CONSTRAINT ck_tp_syll_sort CHECK (sort_order > 0)
);

CREATE TABLE classes (
                         id NUMBER(19) PRIMARY KEY,
                         name VARCHAR2(255) NOT NULL,
                         class_code VARCHAR2(100) NOT NULL,
                         training_program_id NUMBER(19) NOT NULL,
                         status VARCHAR2(20) DEFAULT 'Planning' NOT NULL,
                         location VARCHAR2(100),
                         location_detail VARCHAR2(255),
                         fsu VARCHAR2(20),
                         class_time VARCHAR2(50),
                         start_date DATE,
                         end_date DATE,
                         duration VARCHAR2(50),
                         capacity NUMBER(10) DEFAULT 30 NOT NULL,
                         self_enrollment_enabled NUMBER(1) DEFAULT 0 NOT NULL,
                         enrollment_start_date DATE,
                         enrollment_end_date DATE,
                         minimum_attendance_rate NUMBER(5,2) DEFAULT 80 NOT NULL,
                         is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
                         deleted_at TIMESTAMP,
                         version_no NUMBER(19) DEFAULT 0 NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         created_by NUMBER(19),
                         updated_by NUMBER(19),
                         CONSTRAINT uk_classes_code UNIQUE (class_code),
                         CONSTRAINT fk_classes_tp FOREIGN KEY (training_program_id) REFERENCES training_programs(id),
                         CONSTRAINT fk_classes_created_by FOREIGN KEY (created_by) REFERENCES users(id),
                         CONSTRAINT fk_classes_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
                         CONSTRAINT ck_classes_status CHECK (status IN ('Planning', 'Active', 'Closed')),
                         CONSTRAINT ck_classes_deleted CHECK (is_deleted IN (0, 1)),
                         CONSTRAINT ck_classes_dates CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date),
                         CONSTRAINT ck_classes_capacity CHECK (capacity > 0),
                         CONSTRAINT ck_classes_self_enrollment CHECK (self_enrollment_enabled IN (0, 1)),
                         CONSTRAINT ck_classes_enrollment_dates CHECK (
                             enrollment_start_date IS NULL OR enrollment_end_date IS NULL
                                 OR enrollment_start_date <= enrollment_end_date
                         ),
                         CONSTRAINT ck_classes_min_attendance CHECK (minimum_attendance_rate BETWEEN 0 AND 100)
);

CREATE TABLE class_trainers (
                                id NUMBER(19) PRIMARY KEY,
                                class_id NUMBER(19) NOT NULL,
                                user_id NUMBER(19) NOT NULL,
                                syllabus_id NUMBER(19),
                                CONSTRAINT fk_class_trainers_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
                                CONSTRAINT fk_class_trainers_user FOREIGN KEY (user_id) REFERENCES users(id),
                                CONSTRAINT fk_class_trainers_syll FOREIGN KEY (syllabus_id) REFERENCES syllabuses(id)
);

CREATE UNIQUE INDEX uk_class_trainers_scope ON class_trainers (
                                                               class_id,
                                                               user_id,
                                                               NVL(syllabus_id, -1)
    );

CREATE TABLE class_admins (
                              class_id NUMBER(19) NOT NULL,
                              user_id NUMBER(19) NOT NULL,
                              CONSTRAINT pk_class_admins PRIMARY KEY (class_id, user_id),
                              CONSTRAINT fk_class_admins_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
                              CONSTRAINT fk_class_admins_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE questions (
                           id NUMBER(19) PRIMARY KEY,
                           content CLOB NOT NULL,
                           question_type VARCHAR2(20) NOT NULL,
                           category VARCHAR2(100) NOT NULL,
                           difficulty VARCHAR2(20) NOT NULL,
                           options_json CLOB NOT NULL,
                           correct_answers_json CLOB NOT NULL,
                           explanation CLOB,
                           is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
                           deleted_at TIMESTAMP,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                           created_by NUMBER(19),
                           updated_by NUMBER(19),
                           CONSTRAINT fk_questions_created_by FOREIGN KEY (created_by) REFERENCES users(id),
                           CONSTRAINT fk_questions_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
                           CONSTRAINT ck_questions_type CHECK (question_type IN ('single', 'multiple')),
                           CONSTRAINT ck_questions_diff CHECK (difficulty IN ('Easy', 'Medium', 'Hard')),
                           CONSTRAINT ck_questions_deleted CHECK (is_deleted IN (0, 1))
);

CREATE TABLE quizzes (
                         id NUMBER(19) PRIMARY KEY,
                         title VARCHAR2(255) NOT NULL,
                         description CLOB,
                         duration_minutes NUMBER(10) NOT NULL,
                         passing_score NUMBER(3) NOT NULL,
                         max_attempts NUMBER(10) DEFAULT 1 NOT NULL,
                         randomize NUMBER(1) DEFAULT 0 NOT NULL,
                         category VARCHAR2(100) NOT NULL,
                         status VARCHAR2(20) DEFAULT 'Draft' NOT NULL,
                         open_date DATE,
                         close_date DATE,
                         is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
                         deleted_at TIMESTAMP,
                         version_no NUMBER(19) DEFAULT 0 NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         created_by NUMBER(19),
                         updated_by NUMBER(19),
                         CONSTRAINT fk_quizzes_created_by FOREIGN KEY (created_by) REFERENCES users(id),
                         CONSTRAINT fk_quizzes_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
                         CONSTRAINT ck_quizzes_duration CHECK (duration_minutes > 0),
                         CONSTRAINT ck_quizzes_score CHECK (passing_score BETWEEN 0 AND 100),
                         CONSTRAINT ck_quizzes_attempts CHECK (max_attempts > 0),
                         CONSTRAINT ck_quizzes_randomize CHECK (randomize IN (0, 1)),
                         CONSTRAINT ck_quizzes_status CHECK (status IN ('Draft', 'Published', 'Closed')),
                         CONSTRAINT ck_quizzes_deleted CHECK (is_deleted IN (0, 1)),
                         CONSTRAINT ck_quizzes_dates CHECK (open_date IS NULL OR close_date IS NULL OR open_date <= close_date)
);

CREATE TABLE quiz_questions (
                                quiz_id NUMBER(19) NOT NULL,
                                question_id NUMBER(19) NOT NULL,
                                sort_order NUMBER(10) NOT NULL,
                                points NUMBER(5,2) DEFAULT 1 NOT NULL,
                                CONSTRAINT pk_quiz_questions PRIMARY KEY (quiz_id, question_id),
                                CONSTRAINT fk_qq_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
                                CONSTRAINT fk_qq_question FOREIGN KEY (question_id) REFERENCES questions(id),
                                CONSTRAINT uk_qq_sort UNIQUE (quiz_id, sort_order),
                                CONSTRAINT ck_qq_sort CHECK (sort_order > 0),
                                CONSTRAINT ck_qq_points CHECK (points > 0)
);

CREATE TABLE class_enrollments (
                                    id NUMBER(19) PRIMARY KEY,
                                    class_id NUMBER(19) NOT NULL,
                                    user_id NUMBER(19) NOT NULL,
                                    status VARCHAR2(20) NOT NULL,
                                    source VARCHAR2(20) NOT NULL,
                                    enrolled_at TIMESTAMP,
                                    withdrawn_at TIMESTAMP,
                                    completed_at TIMESTAMP,
                                    reviewed_at TIMESTAMP,
                                    reviewed_by NUMBER(19),
                                    version_no NUMBER(19) DEFAULT 0 NOT NULL,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                    created_by NUMBER(19),
                                    updated_by NUMBER(19),
                                    CONSTRAINT fk_class_enrollments_class FOREIGN KEY (class_id) REFERENCES classes(id),
                                    CONSTRAINT fk_class_enrollments_user FOREIGN KEY (user_id) REFERENCES users(id),
                                    CONSTRAINT fk_class_enrollments_created_by FOREIGN KEY (created_by) REFERENCES users(id),
                                    CONSTRAINT fk_class_enrollments_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
                                    CONSTRAINT fk_class_enrollments_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id),
                                    CONSTRAINT uk_class_enrollments_class_user UNIQUE (class_id, user_id),
                                    CONSTRAINT ck_class_enrollments_status CHECK (status IN ('PendingApproval', 'Enrolled', 'Waitlisted', 'Rejected', 'Withdrawn', 'Completed')),
                                    CONSTRAINT ck_class_enrollments_source CHECK (source IN ('AdminAdded', 'SelfRegistered', 'Migration')),
                                    CONSTRAINT ck_class_enrollments_withdrawn CHECK ((status = 'Withdrawn' AND withdrawn_at IS NOT NULL) OR status <> 'Withdrawn'),
                                    CONSTRAINT ck_class_enrollments_completed CHECK ((status = 'Completed' AND completed_at IS NOT NULL) OR status <> 'Completed')
);

CREATE TABLE training_sessions (
                                   id NUMBER(19) PRIMARY KEY,
                                   class_id NUMBER(19),
                                   title VARCHAR2(255) NOT NULL,
                                   description CLOB,
                                   trainer_id NUMBER(19),
                                   room VARCHAR2(100),
                                   session_date DATE NOT NULL,
                                   start_time TIMESTAMP NOT NULL,
                                   end_time TIMESTAMP NOT NULL,
                                   session_type VARCHAR2(20) NOT NULL,
                                   meeting_link VARCHAR2(512),
                                   capacity NUMBER(10) DEFAULT 30 NOT NULL,
                                   enrolled_count NUMBER(10) DEFAULT 0 NOT NULL,
                                   registration_mode VARCHAR2(20) DEFAULT 'SelfEnroll' NOT NULL,
                                   status VARCHAR2(20) DEFAULT 'Upcoming' NOT NULL,
                                   is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
                                   deleted_at TIMESTAMP,
                                   version_no NUMBER(19) DEFAULT 0 NOT NULL,
                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                   created_by NUMBER(19),
                                   updated_by NUMBER(19),
                                   CONSTRAINT fk_ts_class FOREIGN KEY (class_id) REFERENCES classes(id),
                                   CONSTRAINT fk_ts_trainer FOREIGN KEY (trainer_id) REFERENCES users(id),
                                   CONSTRAINT fk_ts_created_by FOREIGN KEY (created_by) REFERENCES users(id),
                                   CONSTRAINT fk_ts_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
                                   CONSTRAINT ck_ts_type CHECK (session_type IN ('Offline', 'Online', 'Hybrid')),
                                   CONSTRAINT ck_ts_status CHECK (status IN ('Upcoming', 'Completed', 'Canceled')),
                                   CONSTRAINT ck_ts_deleted CHECK (is_deleted IN (0, 1)),
                                   CONSTRAINT ck_ts_capacity CHECK (capacity > 0),
                                   CONSTRAINT ck_ts_enrolled CHECK (enrolled_count >= 0 AND enrolled_count <= capacity),
                                   CONSTRAINT ck_ts_registration_mode CHECK (registration_mode IN ('AutoEnroll', 'SelfEnroll')),
                                   CONSTRAINT ck_ts_time CHECK (end_time > start_time)
);

CREATE TABLE quiz_assignments (
                                  id NUMBER(19) PRIMARY KEY,
                                  quiz_id NUMBER(19) NOT NULL,
                                  class_id NUMBER(19),
                                  training_session_id NUMBER(19),
                                  assigned_by NUMBER(19) NOT NULL,
                                  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                  CONSTRAINT fk_qa_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_qa_class FOREIGN KEY (class_id) REFERENCES classes(id),
                                  CONSTRAINT fk_qa_session FOREIGN KEY (training_session_id) REFERENCES training_sessions(id),
                                  CONSTRAINT fk_qa_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id),
                                  CONSTRAINT ck_qa_one_scope CHECK (
                                      (class_id IS NOT NULL AND training_session_id IS NULL)
                                          OR (class_id IS NULL AND training_session_id IS NOT NULL)
                                      )
);

CREATE UNIQUE INDEX uk_qa_quiz_class ON quiz_assignments (
                                                          CASE WHEN class_id IS NOT NULL THEN quiz_id END,
                                                          CASE WHEN class_id IS NOT NULL THEN class_id END
    );

CREATE UNIQUE INDEX uk_qa_quiz_session ON quiz_assignments (
                                                            CASE WHEN training_session_id IS NOT NULL THEN quiz_id END,
                                                            CASE WHEN training_session_id IS NOT NULL THEN training_session_id END
    );

CREATE TABLE quiz_attempts (
                               id NUMBER(19) PRIMARY KEY,
                               quiz_id NUMBER(19) NOT NULL,
                               user_id NUMBER(19) NOT NULL,
                               attempt_number NUMBER(10) NOT NULL,
                               status VARCHAR2(20) DEFAULT 'InProgress' NOT NULL,
                               answers_json CLOB NOT NULL,
                               score NUMBER(3),
                               correct_count NUMBER(10),
                               total_questions NUMBER(10),
                               passed NUMBER(1),
                               time_taken_seconds NUMBER(10),
                               started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                               submitted_at TIMESTAMP,
                               CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id),
                               CONSTRAINT fk_attempt_user FOREIGN KEY (user_id) REFERENCES users(id),
                               CONSTRAINT uk_attempt_number UNIQUE (quiz_id, user_id, attempt_number),
                               CONSTRAINT ck_attempt_answers_json CHECK (answers_json IS JSON),
                               CONSTRAINT ck_attempt_status CHECK (status IN ('InProgress', 'Submitted')),
                               CONSTRAINT ck_attempt_score CHECK (score BETWEEN 0 AND 100),
                               CONSTRAINT ck_attempt_counts CHECK (correct_count >= 0 AND total_questions > 0 AND correct_count <= total_questions),
                               CONSTRAINT ck_attempt_passed CHECK (passed IN (0, 1)),
                               CONSTRAINT ck_attempt_time CHECK (time_taken_seconds >= 0),
                               CONSTRAINT ck_attempt_submission_complete CHECK (
                                   status = 'InProgress'
                                       OR (
                                       score IS NOT NULL
                                           AND correct_count IS NOT NULL
                                           AND total_questions IS NOT NULL
                                           AND passed IS NOT NULL
                                           AND time_taken_seconds IS NOT NULL
                                           AND submitted_at IS NOT NULL
                                       )
                                   )
);

CREATE TABLE class_completion_quizzes (
    id NUMBER(19) PRIMARY KEY,
    class_id NUMBER(19) NOT NULL,
    quiz_id NUMBER(19) NOT NULL,
    passing_score NUMBER(3) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by NUMBER(19),
    updated_by NUMBER(19),
    CONSTRAINT fk_completion_quiz_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_completion_quiz_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id),
    CONSTRAINT fk_completion_quiz_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_completion_quiz_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT uk_completion_quiz_class_quiz UNIQUE (class_id, quiz_id),
    CONSTRAINT ck_completion_quiz_score CHECK (passing_score BETWEEN 0 AND 100)
);

CREATE TABLE course_results (
    id NUMBER(19) PRIMARY KEY,
    class_id NUMBER(19) NOT NULL,
    class_enrollment_id NUMBER(19) NOT NULL,
    calculated_status VARCHAR2(20) DEFAULT 'InProgress' NOT NULL,
    override_status VARCHAR2(20),
    attendance_rate NUMBER(5,2) DEFAULT 0 NOT NULL,
    attended_sessions NUMBER(10) DEFAULT 0 NOT NULL,
    total_sessions NUMBER(10) DEFAULT 0 NOT NULL,
    required_quiz_count NUMBER(10) DEFAULT 0 NOT NULL,
    passed_quiz_count NUMBER(10) DEFAULT 0 NOT NULL,
    calculated_at TIMESTAMP,
    calculated_by NUMBER(19),
    override_reason VARCHAR2(1000),
    overridden_at TIMESTAMP,
    overridden_by NUMBER(19),
    published_at TIMESTAMP,
    published_by NUMBER(19),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version_no NUMBER(19) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_course_result_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_course_result_enrollment FOREIGN KEY (class_enrollment_id) REFERENCES class_enrollments(id),
    CONSTRAINT fk_course_result_calculated_by FOREIGN KEY (calculated_by) REFERENCES users(id),
    CONSTRAINT fk_course_result_overridden_by FOREIGN KEY (overridden_by) REFERENCES users(id),
    CONSTRAINT fk_course_result_published_by FOREIGN KEY (published_by) REFERENCES users(id),
    CONSTRAINT uk_course_result_enrollment UNIQUE (class_enrollment_id),
    CONSTRAINT ck_course_result_calculated CHECK (calculated_status IN ('InProgress', 'Passed', 'Failed', 'Withdrawn')),
    CONSTRAINT ck_course_result_override CHECK (override_status IS NULL OR override_status IN ('Passed', 'Failed')),
    CONSTRAINT ck_course_result_attendance CHECK (attendance_rate BETWEEN 0 AND 100)
);

CREATE TABLE course_result_quizzes (
    id NUMBER(19) PRIMARY KEY,
    course_result_id NUMBER(19) NOT NULL,
    quiz_id NUMBER(19) NOT NULL,
    required_score NUMBER(3) NOT NULL,
    best_attempt_id NUMBER(19),
    best_score NUMBER(3),
    passed NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT fk_result_quiz_result FOREIGN KEY (course_result_id) REFERENCES course_results(id) ON DELETE CASCADE,
    CONSTRAINT fk_result_quiz_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id),
    CONSTRAINT fk_result_quiz_attempt FOREIGN KEY (best_attempt_id) REFERENCES quiz_attempts(id),
    CONSTRAINT uk_result_quiz_result_quiz UNIQUE (course_result_id, quiz_id),
    CONSTRAINT ck_result_quiz_required_score CHECK (required_score BETWEEN 0 AND 100),
    CONSTRAINT ck_result_quiz_best_score CHECK (best_score IS NULL OR best_score BETWEEN 0 AND 100),
    CONSTRAINT ck_result_quiz_passed CHECK (passed IN (0, 1))
);

CREATE TABLE course_result_adjustments (
    id NUMBER(19) PRIMARY KEY,
    course_result_id NUMBER(19) NOT NULL,
    previous_status VARCHAR2(20) NOT NULL,
    new_status VARCHAR2(20) NOT NULL,
    reason VARCHAR2(1000) NOT NULL,
    adjusted_by NUMBER(19) NOT NULL,
    adjusted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_result_adjustment_result FOREIGN KEY (course_result_id) REFERENCES course_results(id),
    CONSTRAINT fk_result_adjustment_user FOREIGN KEY (adjusted_by) REFERENCES users(id),
    CONSTRAINT ck_result_adjustment_previous CHECK (previous_status IN ('InProgress', 'Passed', 'Failed', 'Withdrawn')),
    CONSTRAINT ck_result_adjustment_new CHECK (new_status IN ('Passed', 'Failed'))
);

CREATE TABLE training_registrations (
                                        id NUMBER(19) PRIMARY KEY,
                                        training_id NUMBER(19) NOT NULL,
                                        user_id NUMBER(19) NOT NULL,
                                        status VARCHAR2(20) NOT NULL,
                                        registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                        cancelled_at TIMESTAMP,
                                        completed_at TIMESTAMP,
                                        CONSTRAINT fk_reg_training FOREIGN KEY (training_id) REFERENCES training_sessions(id) ON DELETE CASCADE,
                                        CONSTRAINT fk_reg_user FOREIGN KEY (user_id) REFERENCES users(id),
                                        CONSTRAINT uk_reg_training_user UNIQUE (training_id, user_id),
                                        CONSTRAINT ck_reg_status CHECK (status IN ('Registered', 'Waitlist', 'Completed', 'Cancelled')),
                                        CONSTRAINT ck_reg_cancelled_at CHECK ((status = 'Cancelled' AND cancelled_at IS NOT NULL) OR status <> 'Cancelled'),
                                        CONSTRAINT ck_reg_completed_at CHECK ((status = 'Completed' AND completed_at IS NOT NULL) OR status <> 'Completed')
);

CREATE TABLE attendance_records (
                                    id NUMBER(19) PRIMARY KEY,
                                    training_id NUMBER(19) NOT NULL,
                                    user_id NUMBER(19) NOT NULL,
                                    status VARCHAR2(20) NOT NULL,
                                    checked_in_at TIMESTAMP,
                                    check_in_method VARCHAR2(20) DEFAULT 'Manual' NOT NULL,
                                    updated_by NUMBER(19),
                                    correction_reason VARCHAR2(500),
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                    CONSTRAINT fk_att_training FOREIGN KEY (training_id) REFERENCES training_sessions(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_att_user FOREIGN KEY (user_id) REFERENCES users(id),
                                    CONSTRAINT fk_att_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
                                    CONSTRAINT uk_att_training_user UNIQUE (training_id, user_id),
                                    CONSTRAINT ck_att_status CHECK (status IN ('Present', 'Late', 'Absent')),
                                    CONSTRAINT ck_att_method CHECK (check_in_method IN ('Manual', 'QR'))
);

CREATE TABLE notifications (
                               id NUMBER(19) PRIMARY KEY,
                               user_id NUMBER(19) NOT NULL,
                               title VARCHAR2(255) NOT NULL,
                               message CLOB NOT NULL,
                               is_read NUMBER(1) DEFAULT 0 NOT NULL,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                               CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id),
                               CONSTRAINT ck_notifications_read CHECK (is_read IN (0, 1))
);

CREATE TABLE audit_logs (
                            id NUMBER(19) PRIMARY KEY,
                            user_id NUMBER(19),
                            action CLOB NOT NULL,
                            entity_type VARCHAR2(50),
                            entity_id NUMBER(19),
                            ip_address VARCHAR2(45),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                            CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE system_settings (
                                 id NUMBER(19) PRIMARY KEY,
                                 category VARCHAR2(50) NOT NULL,
                                 setting_key VARCHAR2(100) NOT NULL,
                                 setting_value CLOB NOT NULL,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 CONSTRAINT uk_settings_key UNIQUE (category, setting_key)
);

CREATE TABLE refresh_tokens (
                                id NUMBER(19) PRIMARY KEY,
                                user_id NUMBER(19) NOT NULL,
                                token VARCHAR2(512) NOT NULL,
                                expires_at TIMESTAMP NOT NULL,
                                revoked NUMBER(1) DEFAULT 0 NOT NULL,
                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id),
                                CONSTRAINT uk_refresh_token UNIQUE (token),
                                CONSTRAINT ck_refresh_revoked CHECK (revoked IN (0, 1))
);

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_deleted ON users(is_deleted);
CREATE INDEX idx_permissions_role ON permissions(role_id);

CREATE INDEX idx_syll_status ON syllabuses(status);
CREATE INDEX idx_syll_name ON syllabuses(name);
CREATE INDEX idx_syll_deleted ON syllabuses(is_deleted);
CREATE INDEX idx_syll_created_by ON syllabuses(created_by);
CREATE INDEX idx_material_topic ON material_files(topic_id);

CREATE INDEX idx_tp_status ON training_programs(status);
CREATE INDEX idx_tp_deleted ON training_programs(is_deleted);
CREATE INDEX idx_tp_syll_syllabus ON training_program_syllabuses(syllabus_id);

CREATE INDEX idx_classes_status ON classes(status);
CREATE INDEX idx_classes_tp ON classes(training_program_id);
CREATE INDEX idx_classes_dates ON classes(start_date, end_date);
CREATE INDEX idx_classes_deleted ON classes(is_deleted);
CREATE INDEX idx_class_trainers_user ON class_trainers(user_id);
CREATE INDEX idx_class_admins_user ON class_admins(user_id);
CREATE INDEX idx_class_enrollments_class_status ON class_enrollments(class_id, status);
CREATE INDEX idx_class_enrollments_user_status ON class_enrollments(user_id, status);
CREATE INDEX idx_completion_quizzes_class ON class_completion_quizzes(class_id);
CREATE INDEX idx_course_results_class_status ON course_results(class_id, calculated_status);
CREATE INDEX idx_course_results_publish ON course_results(class_id, published_at);
CREATE INDEX idx_result_adjustments_result ON course_result_adjustments(course_result_id, adjusted_at);

CREATE INDEX idx_questions_category ON questions(category);
CREATE INDEX idx_questions_diff ON questions(difficulty);
CREATE INDEX idx_questions_deleted ON questions(is_deleted);
CREATE INDEX idx_quizzes_status ON quizzes(status);
CREATE INDEX idx_quizzes_category ON quizzes(category);
CREATE INDEX idx_quizzes_dates ON quizzes(open_date, close_date);
CREATE INDEX idx_quizzes_deleted ON quizzes(is_deleted);
CREATE INDEX idx_qq_question ON quiz_questions(question_id);
CREATE INDEX idx_attempt_user ON quiz_attempts(user_id);
CREATE INDEX idx_attempt_quiz_user ON quiz_attempts(quiz_id, user_id);

CREATE INDEX idx_ts_class ON training_sessions(class_id);
CREATE INDEX idx_ts_trainer ON training_sessions(trainer_id);
CREATE INDEX idx_ts_status_date ON training_sessions(status, session_date);
CREATE INDEX idx_ts_deleted ON training_sessions(is_deleted);
CREATE INDEX idx_reg_training_status ON training_registrations(training_id, status, registered_at);
CREATE INDEX idx_reg_user ON training_registrations(user_id);
CREATE INDEX idx_att_user ON attendance_records(user_id);

CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read, created_at);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_user_time ON audit_logs(user_id, created_at);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_exp ON refresh_tokens(expires_at);
