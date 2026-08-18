# Design: Academic Calendar & Time-Slot Grid

**Jira Reference:** AID-183
**Source Requirements:** docs/requirements/AID-183-academic-calendar-timeslot-grid-requirements.md
**Application:** Existing (Spring Boot modular monolith)
**Stack:** Java 21 · Spring Boot 3.4.1 · Maven · PostgreSQL 15+ · Flyway
**Generated:** 16 August 2026

---

## 1. Overview

This design covers the Academic Calendar and Time-Slot Grid modules within the UTMS Master Data layer. These two modules define the temporal framework for the scheduling engine:

- **Academic Calendar** — defines semester boundaries, holidays, exam windows, and special periods per campus. The engine uses this to determine which days are available for scheduling.
- **Time-Slot Grid** — defines the daily period structure (slot start/end times, types, working days) per campus. The engine uses this to determine which time periods within a day are schedulable.

Together, they answer: "On which days and at which times can sessions be placed?"

### Key Capabilities

- Full CRUD for academic calendars with sub-resources (holidays, exam windows, special periods)
- Full CRUD for time-slot grids with sub-resources (slot definitions, working days)
- Calendar date-overlap detection (same campus + semester type)
- Slot time-overlap detection within a grid
- Single-active-grid enforcement per campus
- Calendar change impact detection (flag sessions on newly added holidays)
- Data segregation via RLS (campus-scoped visibility)

---

## 2. Architecture

### High-Level Component Diagram

```
                         ┌─────────────────────────┐
                         │     API Gateway / JWT    │
                         └────────────┬────────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              │                       │                         │
┌─────────────▼──────────┐ ┌─────────▼───────────┐ ┌─────────▼───────────┐
│ AcademicCalendarCtrl    │ │ TimeSlotGridCtrl     │ │ CalendarImpactCtrl  │
│ HolidayController       │ │ SlotDefinitionCtrl   │ │                     │
│ ExamWindowController    │ │ WorkingDayController  │ │                     │
│ SpecialPeriodController │ │                       │ │                     │
└─────────────┬──────────┘ └─────────┬───────────┘ └─────────┬───────────┘
              │                       │                         │
┌─────────────▼──────────┐ ┌─────────▼───────────┐ ┌─────────▼───────────┐
│ AcademicCalendarService │ │ TimeSlotGridService  │ │ CalendarImpactSvc   │
│ HolidayService          │ │ SlotDefinitionSvc    │ │                     │
│ ExamWindowService       │ │ WorkingDayService    │ │                     │
│ SpecialPeriodService    │ │                       │ │                     │
└─────────────┬──────────┘ └─────────┬───────────┘ └─────────┬───────────┘
              │                       │                         │
       ┌──────┴──────────────────────┼─────────────────────────┘
       │                              │
┌──────▼──────────────┐    ┌─────────▼───────────┐
│  Repository Layer    │    │   Audit Service     │
│  (Spring Data JPA)   │    │   (same tx)         │
└──────┬──────────────┘    └─────────┬───────────┘
       │                              │
┌──────▼──────────────────────────────▼───┐
│          PostgreSQL (utms)               │
│  academic_calendars, holidays,           │
│  exam_windows, special_periods,          │
│  time_slot_grids, slot_definitions,      │
│  working_days, audit_events              │
└──────────────────────────────────────────┘
```

### Package Structure

```
com.utms.masterdata.academiccalendar
├── controller/
│   ├── AcademicCalendarController.java
│   ├── HolidayController.java
│   ├── ExamWindowController.java
│   └── SpecialPeriodController.java
├── service/
│   ├── AcademicCalendarService.java
│   ├── HolidayService.java
│   ├── ExamWindowService.java
│   ├── SpecialPeriodService.java
│   └── CalendarImpactService.java
├── repository/
│   ├── AcademicCalendarRepository.java
│   ├── HolidayRepository.java
│   ├── ExamWindowRepository.java
│   └── SpecialPeriodRepository.java
├── entity/
│   ├── AcademicCalendar.java
│   ├── Holiday.java
│   ├── ExamWindow.java
│   └── SpecialPeriod.java
├── dto/
│   ├── request/
│   └── response/
├── mapper/
│   └── AcademicCalendarMapper.java
└── enums/
    ├── SemesterType.java
    ├── DayType.java
    ├── ExamType.java
    └── PeriodType.java

com.utms.masterdata.timeslot
├── controller/
│   ├── TimeSlotGridController.java
│   ├── SlotDefinitionController.java
│   └── WorkingDayController.java
├── service/
│   ├── TimeSlotGridService.java
│   ├── SlotDefinitionService.java
│   └── WorkingDayService.java
├── repository/
│   ├── TimeSlotGridRepository.java
│   ├── SlotDefinitionRepository.java
│   └── WorkingDayRepository.java
├── entity/
│   ├── TimeSlotGrid.java
│   ├── SlotDefinition.java
│   └── WorkingDay.java
├── dto/
│   ├── request/
│   └── response/
├── mapper/
│   └── TimeSlotGridMapper.java
└── enums/
    └── SlotType.java
```

### Key Design Decisions

- **Sub-resource routing** — holidays, exam windows, and special periods are accessed under their parent calendar (`/academic-calendars/{id}/holidays`), not as top-level resources. This reinforces the parent-child relationship and simplifies authorization.
- **Single-active-grid enforcement** — activating a new grid atomically deactivates the previous active grid for that campus within one transaction.
- **Impact detection as async-ready** — `CalendarImpactService` is implemented synchronously for Phase 1 but returns a list of impacted sessions. In future phases, it can emit domain events for notification.
- **Soft-delete semantics** — same as campus hierarchy: `deleted_at` timestamp, active-only queries by default.

---

## 3. API Design

### 3.1 Academic Calendar Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/academic-calendars` | List calendars (paginated, filtered) | Authenticated | FR-9.1 |
| GET | `/api/v1/academic-calendars/{id}` | Get single calendar | Authenticated | FR-1.1 |
| POST | `/api/v1/academic-calendars` | Create calendar | ADMIN, REGISTRAR | FR-1.1, FR-1.2 |
| PUT | `/api/v1/academic-calendars/{id}` | Update calendar | ADMIN, REGISTRAR | FR-1.1 |
| DELETE | `/api/v1/academic-calendars/{id}` | Soft-delete calendar | ADMIN | FR-1.1 |

#### Query Parameters (GET list)

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | 0 | Page number (0-indexed) |
| `size` | int | 20 | Page size (max 100) |
| `sort` | string | `startDate,desc` | Sort field and direction |
| `campusId` | Long | — | Filter by campus |
| `academicYear` | String | — | Filter by academic year (e.g., "2026-2027") |
| `semesterType` | String | — | Filter by semester type: ODD, EVEN, SUMMER |
| `isActive` | boolean | true | Filter by active status |

#### Create Academic Calendar Request DTO
```java
public record CreateAcademicCalendarRequest(
    @NotBlank @Size(min = 1, max = 200)
    String name,

    @NotNull
    Long campusId,

    @NotBlank @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "Must be format YYYY-YYYY")
    String academicYear,

    @NotNull
    SemesterType semesterType,

    @NotNull
    LocalDate startDate,

    @NotNull
    LocalDate endDate
) {}
```

#### Update Academic Calendar Request DTO
```java
public record UpdateAcademicCalendarRequest(
    @NotBlank @Size(min = 1, max = 200)
    String name,

    @NotNull
    LocalDate startDate,

    @NotNull
    LocalDate endDate,

    @NotNull
    Boolean isActive
) {}
```

#### Academic Calendar Response DTO
```java
public record AcademicCalendarDto(
    Long id,
    String name,
    Long campusId,
    String campusName,
    String academicYear,
    SemesterType semesterType,
    LocalDate startDate,
    LocalDate endDate,
    Boolean isActive,
    Integer holidayCount,
    Integer examWindowCount,
    Integer specialPeriodCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

#### Example Response
```json
{
  "data": {
    "id": 1,
    "name": "Odd Semester 2026",
    "campusId": 1,
    "campusName": "Main Campus",
    "academicYear": "2026-2027",
    "semesterType": "ODD",
    "startDate": "2026-08-01",
    "endDate": "2026-12-15",
    "isActive": true,
    "holidayCount": 12,
    "examWindowCount": 2,
    "specialPeriodCount": 3,
    "createdAt": "2026-08-16T10:00:00Z",
    "updatedAt": "2026-08-16T10:00:00Z"
  }
}
```

#### Error Responses
- `400` — Validation failure (start_date after end_date, invalid academic_year format)
- `400` — Campus not found or inactive
- `409` — Overlapping calendar for same campus and semester type (FR-1.5)
- `409` — Cannot delete calendar with active holidays/exam windows
- `401` — No/invalid JWT
- `403` — Insufficient role

---

### 3.2 Holiday Endpoints (Sub-resource)

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/academic-calendars/{calendarId}/holidays` | List holidays for calendar | Authenticated | FR-9.2 |
| GET | `/api/v1/academic-calendars/{calendarId}/holidays/{id}` | Get single holiday | Authenticated | FR-2.1 |
| POST | `/api/v1/academic-calendars/{calendarId}/holidays` | Add holiday | ADMIN, REGISTRAR | FR-2.1, FR-2.2 |
| PUT | `/api/v1/academic-calendars/{calendarId}/holidays/{id}` | Update holiday | ADMIN, REGISTRAR | FR-2.1 |
| DELETE | `/api/v1/academic-calendars/{calendarId}/holidays/{id}` | Remove holiday | ADMIN, REGISTRAR | FR-2.1 |

#### Create Holiday Request DTO
```java
public record CreateHolidayRequest(
    @NotBlank @Size(min = 1, max = 100)
    String name,

    @NotNull
    LocalDate date,

    @NotNull
    DayType dayType,

    @NotNull
    Boolean isRecurring
) {}
```

#### Holiday Response DTO
```java
public record HolidayDto(
    Long id,
    Long calendarId,
    String name,
    LocalDate date,
    DayType dayType,
    Boolean isRecurring,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

#### Impact Detection on Create

When a holiday is added to an active calendar, the response includes impacted sessions if any exist:

```json
{
  "data": { "id": 15, "name": "Diwali", "date": "2026-10-20", ... },
  "impactedSessions": [
    {
      "sessionId": 450,
      "courseName": "Data Structures",
      "facultyName": "Dr. Kumar",
      "roomCode": "CS-101",
      "batchName": "2024-28 A",
      "timeSlot": "09:00-10:00"
    }
  ]
}
```

---

### 3.3 Exam Window Endpoints (Sub-resource)

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/academic-calendars/{calendarId}/exam-windows` | List exam windows | Authenticated | FR-9.2 |
| GET | `/api/v1/academic-calendars/{calendarId}/exam-windows/{id}` | Get single exam window | Authenticated | FR-3.1 |
| POST | `/api/v1/academic-calendars/{calendarId}/exam-windows` | Add exam window | ADMIN, REGISTRAR | FR-3.1, FR-3.2 |
| PUT | `/api/v1/academic-calendars/{calendarId}/exam-windows/{id}` | Update exam window | ADMIN, REGISTRAR | FR-3.1 |
| DELETE | `/api/v1/academic-calendars/{calendarId}/exam-windows/{id}` | Remove exam window | ADMIN, REGISTRAR | FR-3.1 |

#### Create Exam Window Request DTO
```java
public record CreateExamWindowRequest(
    @NotBlank @Size(min = 1, max = 200)
    String name,

    @NotNull
    ExamType examType,

    @NotNull
    LocalDate startDate,

    @NotNull
    LocalDate endDate
) {}
```

#### Exam Window Response DTO
```java
public record ExamWindowDto(
    Long id,
    Long calendarId,
    String name,
    ExamType examType,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

---

### 3.4 Special Period Endpoints (Sub-resource)

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/academic-calendars/{calendarId}/special-periods` | List special periods | Authenticated | FR-9.2 |
| GET | `/api/v1/academic-calendars/{calendarId}/special-periods/{id}` | Get single period | Authenticated | FR-4.1 |
| POST | `/api/v1/academic-calendars/{calendarId}/special-periods` | Add special period | ADMIN, REGISTRAR | FR-4.1, FR-4.2 |
| PUT | `/api/v1/academic-calendars/{calendarId}/special-periods/{id}` | Update period | ADMIN, REGISTRAR | FR-4.1 |
| DELETE | `/api/v1/academic-calendars/{calendarId}/special-periods/{id}` | Remove period | ADMIN, REGISTRAR | FR-4.1 |

#### Create Special Period Request DTO
```java
public record CreateSpecialPeriodRequest(
    @NotBlank @Size(min = 1, max = 200)
    String name,

    @NotNull
    PeriodType periodType,

    @NotNull
    LocalDate startDate,

    @NotNull
    LocalDate endDate
) {}
```

#### Special Period Response DTO
```java
public record SpecialPeriodDto(
    Long id,
    Long calendarId,
    String name,
    PeriodType periodType,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

---

### 3.5 Time-Slot Grid Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/time-slot-grids` | List grids (paginated, filtered) | Authenticated | FR-5.1 |
| GET | `/api/v1/time-slot-grids/{id}` | Get single grid (with slots & working days) | Authenticated | FR-5.1, FR-9.4 |
| POST | `/api/v1/time-slot-grids` | Create grid | ADMIN, REGISTRAR | FR-5.1, FR-5.2 |
| PUT | `/api/v1/time-slot-grids/{id}` | Update grid | ADMIN, REGISTRAR | FR-5.1 |
| DELETE | `/api/v1/time-slot-grids/{id}` | Soft-delete grid | ADMIN | FR-5.1 |
| PUT | `/api/v1/time-slot-grids/{id}/activate` | Activate grid (deactivate others) | ADMIN, REGISTRAR | FR-5.3 |
| GET | `/api/v1/campuses/{campusId}/active-grid` | Get active grid for campus | Authenticated | FR-9.3 |

#### Query Parameters (GET list)

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | 0 | Page number (0-indexed) |
| `size` | int | 20 | Page size (max 100) |
| `campusId` | Long | — | Filter by campus |
| `isActive` | boolean | — | Filter by active status |

#### Create Time-Slot Grid Request DTO
```java
public record CreateTimeSlotGridRequest(
    @NotBlank @Size(min = 1, max = 200)
    String name,

    @NotNull
    Long campusId,

    @NotNull
    LocalDate effectiveFrom,

    Boolean isActive  // defaults to false; use /activate endpoint to activate
) {}
```

#### Time-Slot Grid Response DTO
```java
public record TimeSlotGridDto(
    Long id,
    String name,
    Long campusId,
    String campusName,
    LocalDate effectiveFrom,
    Boolean isActive,
    List<SlotDefinitionDto> slots,
    List<WorkingDayDto> workingDays,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

#### Example Response (GET single grid)
```json
{
  "data": {
    "id": 1,
    "name": "Main Campus Standard Grid",
    "campusId": 1,
    "campusName": "Main Campus",
    "effectiveFrom": "2026-08-01",
    "isActive": true,
    "slots": [
      { "id": 1, "slotNumber": 1, "startTime": "08:00", "endTime": "09:00", "slotType": "LECTURE", "durationMinutes": 60 },
      { "id": 2, "slotNumber": 2, "startTime": "09:00", "endTime": "10:00", "slotType": "LECTURE", "durationMinutes": 60 },
      { "id": 3, "slotNumber": 3, "startTime": "10:00", "endTime": "10:15", "slotType": "BREAK", "durationMinutes": 15 },
      { "id": 4, "slotNumber": 4, "startTime": "10:15", "endTime": "11:15", "slotType": "LECTURE", "durationMinutes": 60 },
      { "id": 5, "slotNumber": 5, "startTime": "11:15", "endTime": "12:15", "slotType": "LECTURE", "durationMinutes": 60 },
      { "id": 6, "slotNumber": 6, "startTime": "12:15", "endTime": "13:00", "slotType": "LUNCH", "durationMinutes": 45 },
      { "id": 7, "slotNumber": 7, "startTime": "13:00", "endTime": "14:00", "slotType": "TUTORIAL", "durationMinutes": 60 },
      { "id": 8, "slotNumber": 8, "startTime": "14:00", "endTime": "17:00", "slotType": "PRACTICAL", "durationMinutes": 180 }
    ],
    "workingDays": [
      { "dayOfWeek": 1, "dayName": "MONDAY", "isWorkingDay": true },
      { "dayOfWeek": 2, "dayName": "TUESDAY", "isWorkingDay": true },
      { "dayOfWeek": 3, "dayName": "WEDNESDAY", "isWorkingDay": true },
      { "dayOfWeek": 4, "dayName": "THURSDAY", "isWorkingDay": true },
      { "dayOfWeek": 5, "dayName": "FRIDAY", "isWorkingDay": true },
      { "dayOfWeek": 6, "dayName": "SATURDAY", "isWorkingDay": true },
      { "dayOfWeek": 7, "dayName": "SUNDAY", "isWorkingDay": false }
    ],
    "createdAt": "2026-08-16T10:00:00Z",
    "updatedAt": "2026-08-16T10:00:00Z"
  }
}
```

---

### 3.6 Slot Definition Endpoints (Sub-resource)

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/time-slot-grids/{gridId}/slots` | List all slots for grid | Authenticated | FR-9.4 |
| POST | `/api/v1/time-slot-grids/{gridId}/slots` | Add slot to grid | ADMIN, REGISTRAR | FR-6.1, FR-6.2 |
| PUT | `/api/v1/time-slot-grids/{gridId}/slots/{id}` | Update slot | ADMIN, REGISTRAR | FR-6.1 |
| DELETE | `/api/v1/time-slot-grids/{gridId}/slots/{id}` | Remove slot | ADMIN, REGISTRAR | FR-6.1 |
| POST | `/api/v1/time-slot-grids/{gridId}/slots/bulk` | Bulk create slots | ADMIN, REGISTRAR | FR-6.1 |

#### Create Slot Definition Request DTO
```java
public record CreateSlotDefinitionRequest(
    @NotNull @Min(1) @Max(50)
    Integer slotNumber,

    @NotNull
    LocalTime startTime,

    @NotNull
    LocalTime endTime,

    @NotNull
    SlotType slotType
) {}
```

#### Bulk Create Slot Definitions Request DTO
```java
public record BulkCreateSlotsRequest(
    @NotEmpty @Size(max = 50)
    @Valid
    List<CreateSlotDefinitionRequest> slots
) {}
```

#### Slot Definition Response DTO
```java
public record SlotDefinitionDto(
    Long id,
    Long gridId,
    Integer slotNumber,
    LocalTime startTime,
    LocalTime endTime,
    SlotType slotType,
    Integer durationMinutes,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

---

### 3.7 Working Days Endpoints (Sub-resource)

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/time-slot-grids/{gridId}/working-days` | Get working days config | Authenticated | FR-7.1 |
| PUT | `/api/v1/time-slot-grids/{gridId}/working-days` | Update working days config | ADMIN, REGISTRAR | FR-7.1, FR-7.2 |

#### Update Working Days Request DTO
```java
public record UpdateWorkingDaysRequest(
    @NotEmpty @Size(min = 7, max = 7)
    @Valid
    List<WorkingDayEntry> days
) {}

public record WorkingDayEntry(
    @NotNull @Min(1) @Max(7)
    Integer dayOfWeek,

    @NotNull
    Boolean isWorkingDay
) {}
```

#### Working Day Response DTO
```java
public record WorkingDayDto(
    Integer dayOfWeek,
    String dayName,
    Boolean isWorkingDay
) {}
```

---

### 3.8 Calendar Impact Detection Endpoint

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/academic-calendars/{calendarId}/impact-analysis` | Check impact of calendar on sessions | Authenticated | FR-8.1, FR-8.3 |
| POST | `/api/v1/academic-calendars/{calendarId}/holidays/{holidayId}/detect-impact` | Detect impact of specific holiday | ADMIN, REGISTRAR | FR-8.1, FR-8.2 |

#### Impact Analysis Response
```java
public record CalendarImpactDto(
    Long calendarId,
    int totalImpactedSessions,
    List<ImpactedSessionDto> sessions
) {}

public record ImpactedSessionDto(
    Long sessionId,
    String courseName,
    String courseCode,
    String facultyName,
    String roomCode,
    String batchName,
    String sectionName,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String status
) {}
```

---

## 4. Data Model

### 4.1 Entity-Relationship Diagram

```
Campus (1) ──→ (N) AcademicCalendar (1) ──→ (N) Holiday
                                     (1) ──→ (N) ExamWindow
                                     (1) ──→ (N) SpecialPeriod

Campus (1) ──→ (N) TimeSlotGrid (1) ──→ (N) SlotDefinition
                                (1) ──→ (7) WorkingDay
```

### 4.2 Table Definitions

#### `utms.academic_calendars`
```sql
CREATE TABLE utms.academic_calendars (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    campus_id       BIGINT NOT NULL,
    academic_year   VARCHAR(9) NOT NULL,  -- e.g., "2026-2027"
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

-- Prevent overlapping calendars for the same campus + semester type
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
```

#### `utms.holidays`
```sql
CREATE TABLE utms.holidays (
    id              BIGSERIAL PRIMARY KEY,
    calendar_id     BIGINT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    date            DATE NOT NULL,
    day_type        VARCHAR(15) NOT NULL CHECK (day_type IN ('FULL_DAY', 'HALF_DAY_AM', 'HALF_DAY_PM')),
    is_recurring    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,

    CONSTRAINT fk_holidays_academic_calendars
        FOREIGN KEY (calendar_id) REFERENCES utms.academic_calendars(id) ON DELETE CASCADE,
    CONSTRAINT uq_holidays_calendar_date
        UNIQUE (calendar_id, date)
);

CREATE INDEX idx_holidays_calendar_id ON utms.holidays(calendar_id);
CREATE INDEX idx_holidays_date ON utms.holidays(date);
```

#### `utms.exam_windows`
```sql
CREATE TABLE utms.exam_windows (
    id              BIGSERIAL PRIMARY KEY,
    calendar_id     BIGINT NOT NULL,
    name            VARCHAR(200) NOT NULL,
    exam_type       VARCHAR(15) NOT NULL CHECK (exam_type IN ('MID_SEM', 'END_SEM', 'SUPPLEMENTARY')),
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,

    CONSTRAINT fk_exam_windows_academic_calendars
        FOREIGN KEY (calendar_id) REFERENCES utms.academic_calendars(id) ON DELETE CASCADE,
    CONSTRAINT chk_exam_windows_dates
        CHECK (start_date <= end_date)
);

CREATE INDEX idx_exam_windows_calendar_id ON utms.exam_windows(calendar_id);
CREATE INDEX idx_exam_windows_dates ON utms.exam_windows(start_date, end_date);
```

#### `utms.special_periods`
```sql
CREATE TABLE utms.special_periods (
    id              BIGSERIAL PRIMARY KEY,
    calendar_id     BIGINT NOT NULL,
    name            VARCHAR(200) NOT NULL,
    period_type     VARCHAR(15) NOT NULL CHECK (period_type IN ('ORIENTATION', 'REGISTRATION', 'BREAK', 'REVISION')),
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,

    CONSTRAINT fk_special_periods_academic_calendars
        FOREIGN KEY (calendar_id) REFERENCES utms.academic_calendars(id) ON DELETE CASCADE,
    CONSTRAINT chk_special_periods_dates
        CHECK (start_date <= end_date)
);

CREATE INDEX idx_special_periods_calendar_id ON utms.special_periods(calendar_id);
CREATE INDEX idx_special_periods_dates ON utms.special_periods(start_date, end_date);
```

#### `utms.time_slot_grids`
```sql
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

-- Only one active grid per campus
CREATE UNIQUE INDEX uq_time_slot_grids_active_campus
    ON utms.time_slot_grids (campus_id)
    WHERE is_active = TRUE AND deleted_at IS NULL;

CREATE INDEX idx_time_slot_grids_campus_id ON utms.time_slot_grids(campus_id);
CREATE INDEX idx_time_slot_grids_active ON utms.time_slot_grids(is_active) WHERE deleted_at IS NULL;
```

#### `utms.slot_definitions`
```sql
CREATE TABLE utms.slot_definitions (
    id                BIGSERIAL PRIMARY KEY,
    grid_id           BIGINT NOT NULL,
    slot_number       INTEGER NOT NULL CHECK (slot_number > 0 AND slot_number <= 50),
    start_time        TIME NOT NULL,
    end_time          TIME NOT NULL,
    slot_type         VARCHAR(15) NOT NULL CHECK (slot_type IN ('LECTURE', 'TUTORIAL', 'PRACTICAL', 'BREAK', 'LUNCH')),
    duration_minutes  INTEGER NOT NULL CHECK (duration_minutes > 0 AND duration_minutes <= 300),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100) NOT NULL,

    CONSTRAINT fk_slot_definitions_time_slot_grids
        FOREIGN KEY (grid_id) REFERENCES utms.time_slot_grids(id) ON DELETE CASCADE,
    CONSTRAINT chk_slot_definitions_times
        CHECK (start_time < end_time),
    CONSTRAINT uq_slot_definitions_grid_number
        UNIQUE (grid_id, slot_number)
);

CREATE INDEX idx_slot_definitions_grid_id ON utms.slot_definitions(grid_id);
CREATE INDEX idx_slot_definitions_times ON utms.slot_definitions(grid_id, start_time, end_time);
```

#### `utms.working_days`
```sql
CREATE TABLE utms.working_days (
    id              BIGSERIAL PRIMARY KEY,
    grid_id         BIGINT NOT NULL,
    day_of_week     INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    is_working_day  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,

    CONSTRAINT fk_working_days_time_slot_grids
        FOREIGN KEY (grid_id) REFERENCES utms.time_slot_grids(id) ON DELETE CASCADE,
    CONSTRAINT uq_working_days_grid_day
        UNIQUE (grid_id, day_of_week)
);

CREATE INDEX idx_working_days_grid_id ON utms.working_days(grid_id);
```

### 4.3 Enum Types

```java
public enum SemesterType {
    ODD, EVEN, SUMMER
}

public enum DayType {
    FULL_DAY, HALF_DAY_AM, HALF_DAY_PM
}

public enum ExamType {
    MID_SEM, END_SEM, SUPPLEMENTARY
}

public enum PeriodType {
    ORIENTATION, REGISTRATION, BREAK, REVISION
}

public enum SlotType {
    LECTURE, TUTORIAL, PRACTICAL, BREAK, LUNCH;

    public boolean isSchedulable() {
        return this == LECTURE || this == TUTORIAL || this == PRACTICAL;
    }
}
```

### 4.4 Migration Strategy

- **Tool:** Flyway
- **Naming:** `V{N}__{description}.sql`
- **Planned migrations:**
  - `V6__create_academic_calendars_table.sql`
  - `V7__create_holidays_table.sql`
  - `V8__create_exam_windows_table.sql`
  - `V9__create_special_periods_table.sql`
  - `V10__create_time_slot_grids_table.sql`
  - `V11__create_slot_definitions_table.sql`
  - `V12__create_working_days_table.sql`
- All migrations are reversible (DROP TABLE IF EXISTS in undo scripts)
- Schema: `utms` (set via Flyway `defaultSchema` config)

---

## 5. Service / Business Logic

### 5.1 Service Classes

| Service | Responsibility | Traces to |
|---------|---------------|-----------|
| `AcademicCalendarService` | Calendar CRUD, overlap detection, campus FK validation | FR-1.x, FR-1.5 |
| `HolidayService` | Holiday CRUD, date-range validation, triggers impact detection | FR-2.x |
| `ExamWindowService` | Exam window CRUD, date-range validation within calendar | FR-3.x |
| `SpecialPeriodService` | Special period CRUD, date-range validation within calendar | FR-4.x |
| `TimeSlotGridService` | Grid CRUD, single-active enforcement, campus FK validation | FR-5.x |
| `SlotDefinitionService` | Slot CRUD, overlap detection, duration calculation, bulk create | FR-6.x |
| `WorkingDayService` | Working day config management, at-least-one-working-day validation | FR-7.x |
| `CalendarImpactService` | Detect sessions on holiday dates, flag for rescheduling | FR-8.x |

### 5.2 Key Business Rules

#### Calendar Date Overlap Detection (FR-1.5)
```java
@Service
@RequiredArgsConstructor
public class AcademicCalendarService {

    private final AcademicCalendarRepository calendarRepository;
    private final CampusRepository campusRepository;

    @Transactional
    public AcademicCalendarDto create(CreateAcademicCalendarRequest request) {
        // 1. Validate campus exists and is active
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(request.campusId())
            .orElseThrow(() -> new ValidationException("campusId",
                "Campus not found or has been deleted", request.campusId()));

        // 2. Validate start_date < end_date
        if (!request.startDate().isBefore(request.endDate())) {
            throw new ValidationException("startDate",
                "Start date must be before end date", request.startDate());
        }

        // 3. Check for overlapping calendars (same campus + semester_type + overlapping dates)
        boolean overlaps = calendarRepository.existsOverlapping(
            request.campusId(),
            request.semesterType(),
            request.startDate(),
            request.endDate()
        );
        if (overlaps) {
            throw new ConflictException(
                "An academic calendar for this campus and semester type already exists " +
                "with overlapping dates.");
        }

        // 4. Create and persist
        AcademicCalendar calendar = new AcademicCalendar();
        calendar.setName(request.name().trim());
        calendar.setCampus(campus);
        calendar.setAcademicYear(request.academicYear());
        calendar.setSemesterType(request.semesterType());
        calendar.setStartDate(request.startDate());
        calendar.setEndDate(request.endDate());
        calendar.setIsActive(true);

        AcademicCalendar saved = calendarRepository.save(calendar);
        auditEventPublisher.record("ACADEMIC_CALENDAR", saved.getId(),
            "CREATE", null, saved, getCurrentUserId());

        return mapper.toDto(saved);
    }
}
```

#### Repository Method for Overlap Detection
```java
public interface AcademicCalendarRepository extends JpaRepository<AcademicCalendar, Long> {

    @Query("""
        SELECT COUNT(c) > 0 FROM AcademicCalendar c
        WHERE c.campus.id = :campusId
          AND c.semesterType = :semesterType
          AND c.deletedAt IS NULL
          AND c.startDate < :endDate
          AND c.endDate > :startDate
        """)
    boolean existsOverlapping(
        @Param("campusId") Long campusId,
        @Param("semesterType") SemesterType semesterType,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
```

#### Holiday Date-Range Validation (FR-2.3)
```java
@Service
@RequiredArgsConstructor
public class HolidayService {

    @Transactional
    public HolidayDto create(Long calendarId, CreateHolidayRequest request) {
        AcademicCalendar calendar = calendarRepository
            .findByIdAndDeletedAtIsNull(calendarId)
            .orElseThrow(() -> new EntityNotFoundException("Academic calendar not found"));

        // Validate holiday date within calendar range
        if (request.date().isBefore(calendar.getStartDate())
                || request.date().isAfter(calendar.getEndDate())) {
            throw new ValidationException("date",
                String.format("Holiday date must be within calendar range (%s to %s)",
                    calendar.getStartDate(), calendar.getEndDate()),
                request.date());
        }

        // Check duplicate holiday on same date
        if (holidayRepository.existsByCalendarIdAndDate(calendarId, request.date())) {
            throw new ConflictException(
                "A holiday already exists on " + request.date() + " in this calendar.");
        }

        Holiday holiday = new Holiday();
        holiday.setCalendar(calendar);
        holiday.setName(request.name().trim());
        holiday.setDate(request.date());
        holiday.setDayType(request.dayType());
        holiday.setIsRecurring(request.isRecurring());

        Holiday saved = holidayRepository.save(holiday);

        // Trigger impact detection if calendar is active
        List<ImpactedSessionDto> impacted = Collections.emptyList();
        if (calendar.getIsActive()) {
            impacted = calendarImpactService.detectImpact(calendar.getCampus().getId(),
                request.date(), request.dayType());
        }

        auditEventPublisher.record("HOLIDAY", saved.getId(),
            "CREATE", null, saved, getCurrentUserId());

        return new HolidayCreateResponse(mapper.toDto(saved), impacted);
    }
}
```

#### Slot Time Overlap Detection (FR-6.4)
```java
@Service
@RequiredArgsConstructor
public class SlotDefinitionService {

    @Transactional
    public SlotDefinitionDto create(Long gridId, CreateSlotDefinitionRequest request) {
        TimeSlotGrid grid = gridRepository.findByIdAndDeletedAtIsNull(gridId)
            .orElseThrow(() -> new EntityNotFoundException("Time-slot grid not found"));

        // Validate start_time < end_time
        if (!request.startTime().isBefore(request.endTime())) {
            throw new ValidationException("startTime",
                "Start time must be before end time", request.startTime());
        }

        // Check for overlapping slots within the same grid
        boolean overlaps = slotDefinitionRepository.existsOverlapping(
            gridId, request.startTime(), request.endTime());
        if (overlaps) {
            throw new ConflictException(
                "Slot times overlap with an existing slot in this grid.");
        }

        // Check duplicate slot number
        if (slotDefinitionRepository.existsByGridIdAndSlotNumber(gridId, request.slotNumber())) {
            throw new ConflictException(
                "Slot number " + request.slotNumber() + " already exists in this grid.");
        }

        // Auto-calculate duration
        int durationMinutes = (int) Duration.between(request.startTime(), request.endTime()).toMinutes();
        if (durationMinutes <= 0 || durationMinutes > 300) {
            throw new ValidationException("endTime",
                "Duration must be between 1 and 300 minutes", durationMinutes);
        }

        SlotDefinition slot = new SlotDefinition();
        slot.setGrid(grid);
        slot.setSlotNumber(request.slotNumber());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setSlotType(request.slotType());
        slot.setDurationMinutes(durationMinutes);

        SlotDefinition saved = slotDefinitionRepository.save(slot);
        auditEventPublisher.record("SLOT_DEFINITION", saved.getId(),
            "CREATE", null, saved, getCurrentUserId());

        return mapper.toDto(saved);
    }
}
```

#### Repository Method for Slot Overlap Detection
```java
public interface SlotDefinitionRepository extends JpaRepository<SlotDefinition, Long> {

    @Query("""
        SELECT COUNT(s) > 0 FROM SlotDefinition s
        WHERE s.grid.id = :gridId
          AND s.startTime < :endTime
          AND s.endTime > :startTime
        """)
    boolean existsOverlapping(
        @Param("gridId") Long gridId,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );
}
```

#### Single-Active-Grid Enforcement (FR-5.3)
```java
@Service
@RequiredArgsConstructor
public class TimeSlotGridService {

    @Transactional
    public TimeSlotGridDto activate(Long gridId) {
        TimeSlotGrid grid = gridRepository.findByIdAndDeletedAtIsNull(gridId)
            .orElseThrow(() -> new EntityNotFoundException("Time-slot grid not found"));

        if (grid.getIsActive()) {
            return mapper.toDto(grid); // Already active, no-op
        }

        // Validate grid has at least one slot definition
        long slotCount = slotDefinitionRepository.countByGridId(gridId);
        if (slotCount == 0) {
            throw new ValidationException("gridId",
                "Cannot activate a grid with no slot definitions", gridId);
        }

        // Validate grid has at least one working day
        long workingDayCount = workingDayRepository.countByGridIdAndIsWorkingDayTrue(gridId);
        if (workingDayCount == 0) {
            throw new ValidationException("gridId",
                "Cannot activate a grid with no working days configured", gridId);
        }

        // Deactivate current active grid for this campus
        gridRepository.deactivateAllForCampus(grid.getCampus().getId());

        // Activate the target grid
        grid.setIsActive(true);
        grid.setUpdatedAt(LocalDateTime.now());

        TimeSlotGrid saved = gridRepository.save(grid);
        auditEventPublisher.record("TIME_SLOT_GRID", saved.getId(),
            "ACTIVATE", null, saved, getCurrentUserId());

        return mapper.toDto(saved);
    }
}
```

#### Repository Method for Deactivation
```java
public interface TimeSlotGridRepository extends JpaRepository<TimeSlotGrid, Long> {

    @Modifying
    @Query("""
        UPDATE TimeSlotGrid g SET g.isActive = false, g.updatedAt = CURRENT_TIMESTAMP
        WHERE g.campus.id = :campusId AND g.isActive = true AND g.deletedAt IS NULL
        """)
    void deactivateAllForCampus(@Param("campusId") Long campusId);
}
```

#### Calendar Change Impact Detection (FR-8.x)
```java
@Service
@RequiredArgsConstructor
public class CalendarImpactService {

    private final SessionRepository sessionRepository;

    /**
     * Detects sessions scheduled on a given holiday date for a campus.
     * Returns impacted sessions and optionally flags them for rescheduling.
     */
    @Transactional(readOnly = true)
    public List<ImpactedSessionDto> detectImpact(Long campusId, LocalDate holidayDate,
                                                  DayType dayType) {
        // Query sessions on the holiday date for the given campus
        List<Session> sessions = sessionRepository
            .findByCampusIdAndDate(campusId, holidayDate);

        if (sessions.isEmpty()) {
            return Collections.emptyList();
        }

        // Filter by day type if half-day
        List<Session> impacted = switch (dayType) {
            case FULL_DAY -> sessions;
            case HALF_DAY_AM -> sessions.stream()
                .filter(s -> s.getStartTime().isBefore(LocalTime.NOON))
                .toList();
            case HALF_DAY_PM -> sessions.stream()
                .filter(s -> s.getStartTime().isAfter(LocalTime.NOON)
                    || s.getStartTime().equals(LocalTime.NOON))
                .toList();
        };

        return impacted.stream()
            .map(this::toImpactedDto)
            .toList();
    }

    /**
     * Flags impacted sessions for rescheduling. Called when a holiday is confirmed
     * on an active calendar with published timetable.
     */
    @Transactional
    public int flagForRescheduling(Long campusId, LocalDate holidayDate, DayType dayType) {
        List<ImpactedSessionDto> impacted = detectImpact(campusId, holidayDate, dayType);
        if (impacted.isEmpty()) {
            return 0;
        }

        List<Long> sessionIds = impacted.stream()
            .map(ImpactedSessionDto::sessionId)
            .toList();

        return sessionRepository.flagNeedsRescheduling(sessionIds);
    }

    private ImpactedSessionDto toImpactedDto(Session session) {
        return new ImpactedSessionDto(
            session.getId(),
            session.getCourse().getName(),
            session.getCourse().getCode(),
            session.getFaculty().getName(),
            session.getRoom().getCode(),
            session.getBatch().getName(),
            session.getSection() != null ? session.getSection().getName() : null,
            session.getDate(),
            session.getStartTime(),
            session.getEndTime(),
            session.getStatus().name()
        );
    }
}
```

### 5.3 Working Day Initialization

When a new time-slot grid is created, the system auto-seeds 7 working-day records with the default configuration (Mon-Sat = working, Sun = off):

```java
@Transactional
public TimeSlotGridDto create(CreateTimeSlotGridRequest request) {
    // ... validation and grid creation ...

    TimeSlotGrid saved = gridRepository.save(grid);

    // Auto-seed default working days (Mon-Sat working, Sun off)
    for (int day = 1; day <= 7; day++) {
        WorkingDay wd = new WorkingDay();
        wd.setGrid(saved);
        wd.setDayOfWeek(day);
        wd.setIsWorkingDay(day <= 6); // Mon(1) through Sat(6) = true, Sun(7) = false
        workingDayRepository.save(wd);
    }

    return mapper.toDto(saved);
}
```

### 5.4 Bulk Slot Creation with Validation

```java
@Transactional
public List<SlotDefinitionDto> bulkCreate(Long gridId, BulkCreateSlotsRequest request) {
    TimeSlotGrid grid = gridRepository.findByIdAndDeletedAtIsNull(gridId)
        .orElseThrow(() -> new EntityNotFoundException("Time-slot grid not found"));

    List<CreateSlotDefinitionRequest> slots = request.slots();

    // 1. Validate no duplicate slot numbers within the request
    Set<Integer> slotNumbers = new HashSet<>();
    for (CreateSlotDefinitionRequest slot : slots) {
        if (!slotNumbers.add(slot.slotNumber())) {
            throw new ValidationException("slotNumber",
                "Duplicate slot number in request: " + slot.slotNumber(), slot.slotNumber());
        }
    }

    // 2. Validate no overlaps within the request itself
    List<CreateSlotDefinitionRequest> sorted = slots.stream()
        .sorted(Comparator.comparing(CreateSlotDefinitionRequest::startTime))
        .toList();
    for (int i = 0; i < sorted.size() - 1; i++) {
        if (sorted.get(i).endTime().isAfter(sorted.get(i + 1).startTime())) {
            throw new ConflictException(String.format(
                "Slots %d and %d overlap in time.",
                sorted.get(i).slotNumber(), sorted.get(i + 1).slotNumber()));
        }
    }

    // 3. Validate no conflicts with existing slots in DB
    for (CreateSlotDefinitionRequest slot : slots) {
        if (slotDefinitionRepository.existsOverlapping(gridId, slot.startTime(), slot.endTime())) {
            throw new ConflictException(
                "Slot " + slot.slotNumber() + " overlaps with existing slot in grid.");
        }
        if (slotDefinitionRepository.existsByGridIdAndSlotNumber(gridId, slot.slotNumber())) {
            throw new ConflictException(
                "Slot number " + slot.slotNumber() + " already exists in grid.");
        }
    }

    // 4. Persist all
    List<SlotDefinition> entities = slots.stream().map(slot -> {
        SlotDefinition sd = new SlotDefinition();
        sd.setGrid(grid);
        sd.setSlotNumber(slot.slotNumber());
        sd.setStartTime(slot.startTime());
        sd.setEndTime(slot.endTime());
        sd.setSlotType(slot.slotType());
        sd.setDurationMinutes((int) Duration.between(slot.startTime(), slot.endTime()).toMinutes());
        return sd;
    }).toList();

    List<SlotDefinition> saved = slotDefinitionRepository.saveAll(entities);
    return saved.stream().map(mapper::toDto).toList();
}
```

### 5.5 Validation Rules (Jakarta Validation)

All validation is applied at the controller layer via `@Valid` on request DTOs. Custom validators:

| Validator | Purpose |
|-----------|---------|
| `@ValidDateRange` | Ensures start_date < end_date on class-level |
| `@WithinCalendarRange` | Ensures sub-resource dates fall within parent calendar |
| `@ActiveCampus` | Validates that referenced campus exists and is active |
| `@AcademicYearFormat` | Validates "YYYY-YYYY" format and logical year sequence |

### 5.6 Transaction Boundaries

- Every `create`, `update`, `delete`, `activate` method is annotated `@Transactional`
- Audit event is written in the same transaction
- Impact detection (read-only) uses `@Transactional(readOnly = true)`
- Bulk slot creation is one transaction (all-or-nothing)

---

## 6. Cross-cutting Concerns

### 6.1 Error Handling

`@RestControllerAdvice` in `com.utms.common.exception.GlobalExceptionHandler`:

| Exception | HTTP Status | Traces to |
|-----------|-------------|-----------|
| `MethodArgumentNotValidException` | 400 | Validation Rules |
| `ValidationException` (custom) | 400 | FR-1.4, FR-2.3, FR-6.3 |
| `EntityNotFoundException` | 404 | — |
| `ConflictException` (custom) | 409 | FR-1.5, FR-6.4 |
| `AccessDeniedException` | 403 | NFR Security |
| `Exception` (catch-all) | 500 | NFR Security (no internals exposed) |

Response format:
```json
{
  "timestamp": "2026-08-16T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "An academic calendar for this campus and semester type already exists with overlapping dates.",
  "path": "/api/v1/academic-calendars",
  "details": []
}
```

### 6.2 Security

- **Authentication:** JWT extracted via Spring Security filter; user ID and roles in claims.
- **Authorization:** `@PreAuthorize` on controller methods:
  - Write operations (create, update, delete, activate): `hasAnyRole('ADMIN', 'REGISTRAR')`
  - Read operations: `isAuthenticated()`
- **Data Segregation (RLS):** Repository queries include campus scope derived from authenticated user's JWT claims. HODs and Coordinators see only calendars/grids for their own campus.
- **Input Sanitization:** All string inputs trimmed; no HTML allowed in any field.

### 6.3 Audit Trail

Every mutation triggers an audit event stored in the `audit_events` table within the same transaction:

```java
auditEventPublisher.record(
    entityType,    // "ACADEMIC_CALENDAR", "HOLIDAY", "TIME_SLOT_GRID", etc.
    entityId,
    action,        // "CREATE", "UPDATE", "DELETE", "ACTIVATE"
    previousValue, // JSON serialized previous state (null for CREATE)
    newValue,      // JSON serialized new state
    userId         // from SecurityContext
);
```

### 6.4 Configuration

```yaml
utms:
  academic-calendar:
    max-calendars-per-campus: 10
    max-holidays-per-calendar: 100
    max-exam-windows-per-calendar: 10
    max-special-periods-per-calendar: 20
  time-slot-grid:
    max-slots-per-grid: 50
    default-working-days: [1, 2, 3, 4, 5, 6]  # Mon-Sat
  pagination:
    default-size: 20
    max-size: 100
```

### 6.5 Logging

- SLF4J + Logback with structured JSON in deployed environments
- Log: entity type, entity ID, operation, user ID, request ID
- Never log: passwords, tokens, full request bodies with PII
- Security events (access denials, validation failures) logged at WARN level
- Impact detection results logged at INFO level (session count, campus)

---

## 7. Non-Functional Design

| NFR | How It's Met |
|-----|-------------|
| Performance (< 200ms single CRUD) | Direct JPA queries with indexes on FKs, date columns, and active-status; no N+1 via `@EntityGraph` for sub-resource loading |
| Performance (< 500ms calendar with all sub-entities) | Custom JPQL with JOIN FETCH loading holidays, exam windows, and special periods in one query |
| Security (RBAC) | `@PreAuthorize` annotations on every endpoint; role hierarchy configured in Spring Security |
| Security (RLS / Data Segregation) | Repository methods include campus filter from SecurityContext |
| Input Validation (allowlist) | Jakarta Validation + `@Pattern` for academic_year format; enum constraints for types; reject unknown fields |
| Parameterized Queries | Spring Data JPA method queries and `@Query` with named parameters — no string concatenation |
| Audit (same transaction) | Audit events persisted within the `@Transactional` boundary of the mutation |
| Single-Active-Grid | Partial unique index `WHERE is_active = TRUE AND deleted_at IS NULL` on (campus_id) guarantees DB-level enforcement |
| No Overlapping Calendars | Overlap detection via JPQL range comparison; unique index on (campus_id, semester_type, academic_year) prevents exact duplicates |
| Slot Non-Overlap | JPQL overlap query + sequential slot_number uniqueness constraint |

---

## 8. Testing Strategy

### 8.1 Unit Tests (JUnit 5 + Mockito)

| Layer | What to Test |
|-------|-------------|
| `AcademicCalendarService` | Overlap detection logic, date validation, campus FK check |
| `HolidayService` | Date-range validation (within calendar), duplicate detection |
| `ExamWindowService` | Date-range validation within calendar |
| `SpecialPeriodService` | Date-range validation within calendar |
| `TimeSlotGridService` | Single-active enforcement, activation preconditions (slots, working days) |
| `SlotDefinitionService` | Time overlap detection, duration calculation, bulk validation |
| `WorkingDayService` | At-least-one-working-day validation |
| `CalendarImpactService` | Impact detection with FULL_DAY/HALF_DAY filtering |

Naming: `methodName_scenario_expectedResult()`

Examples:
```java
@Test
void create_overlappingCalendarDates_throwsConflictException() { ... }

@Test
void create_holidayOutsideCalendarRange_throwsValidationException() { ... }

@Test
void create_overlappingSlotTimes_throwsConflictException() { ... }

@Test
void activate_gridWithNoSlots_throwsValidationException() { ... }

@Test
void activate_deactivatesPreviousGrid_succeeds() { ... }

@Test
void detectImpact_halfDayAM_returnsOnlyMorningSessionsImpacted() { ... }

@Test
void bulkCreate_internalOverlap_throwsConflictException() { ... }

@Test
void create_slotDurationAutoCalculated_correctMinutes() { ... }
```

### 8.2 Integration Tests (Testcontainers + Spring Boot Test)

| Scenario | Covers |
|----------|--------|
| CRUD happy path for academic calendar | FR-1.1 |
| Create calendar with invalid campus → 400 | FR-1.3, AC-2 |
| Create overlapping calendar → 409 | FR-1.5, AC-3 |
| Add holiday within range → 201 | FR-2.1, FR-2.3 |
| Add holiday outside range → 400 | FR-2.3, AC-4 |
| Add holiday triggers impact detection | FR-8.1, AC-5 |
| Create grid and activate → deactivates previous | FR-5.3, AC-7 |
| Add overlapping slot → 409 | FR-6.4, AC-6 |
| Bulk create slots with internal overlap → 409 | FR-6.4 |
| Get active grid for campus | FR-9.3, AC-8 |
| Working days update with all false → 400 | FR-7.3 |
| Pagination and filtering on calendar list | FR-9.1 |
| Unauthenticated access → 401 | NFR Security |
| Coordinator can only see own campus calendars | NFR Data Segregation |

### 8.3 Coverage Target

- 80%+ line coverage on new code (enforced by JaCoCo)

---

## 9. Requirement Traceability

| Requirement | Design Element(s) |
|-------------|-------------------|
| FR-1.1 | `AcademicCalendarController` CRUD endpoints, `AcademicCalendarService` |
| FR-1.2 | `AcademicCalendar` entity fields, `CreateAcademicCalendarRequest` DTO |
| FR-1.3 | Campus FK validation in `AcademicCalendarService.create()` |
| FR-1.4 | `chk_academic_calendars_dates` CHECK constraint + service-level validation |
| FR-1.5 | `existsOverlapping()` query + overlap detection in service |
| FR-2.1 | `HolidayController` CRUD endpoints, `HolidayService` |
| FR-2.2 | `Holiday` entity fields, `CreateHolidayRequest` DTO, `DayType` enum |
| FR-2.3 | Date-range validation in `HolidayService.create()` |
| FR-2.4 | Engine reads holidays via `HolidayRepository.findByCalendarId()` and excludes those dates |
| FR-2.5 | `CalendarImpactService.detectImpact()` + `flagForRescheduling()` |
| FR-3.1 | `ExamWindowController` CRUD endpoints, `ExamWindowService` |
| FR-3.2 | `ExamWindow` entity fields, `CreateExamWindowRequest` DTO, `ExamType` enum |
| FR-3.3 | Date-range validation in `ExamWindowService.create()` |
| FR-3.4 | Engine queries exam windows and blocks regular sessions during those dates |
| FR-4.1 | `SpecialPeriodController` CRUD endpoints, `SpecialPeriodService` |
| FR-4.2 | `SpecialPeriod` entity fields, `CreateSpecialPeriodRequest` DTO, `PeriodType` enum |
| FR-4.3 | Engine queries special periods (BREAK, REVISION) and blocks scheduling |
| FR-5.1 | `TimeSlotGridController` CRUD endpoints, `TimeSlotGridService` |
| FR-5.2 | `TimeSlotGrid` entity fields, `CreateTimeSlotGridRequest` DTO |
| FR-5.3 | `TimeSlotGridService.activate()` with `deactivateAllForCampus()` + partial unique index |
| FR-6.1 | `SlotDefinitionController` CRUD + bulk endpoint, `SlotDefinitionService` |
| FR-6.2 | `SlotDefinition` entity fields, `CreateSlotDefinitionRequest` DTO, `SlotType` enum |
| FR-6.3 | Duration auto-calculation in `SlotDefinitionService.create()` |
| FR-6.4 | `existsOverlapping()` slot overlap query + bulk validation |
| FR-6.5 | `uq_slot_definitions_grid_number` unique constraint on (grid_id, slot_number) |
| FR-6.6 | No duration constraints on slot_type — supports 60, 90, 180 min via `duration_minutes` |
| FR-6.7 | `SlotType.isSchedulable()` method — engine filters BREAK/LUNCH |
| FR-7.1 | `WorkingDayController` GET/PUT, `WorkingDayService`, `WorkingDay` entity |
| FR-7.2 | `day_of_week` column (1-7), `is_working_day` boolean |
| FR-7.3 | Default seeding in `TimeSlotGridService.create()` (Mon-Sat working) |
| FR-8.1 | `CalendarImpactService.detectImpact()` queries sessions by campus + date |
| FR-8.2 | `CalendarImpactService.flagForRescheduling()` updates session status |
| FR-8.3 | `ImpactedSessionDto` with full details returned in API response |
| FR-9.1 | Paginated list endpoints with `campusId`, `academicYear`, `semesterType` filters |
| FR-9.2 | Sub-resource GET endpoints for holidays, exam windows, special periods |
| FR-9.3 | `GET /api/v1/campuses/{campusId}/active-grid` convenience endpoint |
| FR-9.4 | `GET /api/v1/time-slot-grids/{gridId}/slots` returns all slot definitions |
| NFR Performance | Indexed queries, `@EntityGraph`, pagination, < 200ms / < 500ms targets |
| NFR Security | JWT auth, `@PreAuthorize`, RLS scoping |
| NFR Input Validation | Jakarta Validation, enum constraints, `@Pattern` for format |
| NFR Parameterized Queries | Spring Data JPA (no raw concatenation) |
| NFR Audit | `AuditEventPublisher` in same transaction |
| NFR Data Segregation | Repository-level campus filtering from SecurityContext |

---

## 10. Open Questions

| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | Should campuses support multiple active calendars (one per department)? Current design: one active calendar per campus + semester_type. | Product Owner | Open |
| 2 | How far in advance can calendars be created? Limit to current + next academic year, or unlimited? | Product Owner | Open |
| 3 | Should time-slot grid changes propagate to already-generated timetables, or only affect future generations? | Tech Lead | Open |
| 4 | Should recurring holidays auto-populate when a new calendar is created for the next academic year? | Product Owner | Open |
| 5 | When the Session entity/table is implemented (future sprint), what is the exact status field name for "needs rescheduling"? Impact detection code assumes `status` enum with a `NEEDS_RESCHEDULING` value. | Tech Lead | Open |
| 6 | Should exam window overlap within the same calendar be allowed (e.g., mid-sem and supplementary can overlap) or rejected? | Product Owner | Open |
| 7 | Is there a maximum number of slot definitions per grid (cap at 50)? Large grids may indicate misconfiguration. | Product Owner | Open |
