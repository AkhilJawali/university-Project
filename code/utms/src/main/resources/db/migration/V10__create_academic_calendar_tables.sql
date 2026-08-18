-- V10__create_academic_calendar_tables.sql
-- Creates all tables for Academic Calendar and Time-Slot Grid modules

-- ============================================================
-- 1. ACADEMIC CALENDARS
-- ============================================================
CREATE TABLE utms.academic_calendars (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    campus_id       BIGINT NOT NULL,
    academic_year   VARCHAR(9) NOT NULL,
    semester_type   VARCHAR(10) NOT NULL CHECK (semester_type IN ('ODD', 'EVEN', 'SUMMER')),
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_academic_calendars_campuses
        FOREIGN KEY (campus_id) REFERENCES utms.campuses(id),
    CONSTRAINT chk_academic_calendars_dates
        CHECK (start_date < end_date),
    CONSTRAINT chk_academic_calendars_year_format
        CHECK (academic_year ~ '^\d{4}-\d{4}$')
);

CREATE UNIQUE INDEX uq_academic_calendars_no_overlap
    ON utms.academic_calendars (campus_id, semester_type, academic_year)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_academic_calendars_campus_id
    ON utms.academic_calendars(campus_id);
CREATE INDEX idx_academic_calendars_academic_year
    ON utms.academic_calendars(academic_year);
CREATE INDEX idx_academic_calendars_active
    ON utms.academic_calendars(is_active)
    WHERE deleted_at IS NULL;

-- ============================================================
-- 2. HOLIDAYS
-- ============================================================
CREATE TABLE utms.holidays (
    id              BIGSERIAL PRIMARY KEY,
    calendar_id     BIGINT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    date            DATE NOT NULL,
    day_type        VARCHAR(15) NOT NULL CHECK (day_type IN ('FULL_DAY', 'HALF_DAY_AM', 'HALF_DAY_PM')),
    is_recurring    BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_holidays_academic_calendars
        FOREIGN KEY (calendar_id) REFERENCES utms.academic_calendars(id) ON DELETE CASCADE,
    CONSTRAINT uq_holidays_calendar_date
        UNIQUE (calendar_id, date)
);

CREATE INDEX idx_holidays_calendar_id ON utms.holidays(calendar_id);
CREATE INDEX idx_holidays_date ON utms.holidays(date);

-- ============================================================
-- 3. EXAM WINDOWS
-- ============================================================
CREATE TABLE utms.exam_windows (
    id              BIGSERIAL PRIMARY KEY,
    calendar_id     BIGINT NOT NULL,
    name            VARCHAR(200) NOT NULL,
    exam_type       VARCHAR(15) NOT NULL CHECK (exam_type IN ('MID_SEM', 'END_SEM', 'SUPPLEMENTARY')),
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_exam_windows_academic_calendars
        FOREIGN KEY (calendar_id) REFERENCES utms.academic_calendars(id) ON DELETE CASCADE,
    CONSTRAINT chk_exam_windows_dates
        CHECK (start_date <= end_date)
);

CREATE INDEX idx_exam_windows_calendar_id ON utms.exam_windows(calendar_id);
CREATE INDEX idx_exam_windows_dates ON utms.exam_windows(start_date, end_date);

-- ============================================================
-- 4. SPECIAL PERIODS
-- ============================================================
CREATE TABLE utms.special_periods (
    id              BIGSERIAL PRIMARY KEY,
    calendar_id     BIGINT NOT NULL,
    name            VARCHAR(200) NOT NULL,
    period_type     VARCHAR(15) NOT NULL CHECK (period_type IN ('ORIENTATION', 'REGISTRATION', 'BREAK', 'REVISION')),
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_special_periods_academic_calendars
        FOREIGN KEY (calendar_id) REFERENCES utms.academic_calendars(id) ON DELETE CASCADE,
    CONSTRAINT chk_special_periods_dates
        CHECK (start_date <= end_date)
);

CREATE INDEX idx_special_periods_calendar_id ON utms.special_periods(calendar_id);
CREATE INDEX idx_special_periods_dates ON utms.special_periods(start_date, end_date);

-- ============================================================
-- 5. TIME SLOT GRIDS
-- ============================================================
CREATE TABLE utms.time_slot_grids (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    campus_id       BIGINT NOT NULL,
    effective_from  DATE NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_time_slot_grids_campuses
        FOREIGN KEY (campus_id) REFERENCES utms.campuses(id)
);

CREATE UNIQUE INDEX uq_time_slot_grids_active_campus
    ON utms.time_slot_grids (campus_id)
    WHERE is_active = TRUE AND deleted_at IS NULL;

CREATE INDEX idx_time_slot_grids_campus_id ON utms.time_slot_grids(campus_id);
CREATE INDEX idx_time_slot_grids_active ON utms.time_slot_grids(is_active) WHERE deleted_at IS NULL;

-- ============================================================
-- 6. SLOT DEFINITIONS
-- ============================================================
CREATE TABLE utms.slot_definitions (
    id                BIGSERIAL PRIMARY KEY,
    grid_id           BIGINT NOT NULL,
    slot_number       INTEGER NOT NULL CHECK (slot_number > 0 AND slot_number <= 50),
    start_time        TIME NOT NULL,
    end_time          TIME NOT NULL,
    slot_type         VARCHAR(15) NOT NULL CHECK (slot_type IN ('LECTURE', 'TUTORIAL', 'PRACTICAL', 'BREAK', 'LUNCH')),
    duration_minutes  INTEGER NOT NULL CHECK (duration_minutes > 0 AND duration_minutes <= 300),
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100) NOT NULL,
    deleted_at        TIMESTAMP NULL,

    CONSTRAINT fk_slot_definitions_time_slot_grids
        FOREIGN KEY (grid_id) REFERENCES utms.time_slot_grids(id) ON DELETE CASCADE,
    CONSTRAINT chk_slot_definitions_times
        CHECK (start_time < end_time),
    CONSTRAINT uq_slot_definitions_grid_number
        UNIQUE (grid_id, slot_number)
);

CREATE INDEX idx_slot_definitions_grid_id ON utms.slot_definitions(grid_id);
CREATE INDEX idx_slot_definitions_times ON utms.slot_definitions(grid_id, start_time, end_time);

-- ============================================================
-- 7. WORKING DAYS
-- ============================================================
CREATE TABLE utms.working_days (
    id              BIGSERIAL PRIMARY KEY,
    grid_id         BIGINT NOT NULL,
    day_of_week     INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    is_working_day  BOOLEAN NOT NULL DEFAULT TRUE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_working_days_time_slot_grids
        FOREIGN KEY (grid_id) REFERENCES utms.time_slot_grids(id) ON DELETE CASCADE,
    CONSTRAINT uq_working_days_grid_day
        UNIQUE (grid_id, day_of_week)
);

CREATE INDEX idx_working_days_grid_id ON utms.working_days(grid_id);
