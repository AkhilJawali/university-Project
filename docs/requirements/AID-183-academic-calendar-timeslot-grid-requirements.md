# Requirements Document — Academic Calendar & Time-Slot Grid

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-183 |
| Story URL | https://akhiljawali.atlassian.net/browse/AID-183 |
| Subtask Key | AID-219 (Requirement Generation) |
| BRD Reference | Section 6.1 — Master Data Management |
| Priority | High |
| Story Points | 5 |
| Status | Draft |
| Author | Akhil Jawali |
| Date | 16 August 2026 |

---

## 1. Introduction

This document captures the detailed functional and technical requirements for managing academic calendars and time-slot grids within the University Timetable Management System (UTMS). The academic calendar defines the institutional time boundaries (semesters, holidays, exam windows), while the time-slot grid defines the daily scheduling periods per campus. Together, they form the temporal framework within which the scheduling engine operates.

---

## 2. User Story

> As a System Administrator, I want to configure academic calendars (semester dates, holidays, exam windows) and campus-specific time-slot grids so that scheduling respects institutional time boundaries.

---

## 3. Actors

| Actor | Interaction |
|-------|-------------|
| System Administrator | Full CRUD on calendars and time-slot grids |
| IT Admin | Full CRUD on calendars and time-slot grids |
| Registrar | Create/update academic calendars; view time-slot grids |
| HOD | Read access to academic calendar for their campus |
| Department Coordinator | Read access; reference calendar for scheduling |

---

## 4. Functional Requirements

### FR-1: Academic Calendar CRUD

| ID | Requirement |
|----|-------------|
| FR-1.1 | The system shall support Create, Read, Update, and Soft-Delete operations for Academic Calendar entities. |
| FR-1.2 | Each Academic Calendar shall have: `id`, `name` (e.g., "Odd Semester 2026"), `campus_id` (FK), `academic_year` (e.g., "2026-2027"), `semester_type` (enum: ODD, EVEN, SUMMER), `start_date`, `end_date`, `is_active`, audit columns. |
| FR-1.3 | A calendar must reference an existing, active Campus. |
| FR-1.4 | Start date must be before end date. |
| FR-1.5 | Calendars within the same campus must not have overlapping date ranges for the same semester type. |

### FR-2: Holiday Management

| ID | Requirement |
|----|-------------|
| FR-2.1 | Each academic calendar may have multiple holidays. |
| FR-2.2 | Each Holiday shall have: `id`, `calendar_id` (FK), `name` (e.g., "Diwali"), `date`, `day_type` (enum: FULL_DAY, HALF_DAY_AM, HALF_DAY_PM), `is_recurring` (boolean). |
| FR-2.3 | A holiday date must fall within the parent calendar's start_date and end_date range. |
| FR-2.4 | The scheduling engine must exclude holidays from available scheduling days. |
| FR-2.5 | If a holiday is added after timetable generation, the system must flag any sessions scheduled on that date for rescheduling (calendar change impact detection). |

### FR-3: Exam Windows

| ID | Requirement |
|----|-------------|
| FR-3.1 | Each academic calendar may define one or more exam windows. |
| FR-3.2 | Each Exam Window shall have: `id`, `calendar_id` (FK), `name` (e.g., "Mid-Sem Exams"), `exam_type` (enum: MID_SEM, END_SEM, SUPPLEMENTARY), `start_date`, `end_date`. |
| FR-3.3 | Exam windows must fall within the parent calendar's date range. |
| FR-3.4 | During exam windows, regular lectures are not scheduled (engine treats these dates as blocked for regular sessions). |

### FR-4: Orientation & Special Periods

| ID | Requirement |
|----|-------------|
| FR-4.1 | The calendar may define special periods: orientation weeks, registration windows, break periods. |
| FR-4.2 | Each Special Period shall have: `id`, `calendar_id` (FK), `name`, `period_type` (enum: ORIENTATION, REGISTRATION, BREAK, REVISION), `start_date`, `end_date`. |
| FR-4.3 | Special periods affect scheduling availability (e.g., no lectures during break periods). |

### FR-5: Time-Slot Grid Configuration

| ID | Requirement |
|----|-------------|
| FR-5.1 | The system shall support CRUD operations for Time-Slot Grid entities per campus. |
| FR-5.2 | Each Time-Slot Grid shall have: `id`, `campus_id` (FK), `name` (e.g., "Main Campus Standard Grid"), `effective_from` (date), `is_active`, audit columns. |
| FR-5.3 | Each campus has exactly one active time-slot grid at any time. Activating a new grid deactivates the previous one. |

### FR-6: Time-Slot Definitions

| ID | Requirement |
|----|-------------|
| FR-6.1 | Each time-slot grid contains multiple slot definitions. |
| FR-6.2 | Each Slot Definition shall have: `id`, `grid_id` (FK), `slot_number` (order), `start_time`, `end_time`, `slot_type` (enum: LECTURE, TUTORIAL, PRACTICAL, BREAK, LUNCH), `duration_minutes`. |
| FR-6.3 | `duration_minutes` must be auto-calculated from start_time and end_time, or validated if manually entered. |
| FR-6.4 | Slots within a grid must not overlap in time. |
| FR-6.5 | Slots must be ordered sequentially (slot_number reflects chronological order). |
| FR-6.6 | The grid must support mixed slot durations (e.g., 60-min lectures, 90-min tutorials, 180-min labs). |
| FR-6.7 | BREAK and LUNCH slots are non-schedulable — the engine skips them. |

### FR-7: Working Days Configuration

| ID | Requirement |
|----|-------------|
| FR-7.1 | Each time-slot grid defines which days of the week are working days. |
| FR-7.2 | Working days are stored as: `grid_id`, `day_of_week` (1=Monday through 7=Sunday), `is_working_day` (boolean). |
| FR-7.3 | Default: Monday through Saturday working; Sunday off. Configurable per campus. |

### FR-8: Calendar Change Impact Detection

| ID | Requirement |
|----|-------------|
| FR-8.1 | When a holiday is added to an active calendar that already has a published timetable, the system must detect sessions scheduled on that date. |
| FR-8.2 | Affected sessions shall be flagged with a "needs rescheduling" status. |
| FR-8.3 | The system shall provide a list of impacted sessions with their details (course, faculty, room, batch). |

### FR-9: Listing & Querying

| ID | Requirement |
|----|-------------|
| FR-9.1 | The system shall provide paginated listing endpoints for academic calendars, filtered by campus and academic year. |
| FR-9.2 | The system shall provide endpoints to retrieve holidays, exam windows, and special periods for a given calendar. |
| FR-9.3 | The system shall provide an endpoint to retrieve the active time-slot grid for a campus. |
| FR-9.4 | The system shall provide an endpoint to retrieve all slot definitions for a grid. |

---

## 5. Validation Rules

| Field | Rule |
|-------|------|
| Calendar `name` | Required, 1-200 characters |
| Calendar `academic_year` | Required, format "YYYY-YYYY" (e.g., "2026-2027") |
| Calendar `campus_id` | Required FK, must reference active campus |
| Calendar `start_date` | Required, must be before `end_date` |
| Calendar `semester_type` | Required, one of: ODD, EVEN, SUMMER |
| Holiday `name` | Required, 1-100 characters |
| Holiday `date` | Required, must be within parent calendar range |
| Holiday `day_type` | Required, one of: FULL_DAY, HALF_DAY_AM, HALF_DAY_PM |
| Exam Window `start_date` | Required, must be within calendar range, before `end_date` |
| Slot `start_time` | Required, must be before `end_time` |
| Slot `end_time` | Required, must be after `start_time` |
| Slot `slot_type` | Required, one of: LECTURE, TUTORIAL, PRACTICAL, BREAK, LUNCH |
| Slot `duration_minutes` | Auto-calculated or validated, > 0, max 300 |
| Grid working days | At least 1 working day required |

---

## 6. Non-Functional Requirements

| Category | Requirement |
|----------|-------------|
| Performance | CRUD operations respond within 200ms; calendar with all sub-entities within 500ms |
| Security | All endpoints require authentication; RBAC enforced |
| Input Validation | All inputs validated server-side using allowlist approach |
| Parameterized Queries | All database access uses parameterized queries |
| Audit | Every create/update/delete operation logged in audit trail within same transaction |
| Data Segregation | HODs and Coordinators see only calendars for their campus (RLS) |

---

## 7. Acceptance Criteria

| # | Given | When | Then |
|---|-------|------|------|
| AC-1 | A valid academic calendar payload | I create a calendar | It appears in the listing with correct semester dates |
| AC-2 | A calendar references a non-existent campus | I submit | The system rejects with HTTP 400 |
| AC-3 | Two calendars for the same campus and semester type have overlapping dates | I create the second one | The system rejects with a conflict error |
| AC-4 | A holiday date is outside the calendar's date range | I add the holiday | The system rejects with validation error |
| AC-5 | I add a holiday to a calendar with published sessions on that date | Holiday is saved | Affected sessions are flagged "needs rescheduling" |
| AC-6 | I define time slots with overlapping times | I save the grid | The system rejects with overlap error |
| AC-7 | I activate a new grid for a campus | Activation succeeds | Previous grid is automatically deactivated |
| AC-8 | The scheduling engine reads the grid | It queries the active grid | Only LECTURE/TUTORIAL/PRACTICAL slots are available for scheduling (BREAK/LUNCH excluded) |

---

## 8. Data Model (Conceptual)

```
Campus (1) ──→ (N) AcademicCalendar (1) ──→ (N) Holiday
                                        (1) ──→ (N) ExamWindow
                                        (1) ──→ (N) SpecialPeriod

Campus (1) ──→ (N) TimeSlotGrid (1) ──→ (N) SlotDefinition
                               (1) ──→ (N) WorkingDay
```

---

## 9. Dependencies

| Dependency | Description |
|------------|-------------|
| Campus (AID-179) | Campus entities must exist for FK validation |
| Scheduling Engine | Reads calendar + grid data to determine valid scheduling slots |
| Conflict Detection | Uses calendar to detect sessions on holidays |
| Authentication (JWT) | User identification for audit and RBAC |
| Audit Trail (AID-200) | Audit service for logging mutations |

---

## 10. Open Questions

- Should campuses be allowed to have multiple active calendars (e.g., one per department) or strictly one per campus?
- How far in advance can calendars be created (limit to current + next academic year)?
- Should time-slot grid changes propagate to already-generated timetables, or only affect future generations?
- Should recurring holidays auto-populate from the previous year's calendar?

---

## 11. Traceability

| BRD Section | Requirement IDs |
|-------------|-----------------|
| 6.1 — Master Data Management (Academic Calendar) | FR-1, FR-2, FR-3, FR-4 |
| 6.1 — Master Data Management (Time-Slot Grid) | FR-5, FR-6, FR-7 |
| 7 — Scheduling Engine (Calendar constraints) | FR-8 |
| 8 — Security & Access Control | NFR (Security, Data Segregation) |
