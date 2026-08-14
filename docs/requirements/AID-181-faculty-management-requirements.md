# Requirements Document — Master Data: Faculty Management

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-181 |
| Story URL | https://akhiljawali.atlassian.net/browse/AID-181 |
| Subtask Key | AID-211 (Requirement Generation) |
| BRD Reference | Section 6.1 — Master Data Management |
| Priority | High |
| Story Points | 5 |
| Status | Draft |
| Author | Akhil Jawali |
| Date | 13 August 2026 |

---

## 1. Introduction

This document captures the detailed functional and technical requirements for managing faculty profiles within the University Timetable Management System (UTMS). Faculty data includes availability preferences, subject competencies, multi-campus associations, and workload configuration — all of which feed into the scheduling engine as constraints.

---

## 2. User Story

> As a System Administrator, I want to manage faculty profiles (availability, competencies, multi-campus associations, workload config) so that the scheduling engine respects faculty constraints.

---

## 3. Actors

| Actor | Interaction |
|-------|-------------|
| System Administrator | Full CRUD on all faculty profiles |
| IT Admin | Full CRUD on all faculty profiles |
| HOD | Create/update faculty in own department; view workload |
| Faculty | Update own availability preferences; view own schedule |
| Department Coordinator | Read access to faculty in own department |

---

## 4. Functional Requirements

### FR-1: Faculty Profile CRUD

| ID | Requirement |
|----|-------------|
| FR-1.1 | The system shall support Create, Read, Update, and Soft-Delete operations for Faculty entities. |
| FR-1.2 | Each Faculty shall have: `id`, `employee_id` (unique), `first_name`, `last_name`, `email` (unique), `phone`, `department_id` (FK), `cadre` (enum: Professor, Associate Professor, Assistant Professor, Lecturer, Visiting), `qualification`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`. |
| FR-1.3 | Faculty `employee_id` and `email` must be unique across the system. |
| FR-1.4 | Soft-delete: setting `deleted_at` timestamp. A soft-deleted faculty shall not appear in active listings. |
| FR-1.5 | A faculty cannot be soft-deleted if they have sessions assigned in a published timetable for the current/upcoming semester. |

### FR-2: Availability Windows

| ID | Requirement |
|----|-------------|
| FR-2.1 | Each faculty may define availability windows indicating when they are available/unavailable for scheduling. |
| FR-2.2 | An availability window has: `day_of_week`, `start_time`, `end_time`, `constraint_type` (HARD_UNAVAILABLE, SOFT_PREFERRED, SOFT_AVOID). |
| FR-2.3 | HARD_UNAVAILABLE windows are absolute — the engine must never schedule this faculty during these times. |
| FR-2.4 | SOFT_PREFERRED/SOFT_AVOID are optimization hints — the engine tries to respect them but may relax if needed. |
| FR-2.5 | A faculty's availability windows must not have overlapping time ranges for the same day. |

### FR-3: Subject Competencies

| ID | Requirement |
|----|-------------|
| FR-3.1 | Each faculty may be associated with multiple courses they are competent to teach. |
| FR-3.2 | Competencies are stored as a junction: `faculty_id` + `course_id`. |
| FR-3.3 | Referenced course must exist and be active. |
| FR-3.4 | The scheduling engine uses competencies to validate faculty-course assignments. |

### FR-4: Multi-Campus Associations

| ID | Requirement |
|----|-------------|
| FR-4.1 | A faculty may be associated with multiple campuses (teaching across campuses). |
| FR-4.2 | Associations are stored as: `faculty_id` + `campus_id` + `travel_time_minutes` (buffer between campuses). |
| FR-4.3 | The scheduling engine uses travel_time_minutes to enforce gaps between sessions at different campuses. |

### FR-5: Workload Configuration

| ID | Requirement |
|----|-------------|
| FR-5.1 | Each faculty has workload limits defined by their cadre: `min_weekly_hours`, `max_weekly_hours`. |
| FR-5.2 | Workload limits are configurable per cadre at the institution level. |
| FR-5.3 | The system must validate that assigned sessions do not exceed max_weekly_hours (hard constraint). |
| FR-5.4 | The system must flag (warning) when a faculty is below min_weekly_hours after scheduling. |

### FR-6: Listing & Querying

| ID | Requirement |
|----|-------------|
| FR-6.1 | The system shall provide paginated listing endpoints for faculty. |
| FR-6.2 | Listings shall support filtering by `department_id`, `cadre`, `campus_id`, `is_active`, and name/email search. |
| FR-6.3 | The system shall provide an endpoint to retrieve a faculty's full profile including availability, competencies, and campus associations. |

---

## 5. Validation Rules

| Field | Rule |
|-------|------|
| `employee_id` | Required, 2-20 characters, alphanumeric |
| `first_name` | Required, 1-100 characters |
| `last_name` | Required, 1-100 characters |
| `email` | Required, valid email format, max 200 characters |
| `phone` | Optional, 10-15 digits |
| `department_id` | Required FK, must reference existing active department |
| `cadre` | Required, one of: PROFESSOR, ASSOCIATE_PROFESSOR, ASSISTANT_PROFESSOR, LECTURER, VISITING |
| `min_weekly_hours` | Positive integer, cadre-dependent |
| `max_weekly_hours` | Positive integer, must be >= min_weekly_hours |
| Availability `start_time` | Must be before `end_time` |
| Availability `day_of_week` | 1-7 (Monday=1, Sunday=7) |

---

## 6. Non-Functional Requirements

| Category | Requirement |
|----------|-------------|
| Performance | CRUD operations respond within 200ms; full profile within 300ms |
| Security | All endpoints require authentication; RBAC enforced |
| Input Validation | All inputs validated server-side using allowlist approach |
| Parameterized Queries | All database access uses parameterized queries |
| Audit | Every create/update/delete operation logged in audit trail within the same transaction |
| Data Segregation | HODs see only faculty in their department (RLS) |

---

## 7. Acceptance Criteria

| # | Given | When | Then |
|---|-------|------|------|
| AC-1 | A valid faculty payload | I create a faculty profile | It appears in the listing with correct attributes |
| AC-2 | A faculty references a non-existent department | I submit | The system rejects with HTTP 400 |
| AC-3 | An employee_id already exists | I create another faculty with same employee_id | The system rejects with uniqueness error |
| AC-4 | Faculty sets HARD_UNAVAILABLE on Monday 9-11am | Scheduling engine runs | No sessions assigned to this faculty on Monday 9-11am |
| AC-5 | Two availability windows overlap on the same day | I submit | The system rejects with overlap error |
| AC-6 | Faculty is associated with 2 campuses with 30min travel time | Engine schedules back-to-back | Engine ensures 30min gap between cross-campus sessions |
| AC-7 | Faculty total assigned hours exceed max_weekly_hours | Engine validates | Engine flags constraint violation |

---

## 8. Data Model (Conceptual)

```
Department (1) ──→ (N) Faculty
Faculty (1) ──→ (N) FacultyAvailability
Faculty (N) ──→ (N) Course (competencies)
Faculty (N) ──→ (N) Campus (associations with travel_time)
```

---

## 9. Dependencies

| Dependency | Description |
|------------|-------------|
| Department (AID-179) | Department entities must exist for FK validation |
| Course (AID-180) | Course entities must exist for competency mapping |
| Campus (AID-179) | Campus entities must exist for multi-campus associations |
| Authentication (JWT) | User identification for audit and RBAC |
| Audit Trail (AID-200) | Audit service for logging mutations |

---

## 10. Open Questions

- Should faculty be able to update their own availability without HOD approval?
- Maximum number of campuses a faculty can be associated with?
- How to handle visiting faculty who may have shorter contract periods?

---

## 11. Traceability

| BRD Section | Requirement IDs |
|-------------|-----------------|
| 6.1 — Master Data Management (Faculty) | FR-1 through FR-6 |
| 7 — Faculty Availability & Workload | FR-2, FR-5 |
| 14 — Multi-Campus Coordination | FR-4 |
| 8 — Security & Access Control | NFR (Security, Data Segregation) |
