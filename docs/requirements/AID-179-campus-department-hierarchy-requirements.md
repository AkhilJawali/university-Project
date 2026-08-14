# Requirements Document — Master Data: Campus & Department Hierarchy

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-179 |
| Story URL | https://akhiljawali.atlassian.net/browse/AID-179 |
| Subtask Key | AID-203 (Requirement Generation) |
| BRD Reference | Section 6.1 — Master Data Management |
| Priority | High |
| Story Points | 5 |
| Status | Draft |
| Author | Akhil Jawali |
| Date | 12 August 2026 |

---

## 1. Introduction

This document captures the detailed functional and technical requirements for managing the hierarchical master data that forms the foundation of the University Timetable Management System (UTMS). All other modules (scheduling, conflict detection, approval, reporting) depend on this data being accurate, consistent, and properly structured.

The hierarchy is: **Campus → Department → Program → Batch → Section**

---

## 2. User Story

> As a System Administrator, I want to manage hierarchical master data (campuses, departments, programs, batches/sections) so that all scheduling entities are properly organized and referenced.

---

## 3. Actors

| Actor | Interaction |
|-------|-------------|
| System Administrator | Full CRUD on all hierarchy entities |
| IT Admin | Full CRUD on all hierarchy entities |
| Registrar | Read access to full hierarchy; may create/update campuses |
| HOD | Read access to own department and below |
| Department Coordinator | Read access to own department and below |

---

## 4. Functional Requirements

### FR-1: Campus Management

| ID | Requirement |
|----|-------------|
| FR-1.1 | The system shall support Create, Read, Update, and Soft-Delete operations for Campus entities. |
| FR-1.2 | Each Campus shall have: `id`, `name`, `code` (unique), `address`, `city`, `state`, `timezone`, `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`. |
| FR-1.3 | Campus `code` must be unique across the system. Duplicate codes shall be rejected with a validation error. |
| FR-1.4 | Soft-delete: setting `deleted_at` timestamp. A soft-deleted campus shall not appear in active listings but remains queryable for audit. |
| FR-1.5 | A campus cannot be soft-deleted if it has active (non-deleted) departments referencing it. The system shall return a referential integrity error. |

### FR-2: Department Management

| ID | Requirement |
|----|-------------|
| FR-2.1 | The system shall support CRUD operations for Department entities. |
| FR-2.2 | Each Department shall have: `id`, `name`, `code` (unique within campus), `campus_id` (FK), `hod_faculty_id` (FK, nullable), `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`. |
| FR-2.3 | A Department must reference an existing, active Campus. Creating a department with a non-existent or deleted `campus_id` shall be rejected. |
| FR-2.4 | Department `code` must be unique within its parent campus. |
| FR-2.5 | A department cannot be deleted if it has active programs referencing it. |

### FR-3: Program Management

| ID | Requirement |
|----|-------------|
| FR-3.1 | The system shall support CRUD operations for Program entities. |
| FR-3.2 | Each Program shall have: `id`, `name`, `code` (unique within department), `department_id` (FK), `duration_years`, `total_semesters`, `degree_type` (enum: UG, PG, PhD, Diploma), `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`. |
| FR-3.3 | A Program must reference an existing, active Department. |
| FR-3.4 | Program `code` must be unique within its parent department. |
| FR-3.5 | A program cannot be deleted if it has active batches referencing it. |

### FR-4: Batch Management

| ID | Requirement |
|----|-------------|
| FR-4.1 | The system shall support CRUD operations for Batch entities. |
| FR-4.2 | Each Batch shall have: `id`, `name`, `program_id` (FK), `academic_year`, `semester_number`, `strength` (student count), `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`. |
| FR-4.3 | A Batch must reference an existing, active Program. |
| FR-4.4 | `strength` must be a positive integer (> 0). |
| FR-4.5 | A batch cannot be deleted if it has active sections referencing it. |

### FR-5: Section Management

| ID | Requirement |
|----|-------------|
| FR-5.1 | The system shall support CRUD operations for Section entities. |
| FR-5.2 | Each Section shall have: `id`, `name` (e.g., "A", "B"), `batch_id` (FK), `strength` (student count for this section), `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`. |
| FR-5.3 | A Section must reference an existing, active Batch. |
| FR-5.4 | The sum of section strengths within a batch should not exceed the batch strength (warning, not hard block). |
| FR-5.5 | Section `name` must be unique within its parent batch. |

### FR-6: Referential Integrity (Cross-Cutting)

| ID | Requirement |
|----|-------------|
| FR-6.1 | All foreign key references must be validated before persisting any entity. |
| FR-6.2 | Attempting to reference a non-existent parent entity shall return HTTP 400 with a field-level error message identifying the invalid reference. |
| FR-6.3 | Attempting to delete a parent entity that has active children shall return HTTP 409 (Conflict) with a message listing the dependent entities. |
| FR-6.4 | Cascade rules: No hard cascading deletes. All deletions are soft-deletes requiring children to be removed/deactivated first. |

### FR-7: Listing & Querying

| ID | Requirement |
|----|-------------|
| FR-7.1 | The system shall provide paginated listing endpoints for each entity type. |
| FR-7.2 | Listings shall support filtering by `is_active`, parent entity ID, and name/code search. |
| FR-7.3 | The system shall provide a full hierarchy tree endpoint: given a campus ID, return all departments → programs → batches → sections nested. |
| FR-7.4 | All listings default to showing only active (non-deleted) records unless `include_deleted=true` is specified. |

---

## 5. Validation Rules

| Field | Rule |
|-------|------|
| `name` | Required, 1-200 characters, trimmed |
| `code` | Required, 2-20 characters, alphanumeric + hyphen only, uppercase |
| `strength` | Required (for batch/section), positive integer, max 10000 |
| `duration_years` | Required (for program), 1-8 |
| `total_semesters` | Required (for program), 1-16 |
| `campus_id`, `department_id`, etc. | Required FK, must reference existing active entity |

---

## 6. Non-Functional Requirements

| Category | Requirement |
|----------|-------------|
| Performance | CRUD operations respond within 200ms for single entity; hierarchy tree within 500ms |
| Security | All endpoints require authentication; RBAC enforced (Admin/Registrar for write, others read-only on their scope) |
| Input Validation | All inputs validated server-side using allowlist approach; reject malformed input early |
| Parameterized Queries | All database access uses parameterized queries — no string concatenation |
| Audit | Every create/update/delete operation logged in audit trail within the same transaction |
| Data Segregation | HODs and Coordinators see only their campus/department data (RLS enforcement) |

---

## 7. Acceptance Criteria

| # | Given | When | Then |
|---|-------|------|------|
| AC-1 | A valid campus payload | I create a campus | It appears in the hierarchy list with correct attributes |
| AC-2 | A department references a non-existent campus | I submit the department | The system rejects with HTTP 400 and a referential integrity error message |
| AC-3 | A batch belongs to a program | I attempt to delete the program | The system returns HTTP 409 and prevents deletion, listing dependent batches |
| AC-4 | A campus code already exists | I create another campus with the same code | The system rejects with a uniqueness validation error |
| AC-5 | A section's strength would cause batch total to exceed batch strength | I create the section | The system creates it but returns a warning in the response |
| AC-6 | An unauthenticated request | Any CRUD endpoint is called | The system returns HTTP 401 |
| AC-7 | A coordinator requests data from another department | They call the listing API | The system returns only their department's data (RLS) |

---

## 8. Data Model (Conceptual)

```
Campus (1) ──→ (N) Department (1) ──→ (N) Program (1) ──→ (N) Batch (1) ──→ (N) Section
```

All entities share common audit columns:
- `created_at` (timestamp, auto-set)
- `updated_at` (timestamp, auto-updated)
- `created_by` (user ID from JWT)
- `updated_by` (user ID from JWT)
- `deleted_at` (nullable timestamp for soft-delete)

---

## 9. Dependencies

| Dependency | Description |
|------------|-------------|
| Authentication (Task 1.6) | JWT auth must be in place for user identification |
| Audit Trail (Story AID-200) | Audit service must be available for logging mutations |
| RBAC (Story AID-197) | Role enforcement needed for write/read scoping |
| Database (Task 1.2) | PostgreSQL with migrations framework ready |

---

## 10. Open Questions

- Confirm maximum number of campuses expected (for index optimization).
- Confirm whether campus timezone affects slot-grid interpretation or is informational only.
- Confirm soft-delete vs. hard-delete policy for historical data retention.

---

## 11. Traceability

| BRD Section | Requirement IDs |
|-------------|-----------------|
| 6.1 — Master Data Management (Campus hierarchy) | FR-1 through FR-7 |
| 8 — Security & Access Control | NFR (Security, Data Segregation) |
| 8 — Auditability | NFR (Audit) |
| 7.1 — Structural/Curriculum Parameters | FR-3 (Program structure) |
