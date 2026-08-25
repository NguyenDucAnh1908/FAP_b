ALTER TABLE classes ADD (
    capacity NUMBER(10) DEFAULT 30 NOT NULL,
    self_enrollment_enabled NUMBER(1) DEFAULT 0 NOT NULL,
    enrollment_start_date DATE,
    enrollment_end_date DATE,
    CONSTRAINT ck_classes_capacity CHECK (capacity > 0),
    CONSTRAINT ck_classes_self_enrollment CHECK (self_enrollment_enabled IN (0, 1)),
    CONSTRAINT ck_classes_enrollment_dates CHECK (
        enrollment_start_date IS NULL
        OR enrollment_end_date IS NULL
        OR enrollment_start_date <= enrollment_end_date
    )
);

UPDATE classes c
SET capacity = GREATEST(
    30,
    COALESCE((
        SELECT MAX(ts.capacity)
        FROM training_sessions ts
        WHERE ts.class_id = c.id
          AND ts.is_deleted = 0
    ), 30)
);

ALTER TABLE training_sessions ADD (
    registration_mode VARCHAR2(20) DEFAULT 'SelfEnroll' NOT NULL,
    CONSTRAINT ck_ts_registration_mode CHECK (registration_mode IN ('AutoEnroll', 'SelfEnroll'))
);

CREATE SEQUENCE class_enrollments_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE class_enrollments (
    id NUMBER(19) PRIMARY KEY,
    class_id NUMBER(19) NOT NULL,
    user_id NUMBER(19) NOT NULL,
    status VARCHAR2(20) NOT NULL,
    source VARCHAR2(20) NOT NULL,
    enrolled_at TIMESTAMP,
    withdrawn_at TIMESTAMP,
    completed_at TIMESTAMP,
    version_no NUMBER(19) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by NUMBER(19),
    updated_by NUMBER(19),
    CONSTRAINT fk_class_enrollments_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_class_enrollments_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_class_enrollments_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_class_enrollments_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT uk_class_enrollments_class_user UNIQUE (class_id, user_id),
    CONSTRAINT ck_class_enrollments_status CHECK (status IN ('Enrolled', 'Waitlisted', 'Withdrawn', 'Completed')),
    CONSTRAINT ck_class_enrollments_source CHECK (source IN ('AdminAdded', 'SelfRegistered', 'Migration')),
    CONSTRAINT ck_class_enrollments_withdrawn CHECK ((status = 'Withdrawn' AND withdrawn_at IS NOT NULL) OR status <> 'Withdrawn'),
    CONSTRAINT ck_class_enrollments_completed CHECK ((status = 'Completed' AND completed_at IS NOT NULL) OR status <> 'Completed')
);

CREATE INDEX idx_class_enrollments_class_status ON class_enrollments(class_id, status);
CREATE INDEX idx_class_enrollments_user_status ON class_enrollments(user_id, status);

INSERT INTO class_enrollments (
    id,
    class_id,
    user_id,
    status,
    source,
    enrolled_at,
    completed_at,
    created_at,
    updated_at
)
SELECT class_enrollments_seq.NEXTVAL,
       grouped.class_id,
       grouped.user_id,
       CASE
           WHEN grouped.has_eligible = 1 AND grouped.class_status = 'Closed' THEN 'Completed'
           WHEN grouped.has_eligible = 1 THEN 'Enrolled'
           ELSE 'Waitlisted'
       END,
       'Migration',
       grouped.first_registered_at,
       CASE
           WHEN grouped.has_eligible = 1 AND grouped.class_status = 'Closed'
               THEN COALESCE(grouped.last_completed_at, grouped.class_updated_at, CURRENT_TIMESTAMP)
           ELSE NULL
       END,
       grouped.first_registered_at,
       CURRENT_TIMESTAMP
FROM (
    SELECT ts.class_id,
           tr.user_id,
           c.status AS class_status,
           c.updated_at AS class_updated_at,
           MIN(tr.registered_at) AS first_registered_at,
           MAX(tr.completed_at) AS last_completed_at,
           MAX(CASE WHEN tr.status IN ('Registered', 'Completed') THEN 1 ELSE 0 END) AS has_eligible
    FROM training_registrations tr
    JOIN training_sessions ts ON ts.id = tr.training_id
    JOIN classes c ON c.id = ts.class_id
    WHERE tr.status IN ('Registered', 'Completed', 'Waitlist')
      AND ts.class_id IS NOT NULL
    GROUP BY ts.class_id, tr.user_id, c.status, c.updated_at
) grouped;
