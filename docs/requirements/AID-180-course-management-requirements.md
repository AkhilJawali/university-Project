# Requirements Document — Master Data: Course Management

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-180 |
| Story URL | https://akhiljawali.atlassian.net/browse/AID-180 |
| Subtask Key | AID-207 (Requirement Generation) |
| BRD Reference | Section 6.1 — Master Data Management |
| Priority | High |
| Story Points | 5 |
| Status | Draft |
| Author | Akhil Jawali |
| Date | 13 August 2026 |

---

## 1. Introduction

This document captures the detailed functional and technical requirements for managing course/subject master data within the University Timetable Management System (UTMS). Courses are the primary teaching units that get scheduled — each course has a defined L-T-P (Lecture-Tutorial-Practical) structure that determines how many sessions of each type need to be scheduled per week.

---

## 2. User Story

> As a System Administrator, I want to manage course/subject master data (L-T-P structure, credits, prerequisites, equipment tags) so that the scheduling engine can correctly allocate sessions.

---

## 3. Actors

| Actor | Interaction |
|-------|-------------|
| System Administrator | Full CRUD on all course entities |
| IT Admin | Full CRUD on all course entities |
| HOD | Create/update courses in own department |
| Department Coordinator | Read access to courses in own department |
| Faculty | Read access to courses they teach |

---

## 4. Functional Requirements

### FR-1: Course CRUD

| ID | Requirement |
|----|-------------|
| FR-1.1 | The system shall support Create, Read, Update, and Soft-Delete operations for Course entities. |
| FR-1.2 | Each Course shall have: `id`, `code` (unique), `name`, `department_id` (FK), `lecture_hours` (L), `tutorial_hours` (T), `practical_hours` (P), `credit_hours`, `course_type` (enum: CORE, ELECTIVE, AUDIT, LAB), `prerequisites` (array of course IDs), `equipment_tags` (JSONB array), `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`. |
| FR-1.3 | Course `code` must be unique across the system. Duplicate codes shall be rejected with a validation error. |
| FR-1.4 | Soft-delete: setting `deleted_at` timestamp. A soft-deleted course shall not appear in active listings. |
| FR-1.5 | A course cannot be soft-deleted if it is currently assigned to a published timetable session in the active semester. |

### FR-2: L-T-P Structure

| ID | Requirement |
|----|-------------|
| FR-2.1 | Each course must define its Lecture (L), Tutorial (T), and Practical (P) hours per week as non-negative integers. |
| FR-2.2 | At least one of L, T, or P must be greater than 0 (a course cannot have 0-0-0). |
| FR-2.3 | Credit hours must be a positive integer and should logically correspond to L+T+P (warning if mismatch, not hard block). |

### FR-3: Prerequisites

| ID | Requirement |
|----|-------------|
| FR-3.1 | A course may reference zero or more prerequisite courses by their IDs. |
| FR-3.2 | All referenced prerequisite course IDs must exist and be active. |
| FR-3.3 | Circular prerequisites shall be rejected (A requires B, B requires A). |

### FR-4: Equipment Tags

| ID | Requirement |
|----|-------------|
| FR-4.1 | Each course may specify equipment tags (e.g., "projector", "computer-lab", "chemistry-lab") as a JSONB array. |
| FR-4.2 | Equipment tags are used by the scheduling engine to match courses to rooms with compatible equipment. |
| FR-4.3 | Tags must be non-empty lowercase strings, max 50 characters each, max 10 tags per course. |

### FR-5: Department Association

| ID | Requirement |
|----|-------------|
| FR-5.1 | Every course must belong to exactly one department. |
| FR-5.2 | The department must exist and be active. |
| FR-5.3 | Cross-listed courses (shared across departments) are represented by creating the course in one department and marking it with a `is_cross_listed` flag. |

### FR-6: Listing & Querying

| ID | Requirement |
|----|-------------|
| FR-6.1 | The system shall provide paginated listing endpoints for courses. |
| FR-6.2 | Listings shall support filtering by `department_id`, `course_type`, `is_active`, and name/code search. |
| FR-6.3 | All listings default to showing only active (non-deleted) records unless `include_deleted=true` is specified. |

---

## 5. Validation Rules

| Field | Rule |
|-------|------|
| `code` | Required, 2-20 characters, alphanumeric + hyphen, uppercase |
| `name` | Required, 1-200 characters, trimmed |
| `lecture_hours` | Required, non-negative integer, max 10 |
| `tutorial_hours` | Required, non-negative integer, max 10 |
| `practical_hours` | Required, non-negative integer, max 10 |
| `credit_hours` | Required, positive integer (1-20) |
| `course_type` | Required, one of: CORE, ELECTIVE, AUDIT, LAB |
| `department_id` | Required FK, must reference existing active department |
| `equipment_tags` | Optional, max 10 items, each lowercase string max 50 chars |
| `prerequisites` | Optional, array of existing active course IDs, no circular references |

---

## 6. Non-Functional Requirements

| Category | Requirement |
|----------|-------------|
| Performance | CRUD operations respond within 200ms for single entity |
| Security | All endpoints require authentication; RBAC enforced (Admin/HOD for write, others read-only on their scope) |
| Input Validation | All inputs validated server-side using allowlist approach |
| Parameterized Queries | All database access uses parameterized queries |
| Audit | Every create/update/delete operation logged in audit trail within the same transaction |
| Data Segregation | HODs and Coordinators see only courses in their department (RLS) |

---

## 7. Acceptance Criteria

| # | Given | When | Then |
|---|-------|------|------|
| AC-1 | A valid course payload with L-T-P = 3-1-2 | I create a course | It appears in the listing with correct L-T-P and credit hours |
| AC-2 | A course references a non-existent department | I submit the course | The system rejects with HTTP 400 and field-level error |
| AC-3 | A course code already exists | I create another course with the same code | The system rejects with a uniqueness error |
| AC-4 | A course has L=0, T=0, P=0 | I submit the course | The system rejects with "At least one of L, T, P must be > 0" |
| AC-5 | A course lists prerequisite B, and B lists prerequisite A | I submit course A | The system rejects with circular dependency error |
| AC-6 | A course specifies equipment_tags=["projector", "whiteboard"] | I create the course | Tags are stored and queryable |
| AC-7 | An HOD requests courses from another department | They call the listing API | The system returns only their department's courses (RLS) |

---

## 8. Data Model (Conceptual)

```
Department (1) ──→ (N) Course
Course (N) ──→ (N) Course (prerequisites - self-referencing)
```

Course fields:
- `id`, `code`, `name`, `department_id`, `lecture_hours`, `tutorial_hours`, `practical_hours`
- `credit_hours`, `course_type`, `is_cross_listed`, `prerequisites` (JSONB array of IDs)
- `equipment_tags` (JSONB array of strings)
- Audit columns: `created_at`, `updated_at`, `created_by`, `updated_by`, `deleted_at`

---

## 9. Dependencies

| Dependency | Description |
|------------|-------------|
| Department (AID-179) | Department entities must exist for FK validation |
| Authentication (JWT) | User identification for audit and RBAC |
| Audit Trail (AID-200) | Audit service for logging mutations |
| RBAC (AID-197) | Role enforcement for write/read scoping |

---

## 10. Open Questions

- Should `credit_hours` be auto-calculated from L+T+P or manually entered?
- How should cross-listed courses be handled in the scheduling engine (counted once or per department)?
- Maximum number of prerequisites allowed per course?

---

## 11. Traceability

| BRD Section | Requirement IDs |
|-------------|-----------------|
| 6.1 — Master Data Management (Courses) | FR-1 through FR-6 |
| 7.1 — Structural/Curriculum Parameters | FR-2 (L-T-P structure) |
| 8 — Security & Access Control | NFR (Security, Data Segregation) |
