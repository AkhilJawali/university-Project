-- V2: Create departments table
-- Traces to: FR-2.1, FR-2.2, FR-2.3, FR-2.4

CREATE TABLE utms.departments (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20) NOT NULL,
    campus_id       BIGINT NOT NULL,
    hod_faculty_id  BIGINT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_departments_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id),
    CONSTRAINT uq_departments_code_campus UNIQUE (campus_id, code)
);

CREATE INDEX idx_departments_campus_id ON utms.departments(campus_id);
CREATE INDEX idx_departments_is_active ON utms.departments(is_active) WHERE deleted_at IS NULL;
