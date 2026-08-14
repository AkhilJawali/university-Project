# Requirements Document — Master Data: Room & Resource Management

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-182 |
| Story URL | https://akhiljawali.atlassian.net/browse/AID-182 |
| Subtask Key | AID-215 (Requirement Generation) |
| BRD Reference | Section 6.1 — Master Data Management |
| Priority | High |
| Story Points | 5 |
| Status | Draft |
| Author | Akhil Jawali |
| Date | 13 August 2026 |

---

## 1. Introduction

This document captures the detailed functional and technical requirements for managing rooms, labs, and resource blocks within the University Timetable Management System (UTMS). Rooms are the physical spaces where sessions are scheduled — each has a capacity, type, equipment, and may be subject to temporary blocks (maintenance, events).

---

## 2. User Story

> As a System Administrator, I want to manage rooms and labs (capacity, equipment tags, resource blocks) so that the scheduling engine can allocate appropriate spaces.

---

## 3. Actors

| Actor | Interaction |
|-------|-------------|
| System Administrator | Full CRUD on rooms and resource blocks |
| IT Admin | Full CRUD on rooms and resource blocks |
| Registrar | Read access; approve resource blocks that impact published sessions |
| HOD | Read access to rooms in own campus |
| Department Coordinator | Read access; raise resource block requests |
| Facilities Team | Update room status; raise maintenance blocks |

---

## 4. Functional Requirements

### FR-1: Room CRUD

| ID | Requirement |
|----|-------------|
| FR-1.1 | The system shall support Create, Read, Update, and Soft-Delete operations for Room entities. |
| FR-1.2 | Each Room shall have: `id`, `code` (unique within campus), `name`, `campus_id` (FK), `building`, `floor`, `capacity`, `room_type` (enum: LECTURE_HALL, LAB, SEMINAR, AUDITORIUM, TUTORIAL), `equipment_tags` (JSONB array), `is_active`, `created_at`, `updated_at`, `created_by`, `updated_by`. |
| FR-1.3 | Room `code` must be unique within its campus. |
| FR-1.4 | Soft-delete: setting `deleted_at` timestamp. |
| FR-1.5 | A room cannot be soft-deleted if it has sessions assigned in a published timetable for the current semester. |

### FR-2: Capacity & Type

| ID | Requirement |
|----|-------------|
| FR-2.1 | Room capacity must be a positive integer (1-5000). |
| FR-2.2 | The scheduling engine uses capacity to ensure assigned section strength does not exceed room capacity. |
| FR-2.3 | Room type determines which session types can be scheduled (e.g., LAB type for practical sessions). |

### FR-3: Equipment Tags

| ID | Requirement |
|----|-------------|
| FR-3.1 | Each room may specify equipment tags (e.g., "projector", "computers-40", "chemistry-fume-hood") as a JSONB array. |
| FR-3.2 | The scheduling engine matches course equipment requirements to room equipment tags. |
| FR-3.3 | Tags must be non-empty lowercase strings, max 50 characters each, max 20 tags per room. |

### FR-4: Resource Blocks

| ID | Requirement |
|----|-------------|
| FR-4.1 | A resource block temporarily makes a room unavailable for a defined period. |
| FR-4.2 | Each resource block has: `id`, `room_id` (FK), `reason`, `start_date`, `end_date`, `start_time` (nullable for full-day), `end_time`, `status` (enum: REQUESTED, APPROVED, ACTIVE, RELEASED), `requested_by`, `approved_by`. |
| FR-4.3 | A block with status ACTIVE prevents the engine from scheduling sessions in that room during the block period. |
| FR-4.4 | If a block is raised on a room with already-published sessions during the block period, the system must flag those sessions for displacement. |
| FR-4.5 | Block approval is required when the block impacts published sessions (approval gating). |
| FR-4.6 | Blocks can be released early (status → RELEASED), making the room available again. |

### FR-5: Listing & Querying

| ID | Requirement |
|----|-------------|
| FR-5.1 | The system shall provide paginated listing endpoints for rooms. |
| FR-5.2 | Listings shall support filtering by `campus_id`, `building`, `room_type`, `min_capacity`, `equipment_tags`, and `is_active`. |
| FR-5.3 | The system shall provide a room availability endpoint: given a room ID and date range, return available time slots (excluding blocks and scheduled sessions). |
| FR-5.4 | Listings shall support filtering resource blocks by `room_id`, `status`, and date range. |

---

## 5. Validation Rules

| Field | Rule |
|-------|------|
| `code` | Required, 2-20 characters, alphanumeric + hyphen, uppercase |
| `name` | Required, 1-200 characters, trimmed |
| `campus_id` | Required FK, must reference existing active campus |
| `building` | Required, 1-100 characters |
| `floor` | Optional, 1-20 characters |
| `capacity` | Required, positive integer, 1-5000 |
| `room_type` | Required, one of: LECTURE_HALL, LAB, SEMINAR, AUDITORIUM, TUTORIAL |
| `equipment_tags` | Optional, max 20 items, each lowercase string max 50 chars |
| Block `start_date` | Required, must be today or future |
| Block `end_date` | Required, must be >= start_date |
| Block `reason` | Required, 1-500 characters |

---

## 6. Non-Functional Requirements

| Category | Requirement |
|----------|-------------|
| Performance | CRUD operations respond within 200ms; availability query within 500ms |
| Security | All endpoints require authentication; RBAC enforced |
| Input Validation | All inputs validated server-side using allowlist approach |
| Parameterized Queries | All database access uses parameterized queries |
| Audit | Every create/update/delete operation logged in audit trail within the same transaction |
| Data Segregation | Coordinators see only rooms in their campus (RLS) |

---

## 7. Acceptance Criteria

| # | Given | When | Then |
|---|-------|------|------|
| AC-1 | A valid room payload with capacity=60, type=LECTURE_HALL | I create a room | It appears in the listing with correct attributes |
| AC-2 | A room references a non-existent campus | I submit | The system rejects with HTTP 400 |
| AC-3 | A room code already exists in the same campus | I create another room with same code | The system rejects with uniqueness error |
| AC-4 | A resource block is raised on a room with published sessions | Block is submitted | System flags impacted sessions for displacement |
| AC-5 | A block status is ACTIVE | Engine tries to schedule in that room during block period | Engine excludes the room from allocation |
| AC-6 | I query room availability for next week | I call the availability endpoint | Returns only free slots (excluding blocks and sessions) |
| AC-7 | Equipment tags include "projector" | I filter rooms by equipment_tags=projector | Only rooms with that tag are returned |

---

## 8. Data Model (Conceptual)

```
Campus (1) ──→ (N) Room
Room (1) ──→ (N) ResourceBlock
```

Room fields:
- `id`, `code`, `name`, `campus_id`, `building`, `floor`, `capacity`
- `room_type`, `equipment_tags` (JSONB), audit columns, `deleted_at`

ResourceBlock fields:
- `id`, `room_id`, `reason`, `start_date`, `end_date`, `start_time`, `end_time`
- `status`, `requested_by`, `approved_by`, audit columns

---

## 9. Dependencies

| Dependency | Description |
|------------|-------------|
| Campus (AID-179) | Campus entities must exist for FK validation |
| Authentication (JWT) | User identification for audit and RBAC |
| Audit Trail (AID-200) | Audit service for logging mutations |
| RBAC (AID-197) | Role enforcement for write/read scoping |
| Scheduling Engine | Reads room data and blocks for allocation decisions |

---

## 10. Open Questions

- Should room equipment tags be a controlled vocabulary (predefined list) or free-form?
- How far in advance can resource blocks be raised?
- Should room capacity include a "usable capacity" vs "max capacity" distinction?

---

## 11. Traceability

| BRD Section | Requirement IDs |
|-------------|-----------------|
| 6.1 — Master Data Management (Rooms) | FR-1 through FR-5 |
| 6.6 — Room & Resource Management | FR-4 (Resource Blocks) |
| 8 — Security & Access Control | NFR (Security, Data Segregation) |
