-- V3: Create programs table
-- Traces to: FR-3.1, FR-3.2, FR-3.3, FR-3.4

CREATE TABLE utms.programs (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20) NOT NULL,
    department_id   BIGINT NOT NULL,
    duration_years  INTEGER NOT NULL CHECK (duration_years BETWEEN 1 AND 8),
    total_semesters INTEGER NOT NULL CHECK (total_semesters BETWEEN 1 AND 16),
    degree_type     VARCHAR(20) NOT NULL CHECK (degree_type IN ('UG', 'PG', 'PHD', 'DIPLOMA')),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_programs_departments FOREIGN KEY (department_id) REFERENCES utms.departments(id),
    CONSTRAINT uq_programs_code_department UNIQUE (department_id, code)
);

CREATE INDEX idx_programs_department_id ON utms.programs(department_id);
CREATE INDEX idx_programs_is_active ON utms.programs(is_active) WHERE deleted_at IS NULL;
