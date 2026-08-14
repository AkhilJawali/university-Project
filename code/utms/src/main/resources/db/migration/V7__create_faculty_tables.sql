-- V7: Create faculty management tables
-- Traces to: FR-1.4, FR-7.1, FR-7.2, FR-7.3, FR-7.5

-- Faculty table
CREATE TABLE utms.faculty (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     VARCHAR(20) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(200) NOT NULL,
    phone           VARCHAR(15) NULL,
    department_id   BIGINT NOT NULL,
    cadre           VARCHAR(30) NOT NULL CHECK (cadre IN ('PROFESSOR', 'ASSOCIATE_PROFESSOR', 'ASSISTANT_PROFESSOR', 'LECTURER', 'VISITING')),
    qualification   VARCHAR(200) NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_faculty_departments FOREIGN KEY (department_id) REFERENCES utms.departments(id),
    CONSTRAINT uq_faculty_employee_id UNIQUE (employee_id),
    CONSTRAINT uq_faculty_email UNIQUE (email)
);

CREATE INDEX idx_faculty_department_id ON utms.faculty(department_id);
CREATE INDEX idx_faculty_cadre ON utms.faculty(cadre) WHERE deleted_at IS NULL;
CREATE INDEX idx_faculty_is_active ON utms.faculty(is_active) WHERE deleted_at IS NULL;

-- Faculty availability windows
CREATE TABLE utms.faculty_availability_windows (
    id              BIGSERIAL PRIMARY KEY,
    faculty_id      BIGINT NOT NULL,
    day_of_week     INTEGER NOT NULL CHECK (day_of_week >= 1 AND day_of_week <= 7),
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    constraint_type VARCHAR(20) NOT NULL CHECK (constraint_type IN ('HARD_UNAVAILABLE', 'SOFT_PREFERRED', 'SOFT_AVOID')),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_faculty_availability_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id),
    CONSTRAINT uq_faculty_availability_slot UNIQUE (faculty_id, day_of_week, start_time, end_time),
    CONSTRAINT chk_faculty_availability_time_order CHECK (end_time > start_time)
);

CREATE INDEX idx_faculty_availability_faculty_id ON utms.faculty_availability_windows(faculty_id);
CREATE INDEX idx_faculty_availability_day ON utms.faculty_availability_windows(day_of_week) WHERE deleted_at IS NULL;

-- Faculty competencies (faculty-course junction)
CREATE TABLE utms.faculty_competencies (
    id              BIGSERIAL PRIMARY KEY,
    faculty_id      BIGINT NOT NULL,
    course_id       BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_faculty_competencies_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id),
    CONSTRAINT fk_faculty_competencies_courses FOREIGN KEY (course_id) REFERENCES utms.courses(id),
    CONSTRAINT uq_faculty_competencies UNIQUE (faculty_id, course_id)
);

CREATE INDEX idx_faculty_competencies_faculty_id ON utms.faculty_competencies(faculty_id);
CREATE INDEX idx_faculty_competencies_course_id ON utms.faculty_competencies(course_id);

-- Faculty campus associations
CREATE TABLE utms.faculty_campus_associations (
    id                    BIGSERIAL PRIMARY KEY,
    faculty_id            BIGINT NOT NULL,
    campus_id             BIGINT NOT NULL,
    travel_time_minutes   INTEGER NOT NULL CHECK (travel_time_minutes > 0),
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100) NOT NULL,
    updated_by            VARCHAR(100) NOT NULL,
    deleted_at            TIMESTAMP NULL,

    CONSTRAINT fk_faculty_campus_assoc_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id),
    CONSTRAINT fk_faculty_campus_assoc_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id),
    CONSTRAINT uq_faculty_campus_associations UNIQUE (faculty_id, campus_id)
);

CREATE INDEX idx_faculty_campus_assoc_faculty_id ON utms.faculty_campus_associations(faculty_id);
CREATE INDEX idx_faculty_campus_assoc_campus_id ON utms.faculty_campus_associations(campus_id);

-- Workload configuration per cadre
CREATE TABLE utms.workload_configs (
    id                BIGSERIAL PRIMARY KEY,
    cadre             VARCHAR(30) NOT NULL,
    min_weekly_hours  INTEGER NOT NULL,
    max_weekly_hours  INTEGER NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100) NOT NULL,
    deleted_at        TIMESTAMP NULL,

    CONSTRAINT uq_workload_configs_cadre UNIQUE (cadre),
    CONSTRAINT chk_workload_configs_hours CHECK (max_weekly_hours >= min_weekly_hours)
);
