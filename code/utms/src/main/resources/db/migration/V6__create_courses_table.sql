-- V6: Create courses table
-- Traces to: FR-1.1, FR-1.2, FR-2.1, FR-4.1, FR-5.1

CREATE TABLE utms.courses (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    department_id   BIGINT NOT NULL,
    lecture_hours   INTEGER NOT NULL DEFAULT 0 CHECK (lecture_hours >= 0 AND lecture_hours <= 10),
    tutorial_hours  INTEGER NOT NULL DEFAULT 0 CHECK (tutorial_hours >= 0 AND tutorial_hours <= 10),
    practical_hours INTEGER NOT NULL DEFAULT 0 CHECK (practical_hours >= 0 AND practical_hours <= 10),
    credit_hours    INTEGER NOT NULL CHECK (credit_hours > 0 AND credit_hours <= 20),
    course_type     VARCHAR(20) NOT NULL CHECK (course_type IN ('CORE', 'ELECTIVE', 'AUDIT', 'LAB')),
    is_cross_listed BOOLEAN NOT NULL DEFAULT FALSE,
    prerequisites   JSONB DEFAULT '[]'::jsonb,
    equipment_tags  JSONB DEFAULT '[]'::jsonb,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_courses_departments FOREIGN KEY (department_id) REFERENCES utms.departments(id),
    CONSTRAINT uq_courses_code UNIQUE (code),
    CONSTRAINT chk_courses_ltp_not_zero CHECK (lecture_hours + tutorial_hours + practical_hours > 0)
);

CREATE INDEX idx_courses_department_id ON utms.courses(department_id);
CREATE INDEX idx_courses_type ON utms.courses(course_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_courses_is_active ON utms.courses(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_courses_equipment_tags ON utms.courses USING GIN (equipment_tags);
