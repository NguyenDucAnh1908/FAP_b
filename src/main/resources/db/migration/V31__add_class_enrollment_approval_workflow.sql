BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE class_enrollments ADD reviewed_at TIMESTAMP';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE class_enrollments ADD reviewed_by NUMBER(19)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE class_enrollments ADD CONSTRAINT fk_class_enrollments_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2264 THEN
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE class_enrollments DROP CONSTRAINT ck_class_enrollments_status';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2443 THEN
            RAISE;
        END IF;
END;
/

ALTER TABLE class_enrollments ADD CONSTRAINT ck_class_enrollments_status CHECK (
    status IN ('PendingApproval', 'Enrolled', 'Waitlisted', 'Rejected', 'Withdrawn', 'Completed')
);
