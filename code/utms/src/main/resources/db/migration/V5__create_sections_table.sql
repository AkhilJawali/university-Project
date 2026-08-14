-- V5: Create sections table
-- Traces to: FR-5.1, FR-5.2, FR-5.3, FR-5.4, FR-5.5

CREATE TABLE utms.sections (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    batch_id        BIGINT NOT NULL,
    strength        INTEGER NOT NULL CHECK (strength > 0 AND strength <= 10000),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_sections_batches FOREIGN KEY (batch_id) REFERENCES utms.batches(id),
    CONSTRAINT uq_sections_name_batch UNIQUE (batch_id, name)
);

CREATE INDEX idx_sections_batch_id ON utms.sections(batch_id);
CREATE INDEX idx_sections_is_active ON utms.sections(is_active) WHERE deleted_at IS NULL;
