-- V1: Create campuses table
-- Traces to: FR-1.1, FR-1.2, FR-1.3, FR-1.4

CREATE SCHEMA IF NOT EXISTS utms;

CREATE TABLE utms.campuses (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20) NOT NULL,
    address         VARCHAR(500),
    city            VARCHAR(100),
    state           VARCHAR(100),
    timezone        VARCHAR(50) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT uq_campuses_code UNIQUE (code)
);

CREATE INDEX idx_campuses_is_active ON utms.campuses(is_active) WHERE deleted_at IS NULL;
