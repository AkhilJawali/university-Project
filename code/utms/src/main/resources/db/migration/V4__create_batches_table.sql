-- V4: Create batches table
-- Traces to: FR-4.1, FR-4.2, FR-4.3, FR-4.4

CREATE TABLE utms.batches (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    program_id      BIGINT NOT NULL,
    academic_year   VARCHAR(20) NOT NULL,
    semester_number INTEGER NOT NULL CHECK (semester_number BETWEEN 1 AND 16),
    strength        INTEGER NOT NULL CHECK (strength > 0 AND strength <= 10000),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_batches_programs FOREIGN KEY (program_id) REFERENCES utms.programs(id)
);

CREATE INDEX idx_batches_program_id ON utms.batches(program_id);
CREATE INDEX idx_batches_is_active ON utms.batches(is_active) WHERE deleted_at IS NULL;
