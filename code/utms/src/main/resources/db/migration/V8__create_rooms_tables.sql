-- V8: Create room and resource block tables
-- Traces to: FR-1.5, FR-1.6, FR-6.6, FR-6.7, FR-6.9

-- Rooms table
CREATE TABLE utms.rooms (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    campus_id       BIGINT NOT NULL,
    building        VARCHAR(100) NOT NULL,
    floor           VARCHAR(20) NULL,
    capacity        INTEGER NOT NULL,
    room_type       VARCHAR(20) NOT NULL CHECK (room_type IN ('LECTURE_HALL', 'LAB', 'SEMINAR', 'AUDITORIUM', 'TUTORIAL')),
    equipment_tags  JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_rooms_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id),
    CONSTRAINT uq_rooms_campus_code UNIQUE (campus_id, code),
    CONSTRAINT chk_rooms_capacity CHECK (capacity > 0 AND capacity <= 5000)
);

CREATE INDEX idx_rooms_campus_id ON utms.rooms(campus_id);
CREATE INDEX idx_rooms_room_type ON utms.rooms(room_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_rooms_equipment_tags ON utms.rooms USING GIN (equipment_tags);

-- Resource blocks table
CREATE TABLE utms.resource_blocks (
    id              BIGSERIAL PRIMARY KEY,
    room_id         BIGINT NOT NULL,
    reason          VARCHAR(500) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    start_time      TIME NULL,
    end_time        TIME NULL,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('REQUESTED', 'APPROVED', 'ACTIVE', 'RELEASED')),
    requested_by    VARCHAR(100) NOT NULL,
    approved_by     VARCHAR(100) NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_resource_blocks_rooms FOREIGN KEY (room_id) REFERENCES utms.rooms(id),
    CONSTRAINT chk_resource_blocks_date_order CHECK (end_date >= start_date)
);

CREATE INDEX idx_resource_blocks_room_id ON utms.resource_blocks(room_id);
CREATE INDEX idx_resource_blocks_status ON utms.resource_blocks(status) WHERE deleted_at IS NULL;
