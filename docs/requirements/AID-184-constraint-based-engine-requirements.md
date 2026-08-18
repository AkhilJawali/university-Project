# Requirements Document — Timetable Generation: Constraint-Based Engine

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-184 |
| Story URL | https://akhiljawali.atlassian.net/browse/AID-184 |
| Subtask Key | AID-223 (Requirement Generation) |
| BRD Reference | Section 6.2 — Class/Lecture Timetable Generation, Section 6.10 — Scheduling Engine |
| Priority | High |
| Story Points | 8 |
| Status | Draft |
| Author | Akhil Jawali |
| Date | 18 August 2026 |

---

## 1. Introduction

This document captures the detailed functional and technical requirements for the Constraint-Based Scheduling Engine within the University Timetable Management System (UTMS). The engine is the core automated component that generates conflict-free timetables by modelling the scheduling problem as a Constraint Satisfaction Problem (CSP). It takes master data inputs (courses, faculty, rooms, batches, calendar, grid) and produces a valid weekly timetable assignment while satisfying all hard constraints and optimizing soft constraints.

The engine must operate within strict performance bounds (< 2 minutes for ~40 sections), support locked/pre-assigned sessions, handle infeasibility gracefully, and allow partial re-generation without disturbing approved sections.

---

## 2. User Story

> As a Department Coordinator, I want the system to automatically generate a conflict-free timetable using constraint satisfaction so that scheduling turnaround is reduced from weeks to days.

---

## 3. Actors

| Actor | Interaction |
|-------|-------------|
| Department Coordinator | Triggers generation, locks sessions, reviews output, accepts/rejects draft |
| HOD | Reviews generated draft quality score, approves for further workflow |
| Scheduling Engine (System) | Executes CSP algorithm, returns solution or partial solution with diagnostics |
| IT Admin | Configures engine parameters (timeout, suggestion limits) |

---

## 4. Functional Requirements

### FR-1: Constraint Model & Data Structures

| ID | Requirement |
|----|-------------|
| FR-1.1 | The system shall define a `HardConstraint` type representing inviolable rules (faculty double-booking, room double-booking, batch clashes, capacity overflow, equipment mismatch, hard block violations, travel-time violations, common-slot violations). |
| FR-1.2 | The system shall define a `SoftConstraint` type representing preference-based rules (faculty time-preferences, room proximity, gap minimization, day-pattern balance, consecutive vs. spread preferences). |
| FR-1.3 | Each constraint shall carry metadata: `id`, `type` (enum), `category` (HARD/SOFT), `weight` (for soft constraints, integer 1-10), `description`, `affectedEntities` (list of entity IDs). |
| FR-1.4 | The system shall implement a `ConstraintSet` builder that loads all applicable constraints from the database for a given generation scope (campus, department, semester). |
| FR-1.5 | The system shall define a `Variable` type representing a session-to-assign (course + batch + session type + contact hours). |
| FR-1.6 | The system shall define a `Domain` type representing the set of possible (time-slot, room) assignment pairs for each variable. |
| FR-1.7 | Soft constraint weights shall be configurable per institution/department without code changes (stored in configuration table). |

### FR-2: Constraint Propagation (AC-3 Variant)

| ID | Requirement |
|----|-------------|
| FR-2.1 | The system shall implement an arc-consistency algorithm (AC-3 variant) for domain pruning before and during search. |
| FR-2.2 | Propagation shall enforce all hard constraint types: (a) Faculty cannot teach two sessions in the same time-slot, (b) A room cannot host two sessions in the same time-slot, (c) A batch/section cannot attend two sessions in the same time-slot, (d) Room capacity must be >= batch strength, (e) Room equipment tags must satisfy course equipment requirements, (f) Sessions cannot be placed in hard-blocked slots, (g) Institution-level common slots (CCC/UWE) are pre-reserved and cannot be used for department sessions, (h) Cross-campus faculty require travel-time buffer between consecutive sessions at different campuses. |
| FR-2.3 | When propagation empties any variable's domain (no valid assignment possible), the system shall detect early infeasibility and report which variable became infeasible and which constraints caused it. |
| FR-2.4 | Propagation shall run incrementally after each variable assignment during backtracking (maintaining arc consistency). |

### FR-3: Backtracking Search with Heuristics

| ID | Requirement |
|----|-------------|
| FR-3.1 | The system shall implement a backtracking search algorithm to find a complete valid assignment. |
| FR-3.2 | Variable ordering shall use MRV (Minimum Remaining Values): the next variable to assign is the one with the smallest remaining domain. |
| FR-3.3 | Value ordering shall use LCV (Least Constraining Value): try assignments that eliminate the fewest options for other variables first. |
| FR-3.4 | The system shall support chronological backtracking (undo the most recent assignment on failure). |
| FR-3.5 | The system shall support conflict-directed backtracking: on failure, jump back to the variable responsible for the conflict rather than the most recent one. |
| FR-3.6 | Pre-assigned/locked sessions shall be treated as immovable variables with a fixed domain of size 1 (their current assignment). The engine must never move or reassign a locked session. |
| FR-3.7 | The search shall terminate when either: (a) a complete valid assignment is found, (b) timeout is reached, or (c) infeasibility is proven (all branches exhausted). |

### FR-4: Worker Thread Isolation & Timeout

| ID | Requirement |
|----|-------------|
| FR-4.1 | The scheduling engine shall execute in an isolated worker thread (separate from the API request thread) to prevent blocking the application. |
| FR-4.2 | A configurable timeout shall be enforced (default: 120 seconds). |
| FR-4.3 | If the timeout fires before a complete solution is found, the engine shall return the best partial solution found so far (most variables assigned without violation). |
| FR-4.4 | The worker shall report progress to the main thread at regular intervals (percentage of variables assigned, current quality score). |
| FR-4.5 | On infeasibility, the engine shall identify and return the Minimal Unsatisfiable Subset (MUS) — the smallest set of constraints that together make the problem infeasible. |
| FR-4.6 | The engine shall return a structured `EngineResult` containing: `status` (COMPLETE/PARTIAL/INFEASIBLE), `assignments` (list of variable→slot+room mappings), `feasibilityScore`, `qualityScore`, `unresolvedViolations` (list), `musReport` (if infeasible), `executionTimeMs`, `progressPercentage`. |

### FR-5: Generation Orchestrator

| ID | Requirement |
|----|-------------|
| FR-5.1 | The system shall provide a `SchedulingEngineService` that orchestrates the end-to-end generation pipeline: (1) Load master data, (2) Build constraint set, (3) Initialize variables/domains, (4) Run propagation + search, (5) Post-process results. |
| FR-5.2 | Pre-processing shall load: courses assigned to the semester, faculty assignments, room inventory, batch/section data, academic calendar (holidays, exam windows), active time-slot grid, existing locked sessions. |
| FR-5.3 | The engine shall exclude all dates marked as holidays or within exam windows from the scheduling domain (holiday exclusion). |
| FR-5.4 | The engine shall enforce campus-specific time-slot grid conformance — sessions can only be placed in slots defined in the active grid for the target campus. |
| FR-5.5 | The engine shall support partial re-generation: the coordinator specifies a subset of batches/courses to re-schedule while keeping all other assignments fixed (treated as locked). |
| FR-5.6 | Post-processing shall compute and output: (a) Feasibility score — percentage of hard constraints satisfied (should be 100% for valid solution), (b) Quality score — weighted satisfaction of soft constraints (0-100 scale), (c) Unresolved violations list — any soft constraints that could not be satisfied with details. |

### FR-6: Locked Session Handling

| ID | Requirement |
|----|-------------|
| FR-6.1 | Before generation, the coordinator may designate specific sessions as "locked" (pre-assigned to a particular time-slot and room). |
| FR-6.2 | Locked sessions shall be loaded as variables with a singleton domain (only their current assignment is valid). |
| FR-6.3 | The engine shall propagate constraints from locked sessions first — removing conflicting values from other variables' domains. |
| FR-6.4 | If a locked session creates an infeasibility (e.g., two locked sessions conflict), the engine shall report the conflict immediately without running the full search. |

### FR-7: Generation Trigger API

| ID | Requirement |
|----|-------------|
| FR-7.1 | The system shall expose a REST endpoint to trigger timetable generation: `POST /api/scheduling/generate`. |
| FR-7.2 | Request payload shall include: `campusId`, `departmentId` (optional — if omitted, generate for entire campus), `semesterId`, `lockedSessionIds` (optional list), `excludeBatchIds` (optional — for partial re-gen). |
| FR-7.3 | The endpoint shall validate inputs, enqueue the generation job, and return an immediate `202 Accepted` with a `jobId` for polling. |
| FR-7.4 | The system shall expose `GET /api/scheduling/jobs/{jobId}` to poll generation status (QUEUED, IN_PROGRESS, COMPLETED, FAILED) and retrieve results when complete. |
| FR-7.5 | Only users with role `COORDINATOR` or `HOD` or `ADMIN` for the target department/campus may trigger generation (RBAC enforced). |

### FR-8: Output & Draft Creation

| ID | Requirement |
|----|-------------|
| FR-8.1 | On successful generation (COMPLETE or PARTIAL), the engine shall create a new `TimetableDraft` entity with status `DRAFT`. |
| FR-8.2 | Each assignment in the result shall be stored as a `Session` entity linked to the draft: `draftId`, `courseId`, `facultyId`, `roomId`, `batchId`, `dayOfWeek`, `timeSlotId`, `sessionType` (LECTURE/TUTORIAL/PRACTICAL). |
| FR-8.3 | The draft shall carry metadata: `generatedAt`, `generatedBy` (user who triggered), `feasibilityScore`, `qualityScore`, `engineExecutionMs`, `status`. |
| FR-8.4 | The coordinator can then review the draft, make manual adjustments (via drag-and-drop — separate story AID-186), and submit for approval. |

---

## 5. Hard Constraint Catalogue

| ID | Constraint | Type | Description |
|----|-----------|------|-------------|
| HC-1 | Faculty Single-Assignment | HARD | A faculty member can teach at most one session in any given time-slot. |
| HC-2 | Room Single-Assignment | HARD | A room can host at most one session in any given time-slot. |
| HC-3 | Batch Single-Assignment | HARD | A batch/section can attend at most one session in any given time-slot. |
| HC-4 | Room Capacity | HARD | The assigned room capacity must be >= the batch/section strength. |
| HC-5 | Equipment Match | HARD | The assigned room must have all equipment tags required by the course. |
| HC-6 | Hard Block Exclusion | HARD | No session may be placed in a time-slot where the room has an active hard block. |
| HC-7 | Holiday Exclusion | HARD | No session may be placed on a date marked as a holiday in the academic calendar. |
| HC-8 | Grid Conformance | HARD | Sessions can only be placed in time-slots defined in the campus's active grid. |
| HC-9 | Common Slot Reservation | HARD | Institution-level common slots (CCC/UWE) are pre-reserved; department sessions cannot use them. |
| HC-10 | Travel-Time Buffer | HARD | Cross-campus faculty require a configurable minimum gap (e.g., 30 min) between consecutive sessions at different campuses. |
| HC-11 | Lab Technician Availability | HARD | Practical/lab sessions can only be scheduled within lab technician working hours. |
| HC-12 | Locked Session Immutability | HARD | Pre-assigned locked sessions cannot be moved or reassigned. |

---

## 6. Soft Constraint Catalogue

| ID | Constraint | Type | Weight (Default) | Description |
|----|-----------|------|------------------|-------------|
| SC-1 | Faculty Time Preference | SOFT | 7 | Faculty prefer sessions in their declared preferred time-of-day (morning/afternoon). |
| SC-2 | Consecutive Sessions | SOFT | 5 | Faculty prefer (or avoid) back-to-back sessions based on their preference flag. |
| SC-3 | Room Proximity | SOFT | 4 | Minimize building/floor distance between consecutive sessions for the same batch. |
| SC-4 | Student Gap Minimization | SOFT | 6 | Minimize idle gaps between sessions in a student/batch's daily schedule. |
| SC-5 | Day-Pattern Balance | SOFT | 8 | Distribute sessions evenly across working days (avoid clustering all sessions on 2-3 days). |
| SC-6 | Max Daily Hours | SOFT | 9 | Prefer not to exceed a configurable max daily teaching hours per faculty (soft — the hard max is enforced separately). |
| SC-7 | Spread vs. Compact | SOFT | 3 | Some departments prefer compact schedules (all sessions in morning), others prefer spread. |

---

## 7. Validation Rules

| Input | Rule |
|-------|------|
| `campusId` | Required, must reference an existing active campus |
| `departmentId` | Optional; if provided, must reference an existing department in the campus |
| `semesterId` | Required, must reference an existing active academic calendar |
| `lockedSessionIds` | Optional list; each ID must reference a valid existing session |
| `excludeBatchIds` | Optional list; each must reference a valid batch in the target scope |
| Timeout config | Must be positive integer, 30-600 seconds range |
| Soft constraint weights | Must be integers 1-10 |

---

## 8. Non-Functional Requirements

| Category | Requirement |
|----------|-------------|
| Performance | Generate a timetable for ~40 sections within 2 minutes (120 seconds default timeout). |
| Scalability | Support 500+ faculty, 200+ rooms, 10,000+ students in constraint evaluation. |
| Isolation | Engine executes in a separate worker thread; does not block API requests. |
| Concurrency | Only one generation job per department may run at a time (job queue with deduplication). |
| Resilience | On unexpected failure, job status is set to FAILED with error details; no data corruption. |
| Security | Only authorized users (COORDINATOR, HOD, ADMIN) can trigger generation. |
| Audit | Generation trigger, completion, and all parameter inputs are logged in the audit trail. |
| Parameterized Queries | All database access uses parameterized queries — no SQL string concatenation. |
| Determinism | Given the same inputs and random seed, the engine shall produce the same output (for reproducibility/debugging). |

---

## 9. Acceptance Criteria

| # | Given | When | Then |
|---|-------|------|------|
| AC-1 | Valid master data with ~40 sections, faculty, rooms, calendar, grid | I trigger generation | A complete timetable draft is produced within 120 seconds |
| AC-2 | A session is locked to Monday 9:00 Room A | The engine runs | That session remains in Monday 9:00 Room A in the output |
| AC-3 | Two locked sessions conflict (same faculty, same slot) | I trigger generation | The engine immediately reports infeasibility with the conflicting locked sessions identified |
| AC-4 | The constraint set is infeasible (no valid solution exists) | Timeout fires at 120s | The best partial solution is returned with MUS report explaining which constraints caused infeasibility |
| AC-5 | I request partial re-generation for Department CS only | Generation runs | All non-CS sessions remain exactly as they were; only CS sessions are re-assigned |
| AC-6 | Monday is a holiday in the academic calendar | Engine generates | No sessions are placed on Monday |
| AC-7 | Room R1 has a hard block on Tuesday 10:00-12:00 | Engine generates | No sessions are placed in R1 during that blocked period |
| AC-8 | Faculty X teaches across Campus A and B | Engine generates | A minimum 30-min gap exists between Faculty X's sessions at different campuses |
| AC-9 | Campus grid has slots 9:00-10:00, 10:00-11:00 only | Engine generates | All sessions are placed in those slots only (no off-grid placements) |
| AC-10 | Generation completes successfully | I query the job status | Status is COMPLETED with feasibilityScore=100, qualityScore > 0, and a draft is created |
| AC-11 | I am a Student role user | I try to trigger generation | The API returns 403 Forbidden |
| AC-12 | A generation job is already running for CS department | I trigger another for CS | The API returns 409 Conflict (one job at a time per department) |

---

## 10. Data Model (Conceptual)

```
SchedulingJob
├── id (UUID)
├── campus_id (FK)
├── department_id (FK, nullable)
├── semester_id (FK)
├── triggered_by (user_id)
├── status (QUEUED | IN_PROGRESS | COMPLETED | FAILED)
├── started_at (timestamp)
├── completed_at (timestamp)
├── execution_time_ms (long)
├── feasibility_score (decimal)
├── quality_score (decimal)
├── error_message (text, nullable)
└── result_draft_id (FK → TimetableDraft, nullable)

TimetableDraft
├── id (UUID)
├── campus_id (FK)
├── department_id (FK, nullable)
├── semester_id (FK)
├── version (integer, increments on re-generation)
├── status (DRAFT | UNDER_REVIEW | APPROVED | PUBLISHED)
├── generated_at (timestamp)
├── generated_by (user_id)
├── feasibility_score (decimal)
├── quality_score (decimal)
└── audit columns

Session (child of TimetableDraft)
├── id (UUID)
├── draft_id (FK)
├── course_id (FK)
├── faculty_id (FK)
├── room_id (FK)
├── batch_id (FK)
├── day_of_week (1-7)
├── time_slot_id (FK)
├── session_type (LECTURE | TUTORIAL | PRACTICAL)
├── is_locked (boolean)
└── audit columns

ConstraintConfig
├── id (UUID)
├── campus_id (FK, nullable — null = institution-wide)
├── department_id (FK, nullable)
├── constraint_type (enum)
├── category (HARD | SOFT)
├── weight (integer 1-10, for SOFT only)
├── parameters (JSONB — constraint-specific config)
├── is_active (boolean)
└── audit columns
```

---

## 11. Dependencies

| Dependency | Description |
|------------|-------------|
| Campus Hierarchy (AID-179) | Campus, department, batch, section data for scope filtering |
| Course Management (AID-180) | Course data with L-T-P, equipment tags for constraint building |
| Faculty Management (AID-181) | Faculty availability, competencies, campus associations for constraints |
| Room & Resource (AID-182) | Room capacity, equipment, blocks for domain building |
| Academic Calendar (AID-183) | Holidays, exam windows, grid slots for temporal constraints |
| Authentication & RBAC (AID-197) | Authorization for generation trigger |
| Audit Trail (AID-200) | Logging generation events |

---

## 12. Assumptions

- Faculty-to-course assignments for the semester are finalized before generation is triggered.
- The academic calendar and time-slot grid for the target campus are active and configured before generation.
- Room inventory and resource blocks are up-to-date at the time of generation.
- The engine operates on a weekly recurring pattern (generates one canonical week; the pattern repeats across the semester).
- One generation job per department at a time; concurrent generation for different departments on the same campus is allowed.

---

## 13. Open Questions

- Should the engine support fortnightly/alternate-week patterns in Phase 1, or only weekly recurring?
- What is the maximum number of concurrent generation jobs the system should support institution-wide?
- Should the engine auto-assign faculty to courses if assignments are missing, or require pre-assignment?
- Should partial re-generation preserve soft-constraint optimizations from the original run, or re-optimize from scratch?
- What is the preferred random seed strategy: fixed seed for reproducibility or random seed with logging?

---

## 14. Traceability

| BRD Section | BRD Point Name | Requirement IDs |
|-------------|----------------|-----------------|
| 6.2 — Class/Lecture Timetable Generation | Auto-Generation (Req 4.1) | FR-5.1, FR-5.2 |
| 6.2 — Class/Lecture Timetable Generation | Configurable Time-Slot Grids (Req 4.2) | FR-5.4 |
| 6.2 — Class/Lecture Timetable Generation | Recurring Patterns (Req 4.3) | Assumption (weekly recurring) |
| 6.2 — Class/Lecture Timetable Generation | Locked Sessions (Req 4.4) | FR-3.6, FR-6 |
| 6.2 — Class/Lecture Timetable Generation | Draft Review (Req 4.5) | FR-5.6, FR-8 |
| 6.10 — Scheduling Engine | Constraint-Based Algorithm (Req 12.1) | FR-1, FR-2, FR-3 |
| 6.10 — Scheduling Engine | Soft-Constraint Optimization (Req 12.2) | FR-1.2, FR-1.7, SC-1 to SC-7 |
| 6.10 — Scheduling Engine | Re-Run Subset (Req 12.3) | FR-5.5 |
| 6.10 — Scheduling Engine | Partial Re-Generation (Req 12.4) | FR-5.5 |
| 6.10 — Scheduling Engine | Quality Score (Req 12.5) | FR-5.6 |
| 6.5 — Faculty Workload | Time-Preferences (Req 7.1) | SC-1, SC-2 |
| 6.5 — Faculty Workload | Workload Validation (Req 7.2) | SC-6 |
| 6.12 — Multi-Campus | Travel Buffer (Req 14.4) | HC-10 |
| 7.7 — Institution-Specific | Day-Pattern Saturation Balancing | SC-5 |
| 7.7 — Institution-Specific | Common Slots (CCC/UWE) | HC-9 |
| 7.7 — Institution-Specific | Support-Staff Availability | HC-11 |
| 8 — Non-Functional | Performance (NFR) | NFR (Performance) |
| 8 — Non-Functional | Scalability (NFR) | NFR (Scalability) |
