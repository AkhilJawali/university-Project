# Requirements Document — Frontend: Master Data Admin Panel (Campus Hierarchy)

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-325 |
| Story URL | https://akhiljawali.atlassian.net/browse/AID-325 |
| Subtask Key | AID-326 (Requirement Generation) |
| BRD Reference | Section 6.1 — Master Data Management |
| Priority | High |
| Story Points | 5 |
| Status | Draft |
| Author | Akhil Jawali |
| Date | 14 August 2026 |

---

## 1. Introduction

This document captures the requirements for a React-based admin panel that provides a visual interface for managing the campus hierarchy (Campuses → Departments → Programs → Batches → Sections). This frontend consumes the existing backend REST APIs (AID-179) and provides System Administrators with a complete CRUD interface, hierarchy visualization, search, pagination, and inline validation.

---

## 2. User Story

> As a System Administrator, I want a web-based admin panel to manage the campus hierarchy (campuses, departments, programs, batches, sections) so that I can perform CRUD operations visually without needing direct API access.

---

## 3. Actors

| Actor | Interaction |
|-------|-------------|
| System Administrator | Full CRUD on all hierarchy entities via UI |
| IT Admin | Full CRUD on all hierarchy entities via UI |
| Registrar | View hierarchy; create/update campuses |
| HOD | View own department and below (read-only) |

---

## 4. Functional Requirements

### FR-1: Campus List View

| ID | Requirement |
|----|-------------|
| FR-1.1 | The system shall display a paginated list of campuses with columns: Name, Code, City, State, Timezone, Status (Active/Inactive). |
| FR-1.2 | The list shall support search by name or code (debounced, 300ms). |
| FR-1.3 | The list shall support sorting by name, code, or city. |
| FR-1.4 | Each row shall have action buttons: View, Edit, Delete. |
| FR-1.5 | A "Create Campus" button shall open a creation form. |
| FR-1.6 | Pagination controls shall show page number, page size selector (10/20/50), and total count. |

### FR-2: Campus Create/Edit Form

| ID | Requirement |
|----|-------------|
| FR-2.1 | The form shall include fields: Name, Code, Address, City, State, Timezone. |
| FR-2.2 | All fields shall validate inline using Zod schemas matching backend validation rules (code: uppercase alphanumeric + hyphen, 2-20 chars; name: 1-200 chars). |
| FR-2.3 | On submit, the form shall call POST /api/v1/campuses (create) or PUT /api/v1/campuses/{id} (update). |
| FR-2.4 | On success, navigate back to the list with a success toast notification. |
| FR-2.5 | On error (400/409), display field-level error messages from the API response inline on the form. |
| FR-2.6 | The Code field shall auto-uppercase input as the user types. |

### FR-3: Department Management (Nested Under Campus)

| ID | Requirement |
|----|-------------|
| FR-3.1 | When viewing a campus, the user shall see a list of departments belonging to that campus. |
| FR-3.2 | The department list shall support CRUD: Create, Edit, Delete with the same form pattern as campus. |
| FR-3.3 | Department form fields: Name, Code, HOD Faculty (optional dropdown). |
| FR-3.4 | Department code uniqueness errors (409) shall display inline on the Code field. |

### FR-4: Program, Batch, Section Management (Nested)

| ID | Requirement |
|----|-------------|
| FR-4.1 | Programs are managed within a department view. CRUD form: Name, Code, Duration Years, Total Semesters, Degree Type (dropdown: UG/PG/PhD/Diploma). |
| FR-4.2 | Batches are managed within a program view. CRUD form: Name, Academic Year, Semester Number, Strength. |
| FR-4.3 | Sections are managed within a batch view. CRUD form: Name, Strength. |
| FR-4.4 | If section strength exceeds batch strength, display the warning from the API response (non-blocking). |

### FR-5: Hierarchy Tree View

| ID | Requirement |
|----|-------------|
| FR-5.1 | The system shall provide a visual tree view showing the full campus hierarchy (Campus → Departments → Programs → Batches → Sections). |
| FR-5.2 | The tree shall be collapsible/expandable at each level. |
| FR-5.3 | The tree shall consume the GET /api/v1/campuses/{id}/hierarchy endpoint. |
| FR-5.4 | Clicking any node in the tree shall navigate to that entity's detail/edit view. |

### FR-6: Delete Confirmation & Conflict Handling

| ID | Requirement |
|----|-------------|
| FR-6.1 | Delete actions shall show a confirmation dialog before proceeding. |
| FR-6.2 | If the API returns 409 (has active children), display the conflict message in an error dialog listing the dependent entities. |
| FR-6.3 | The user cannot dismiss the conflict error without acknowledging it. |

### FR-7: Navigation & Layout

| ID | Requirement |
|----|-------------|
| FR-7.1 | The admin panel shall have a sidebar navigation with "Master Data" → "Campus Hierarchy" menu item. |
| FR-7.2 | Breadcrumb navigation shall show the current hierarchy path (e.g., Home > Campuses > Main Campus > Computer Science). |
| FR-7.3 | The layout shall be responsive (desktop-first, but usable on tablet). |

### FR-8: Loading & Error States

| ID | Requirement |
|----|-------------|
| FR-8.1 | All data-fetching operations shall show a loading skeleton/spinner. |
| FR-8.2 | Network errors shall display a user-friendly error message with a retry button. |
| FR-8.3 | 401 responses shall redirect to the login page (when auth is implemented). |

---

## 5. Technical Requirements

| Category | Requirement |
|----------|-------------|
| Framework | React 18+ with TypeScript |
| Routing | React Router v6+ |
| State Management | Zustand (client state), TanStack Query (server state) |
| Validation | Zod (runtime schema validation on forms) |
| UI Components | Component library (Shadcn/ui or similar) |
| API Client | Axios or fetch with typed API layer |
| Build Tool | pnpm |
| XSS Prevention | DOMPurify for any user-generated content display; no dangerouslySetInnerHTML |
| CSP | Content-Security-Policy headers configured |

---

## 6. API Endpoints Consumed

| Endpoint | Method | Usage |
|----------|--------|-------|
| `/api/v1/campuses` | GET | List campuses (paginated, filtered) |
| `/api/v1/campuses` | POST | Create campus |
| `/api/v1/campuses/{id}` | GET | Get single campus |
| `/api/v1/campuses/{id}` | PUT | Update campus |
| `/api/v1/campuses/{id}` | DELETE | Soft-delete campus |
| `/api/v1/campuses/{id}/hierarchy` | GET | Full hierarchy tree |
| `/api/v1/departments` | GET/POST | List/Create departments (filter by campusId) |
| `/api/v1/departments/{id}` | GET/PUT/DELETE | Department CRUD |
| `/api/v1/programs` | GET/POST | List/Create programs (filter by departmentId) |
| `/api/v1/programs/{id}` | GET/PUT/DELETE | Program CRUD |
| `/api/v1/batches` | GET/POST | List/Create batches (filter by programId) |
| `/api/v1/batches/{id}` | GET/PUT/DELETE | Batch CRUD |
| `/api/v1/sections` | GET/POST | List/Create sections (filter by batchId) |
| `/api/v1/sections/{id}` | GET/PUT/DELETE | Section CRUD |

---

## 7. Acceptance Criteria

| # | Given | When | Then |
|---|-------|------|------|
| AC-1 | I am logged in as Admin | I navigate to Master Data > Campus Hierarchy | I see a paginated list of campuses |
| AC-2 | I click "Add Campus" | I fill in valid data and submit | Campus is created, success toast shown, list refreshes |
| AC-3 | I submit a campus with duplicate code | Form is submitted | Field-level error shown inline on Code field |
| AC-4 | I try to delete a campus with active departments | I click Delete and confirm | Error dialog shows "Cannot delete: X active departments" |
| AC-5 | I click on a campus | I see its departments | I can CRUD departments, then drill into programs/batches/sections |
| AC-6 | I view the hierarchy tree | I expand nodes | Full tree is visible: Campus → Dept → Program → Batch → Section |
| AC-7 | I create a section that exceeds batch strength | Form submits | Section is created but a warning banner is displayed |
| AC-8 | API is unreachable | I try to load the list | Friendly error message with retry button |

---

## 8. Wireframe (Conceptual)

```
┌──────────────────────────────────────────────────────┐
│ Sidebar    │  Breadcrumb: Home > Campuses            │
│            │                                          │
│ Master Data│  [Search: ___________] [+ Add Campus]   │
│  > Campus  │                                          │
│  Hierarchy │  ┌──────┬──────┬──────┬────────┬─────┐ │
│            │  │ Name │ Code │ City │ Status │ Act │ │
│            │  ├──────┼──────┼──────┼────────┼─────┤ │
│            │  │ Main │ MAIN │ Bgm  │ Active │ ⋮   │ │
│            │  │ North│ NRTH │ Hbl  │ Active │ ⋮   │ │
│            │  └──────┴──────┴──────┴────────┴─────┘ │
│            │  [< 1 2 3 >]  Showing 1-20 of 45       │
└──────────────────────────────────────────────────────┘
```

---

## 9. Dependencies

| Dependency | Description |
|------------|-------------|
| Backend API (AID-179) | All REST endpoints must be available and working |
| Authentication | JWT auth flow (when implemented); for now, APIs are open |
| React Project Setup | React app must be initialized (pnpm, routing, state management) |

---

## 10. Open Questions

- Should the tree view be on a separate page or a collapsible sidebar within the list view?
- Should bulk operations (e.g., bulk delete, bulk import from CSV) be supported in Phase 1?
- Should the admin panel support dark mode?

---

## 11. Traceability

| BRD Section | Requirement IDs |
|-------------|-----------------|
| 6.1 — Master Data Management | FR-1 through FR-8 |
| 23 — UI/UX Requirements | FR-7 (navigation), FR-8 (loading states) |
| 8 — Security | Technical Requirements (XSS, CSP) |
