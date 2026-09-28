ALTER TABLE classes ADD (
    minimum_attendance_rate NUMBER(5,2) DEFAULT 80 NOT NULL,
    CONSTRAINT ck_classes_min_attendance CHECK (minimum_attendance_rate BETWEEN 0 AND 100)
);

CREATE SEQUENCE class_completion_quizzes_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE course_results_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE course_result_quizzes_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE course_result_adjustments_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

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

CREATE INDEX idx_completion_quizzes_class ON class_completion_quizzes(class_id);
CREATE INDEX idx_course_results_class_status ON course_results(class_id, calculated_status);
CREATE INDEX idx_course_results_publish ON course_results(class_id, published_at);
CREATE INDEX idx_result_adjustments_result ON course_result_adjustments(course_result_id, adjusted_at);

INSERT INTO course_results (
    id, class_id, class_enrollment_id, calculated_status,
    attendance_rate, attended_sessions, total_sessions,
    required_quiz_count, passed_quiz_count, updated_at
)
SELECT course_results_seq.NEXTVAL,
       ce.class_id,
       ce.id,
       CASE WHEN ce.status = 'Withdrawn' THEN 'Withdrawn' ELSE 'InProgress' END,
       0, 0, 0, 0, 0, CURRENT_TIMESTAMP
FROM class_enrollments ce
WHERE ce.status IN ('Enrolled', 'Completed')
   OR (ce.status = 'Withdrawn' AND ce.enrolled_at IS NOT NULL);
