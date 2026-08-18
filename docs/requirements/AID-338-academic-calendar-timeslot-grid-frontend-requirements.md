# Requirements Document — Frontend: Academic Calendar & Time-Slot Grid Admin

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-338 |
| Story URL | https://akhiljawali.atlassian.net/browse/AID-338 |
| Subtask Key | AID-339 (Requirement Generation) |
| Backend Dependency | AID-183 (Academic Calendar & Time-Slot Grid APIs) |
| Priority | High |
| Story Points | 5 |
| Status | Draft |
| Author | Akhil Jawali |
| Date | 16 August 2026 |

---

## 1. Introduction

This document captures the requirements for a React-based admin panel that provides a visual interface for managing academic calendars (semesters, holidays, exam windows, special periods) and campus-specific time-slot grids (slot definitions, working days). This frontend consumes the backend REST APIs from AID-183 and provides System Administrators with CRUD interfaces, visual timeline displays, and impact detection feedback.

---

## 2. User Story

> As a System Administrator, I want a web-based admin panel to manage academic calendars (semester dates, holidays, exam windows, special periods) and campus-specific time-slot grids so that I can configure scheduling boundaries visually.

---

## 3. Actors

| Actor | Interaction |
|-------|-------------|
| System Administrator | Full CRUD on calendars, holidays, exam windows, special periods, grids, slots, working days |
| IT Admin | Full CRUD on all calendar and grid entities |
| Registrar | Create/update academic calendars; view grids |
| HOD | Read-only access to calendars for their campus |

---

## 4. Functional Requirements

### FR-1: Academic Calendar List View

| ID | Requirement |
|----|-------------|
| FR-1.1 | The system shall display a paginated list of academic calendars with columns: Name, Campus, Academic Year, Semester Type, Start Date, End Date, Status. |
| FR-1.2 | The list shall support filtering by campus (dropdown), academic year, and semester type. |
| FR-1.3 | The list shall support sorting by name, start date, or academic year. |
| FR-1.4 | Each row shall have action buttons: View (drill into sub-resources), Edit, Delete. |
| FR-1.5 | A "Create Calendar" button shall open a creation form. |

### FR-2: Academic Calendar Create/Edit Form

| ID | Requirement |
|----|-------------|
| FR-2.1 | The form shall include fields: Name, Campus (dropdown), Academic Year (format YYYY-YYYY), Semester Type (select: ODD/EVEN/SUMMER), Start Date (date picker), End Date (date picker). |
| FR-2.2 | Start Date must be before End Date — inline validation on change. |
| FR-2.3 | On submit, if the API returns 409 (overlapping calendar), display the conflict error inline. |
| FR-2.4 | On success, navigate to the calendar detail view (showing sub-resources). |

### FR-3: Calendar Detail View (Sub-Resources)

| ID | Requirement |
|----|-------------|
| FR-3.1 | When viewing a calendar, display three tabbed sections: Holidays, Exam Windows, Special Periods. |
| FR-3.2 | Each tab shows a list of its sub-resources with CRUD actions (Add, Edit, Delete). |
| FR-3.3 | A visual timeline bar at the top shows the calendar date range with holidays (red dots), exam windows (orange bands), and special periods (blue bands) plotted on it. |

### FR-4: Holiday Management

| ID | Requirement |
|----|-------------|
| FR-4.1 | Holiday form fields: Name, Date (date picker constrained to calendar range), Day Type (FULL_DAY/HALF_DAY_AM/HALF_DAY_PM), Is Recurring (checkbox). |
| FR-4.2 | If the date is outside the calendar range, the date picker shall disable those dates. |
| FR-4.3 | On holiday creation, if the API returns impacted sessions, display a warning banner listing them: "X sessions are scheduled on this date and will need rescheduling." |
| FR-4.4 | The impacted sessions list shall show: Course, Faculty, Room, Batch, Time Slot. |

### FR-5: Exam Window Management

| ID | Requirement |
|----|-------------|
| FR-5.1 | Exam window form fields: Name, Exam Type (MID_SEM/END_SEM/SUPPLEMENTARY), Start Date, End Date. |
| FR-5.2 | Start/End dates constrained to parent calendar range via date picker. |
| FR-5.3 | Display exam windows on the visual timeline as colored bands. |

### FR-6: Special Period Management

| ID | Requirement |
|----|-------------|
| FR-6.1 | Special period form fields: Name, Period Type (ORIENTATION/REGISTRATION/BREAK/REVISION), Start Date, End Date. |
| FR-6.2 | Start/End dates constrained to parent calendar range. |
| FR-6.3 | Display special periods on the visual timeline as colored bands (distinct from exam windows). |

### FR-7: Time-Slot Grid List View

| ID | Requirement |
|----|-------------|
| FR-7.1 | Display a paginated list of time-slot grids with columns: Name, Campus, Effective From, Status (Active/Inactive), Slot Count. |
| FR-7.2 | Filter by campus. |
| FR-7.3 | Active grid highlighted with a badge/indicator. |
| FR-7.4 | Action buttons: View, Edit, Delete, Activate (only on inactive grids). |

### FR-8: Time-Slot Grid Detail View

| ID | Requirement |
|----|-------------|
| FR-8.1 | When viewing a grid, display: grid metadata at top, visual slot timeline below, working days toggle at bottom. |
| FR-8.2 | The visual slot timeline shows slots as colored blocks on a horizontal timeline (08:00 → 18:00). |
| FR-8.3 | Slot colors by type: LECTURE (blue), TUTORIAL (green), PRACTICAL (orange), BREAK (gray), LUNCH (yellow). |
| FR-8.4 | Each slot block shows: slot number, start-end time, duration, type label. |

### FR-9: Slot Definition Management

| ID | Requirement |
|----|-------------|
| FR-9.1 | Add slot form: Slot Number, Start Time (time picker), End Time (time picker), Slot Type (select). |
| FR-9.2 | Duration auto-calculated and displayed as the user selects times. |
| FR-9.3 | If the API returns 409 (time overlap), display inline error. |
| FR-9.4 | Support bulk slot creation: a "Quick Setup" button that lets the user define a pattern (e.g., 8 slots of 60 min starting at 08:00 with a 15-min break after slot 3 and 45-min lunch after slot 5). |

### FR-10: Working Days Configuration

| ID | Requirement |
|----|-------------|
| FR-10.1 | Display 7 day toggles (Monday through Sunday) with ON/OFF state. |
| FR-10.2 | At least 1 day must be ON — disable the last remaining ON toggle. |
| FR-10.3 | Changes save on toggle click (auto-save via PUT). |

### FR-11: Grid Activation

| ID | Requirement |
|----|-------------|
| FR-11.1 | An "Activate" button is visible on inactive grids that have at least one slot and at least one working day. |
| FR-11.2 | On click, show confirmation: "Activating this grid will deactivate the current active grid for this campus. Continue?" |
| FR-11.3 | On success, the grid status badge updates to "Active" and the previous active grid updates to "Inactive". |

### FR-12: Navigation & Layout

| ID | Requirement |
|----|-------------|
| FR-12.1 | Sidebar navigation shall have "Academic Calendar" and "Time-Slot Grids" menu items under "Scheduling Config". |
| FR-12.2 | Breadcrumb navigation shall show the path (e.g., Home > Academic Calendars > Odd Semester 2026 > Holidays). |
| FR-12.3 | Layout is responsive (desktop-first). |

### FR-13: Loading & Error States

| ID | Requirement |
|----|-------------|
| FR-13.1 | All data-fetching shows loading skeleton. |
| FR-13.2 | Network errors show error state with retry. |
| FR-13.3 | 401 redirects to login. |

---

## 5. Technical Requirements

| Category | Requirement |
|----------|-------------|
| Framework | React 18+ with TypeScript |
| Routing | React Router v6+ (nested under /admin/scheduling-config/) |
| State Management | TanStack Query (server state), Zustand (client state) |
| Validation | Zod (runtime schema validation on forms) |
| Date Handling | date-fns or dayjs for date formatting and range calculations |
| UI Components | Shadcn/ui + custom timeline visualization |
| API Client | Shared Axios client from existing frontend setup |
| XSS Prevention | DOMPurify, no dangerouslySetInnerHTML |

---

## 6. API Endpoints Consumed

| Endpoint | Method | Usage |
|----------|--------|-------|
| `/api/v1/academic-calendars` | GET/POST | List/Create calendars |
| `/api/v1/academic-calendars/{id}` | GET/PUT/DELETE | Calendar CRUD |
| `/api/v1/academic-calendars/{id}/holidays` | GET/POST | Holiday management |
| `/api/v1/academic-calendars/{id}/holidays/{hid}` | PUT/DELETE | Holiday edit/delete |
| `/api/v1/academic-calendars/{id}/exam-windows` | GET/POST | Exam window management |
| `/api/v1/academic-calendars/{id}/exam-windows/{eid}` | PUT/DELETE | Exam window edit/delete |
| `/api/v1/academic-calendars/{id}/special-periods` | GET/POST | Special period management |
| `/api/v1/academic-calendars/{id}/special-periods/{sid}` | PUT/DELETE | Special period edit/delete |
| `/api/v1/academic-calendars/{id}/impact-analysis` | GET | Impact detection |
| `/api/v1/time-slot-grids` | GET/POST | List/Create grids |
| `/api/v1/time-slot-grids/{id}` | GET/PUT/DELETE | Grid CRUD |
| `/api/v1/time-slot-grids/{id}/activate` | PUT | Activate grid |
| `/api/v1/time-slot-grids/{id}/slots` | GET/POST | Slot definitions |
| `/api/v1/time-slot-grids/{id}/slots/bulk` | POST | Bulk slot creation |
| `/api/v1/time-slot-grids/{id}/slots/{sid}` | PUT/DELETE | Slot edit/delete |
| `/api/v1/time-slot-grids/{id}/working-days` | GET/PUT | Working day config |
| `/api/v1/campuses/{id}/active-grid` | GET | Get active grid for campus |

---

## 7. Acceptance Criteria

| # | Given | When | Then |
|---|-------|------|------|
| AC-1 | I am an Admin | I navigate to Academic Calendars | I see a paginated list filtered by campus and academic year |
| AC-2 | I create a calendar with overlapping dates | I submit | Inline error: "Overlapping calendar exists" |
| AC-3 | I view a calendar | I click on it | I see tabbed view with Holidays, Exam Windows, Special Periods + visual timeline |
| AC-4 | I add a holiday to an active calendar | Sessions exist on that date | Warning banner shows impacted sessions |
| AC-5 | I configure a time-slot grid | I define slots | Visual timeline shows colored blocks for each slot |
| AC-6 | I add overlapping slots | I submit | Inline error: "Slot times overlap" |
| AC-7 | I activate a new grid | Confirmation accepted | New grid becomes active, old one deactivates |
| AC-8 | I toggle working days | I turn off Saturday | Saturday toggle shows OFF, auto-saves |
| AC-9 | I use Quick Setup | I set 8 slots of 60min from 08:00 with break/lunch | All slots created in bulk, timeline updates |

---

## 8. Wireframe (Conceptual)

### Calendar Detail with Timeline
```
┌──────────────────────────────────────────────────────┐
│ Odd Semester 2026 (Aug 1 - Dec 15)                   │
├──────────────────────────────────────────────────────┤
│ Timeline: |█Aug══════█Sep══════█Oct══════█Nov══█Dec|  │
│           🔴     🔴🔴       🟠🟠🟠        🟠🟠🟠🟠   │
│           Diwali  Dussehra  Mid-Sem     End-Sem      │
├──────────────────────────────────────────────────────┤
│ [Holidays] [Exam Windows] [Special Periods]          │
│                                                      │
│ Holidays (12):                                       │
│ ┌─────────┬──────────┬───────────┬──────────┬─────┐ │
│ │ Name    │ Date     │ Day Type  │ Recurring│ Act │ │
│ │ Diwali  │ Oct 20   │ Full Day  │ Yes      │ ⋮   │ │
│ │ Holi    │ Nov 5    │ Full Day  │ Yes      │ ⋮   │ │
│ └─────────┴──────────┴───────────┴──────────┴─────┘ │
│ [+ Add Holiday]                                      │
└──────────────────────────────────────────────────────┘
```

### Time-Slot Grid Visual
```
┌──────────────────────────────────────────────────────┐
│ Main Campus Standard Grid  [Active ✓]  [Activate]    │
├──────────────────────────────────────────────────────┤
│ 08:00  09:00  10:00 10:15  11:15  12:15 13:00 14:00 17:00 │
│ ┌─────┐┌─────┐┌──┐┌─────┐┌─────┐┌───┐┌────┐┌─────────┐ │
│ │ L1  ││ L2  ││Bk││ L3  ││ L4  ││Lch││ T1 ││   P1    │ │
│ │ 60m ││ 60m ││15││ 60m ││ 60m ││45m││ 60m││  180m   │ │
│ └─────┘└─────┘└──┘└─────┘└─────┘└───┘└────┘└─────────┘ │
├──────────────────────────────────────────────────────┤
│ Working Days: [Mon ✓] [Tue ✓] [Wed ✓] [Thu ✓]       │
│               [Fri ✓] [Sat ✓] [Sun ✗]               │
└──────────────────────────────────────────────────────┘
```

---

## 9. Dependencies

| Dependency | Description |
|------------|-------------|
| Backend API (AID-183) | All calendar and grid REST endpoints must be available |
| Frontend Project (AID-325) | React app already initialized with shared components |
| Campus data (AID-179) | Campus dropdown populated from existing campus API |

---

## 10. Open Questions

- Should the visual timeline be interactive (click on a date to add a holiday) or just informational?
- Should the Quick Setup wizard support templates (e.g., "Standard 8-period day", "Lab day")?
- Should working day changes trigger a warning if sessions exist on the day being removed?

---

## 11. Traceability

| BRD Section | Requirement IDs |
|-------------|-----------------|
| 6.1 — Master Data Management (Academic Calendar) | FR-1 through FR-6 |
| 6.1 — Master Data Management (Time-Slot Grid) | FR-7 through FR-11 |
| 23 — UI/UX Requirements | FR-12, FR-13 |
| 8 — Security | Technical Requirements (XSS, CSP) |
