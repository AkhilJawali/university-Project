# Design: Frontend — Academic Calendar & Time-Slot Grid Admin

**Jira Reference:** AID-338
**Source Requirements:** docs/requirements/AID-338-academic-calendar-timeslot-grid-frontend-requirements.md
**Application:** React frontend (part of existing UTMS project)
**Stack:** React 18 · TypeScript · React Router v6 · TanStack Query · Zustand · Zod · Shadcn/ui · date-fns
**Generated:** 16 August 2026

---

## 1. Overview

This module delivers a web-based admin panel for managing academic calendars and campus-specific time-slot grids. It provides System Administrators with:

- **Academic Calendar Management:** CRUD for calendars, holidays, exam windows, and special periods with a visual timeline showing the semester date range and plotted events.
- **Time-Slot Grid Management:** CRUD for grids and slot definitions with a colored block timeline visualization, working day toggles with auto-save, bulk slot creation via a Quick Setup wizard, and grid activation with confirmation.
- **Impact Detection:** When adding holidays to an active calendar, a warning banner shows impacted sessions that will need rescheduling.

This module extends the patterns established in AID-325 (Master Data Admin Panel) — reusing the shared API client, query hooks, form validation, and error handling patterns.

---

## 2. Architecture

### 2.1 Component Hierarchy

```
App
├── AuthProvider (JWT context)
├── QueryClientProvider (TanStack Query)
├── RouterProvider (React Router v6)
│   └── AdminLayout
│       ├── Sidebar (navigation — "Scheduling Config" section)
│       ├── Breadcrumbs
│       └── <Outlet /> (page content)
│           ├── CalendarListPage
│           ├── CalendarFormPage (create/edit)
│           ├── CalendarDetailPage (tabs + timeline)
│           │   ├── CalendarTimeline
│           │   ├── HolidayTab → HolidayForm, ImpactWarningBanner
│           │   ├── ExamWindowTab → ExamWindowForm
│           │   └── SpecialPeriodTab → SpecialPeriodForm
│           ├── GridListPage
│           ├── GridFormPage (create/edit)
│           └── GridDetailPage
│               ├── SlotTimeline
│               ├── SlotForm
│               ├── QuickSetupWizard
│               ├── WorkingDayToggles
│               └── GridActivationDialog
└── Toaster (global notifications)
```

### 2.2 State Management Approach

| Concern | Tool | Justification |
|---------|------|---------------|
| Server state (calendars, grids, slots) | TanStack Query v5 | Caching, refetch, optimistic updates, pagination |
| Client-only UI state | Zustand | Sidebar toggle, toast queue, breadcrumb path, active tab |
| Form state | React Hook Form + Zod | Performant form handling with schema validation |
| Date calculations | date-fns | Lightweight, tree-shakeable, immutable date operations |

### 2.3 API Layer Design

Extends the existing typed Axios client from AID-325:
- `calendarApi` — calendar CRUD + sub-resource endpoints
- `gridApi` — grid CRUD, slot management, working days, activation

Each API module exports strongly-typed functions matching the backend endpoints from AID-183.

### 2.4 Routing Structure

All routes are nested under `/admin/scheduling-config`:

| Route | Page | Description |
|-------|------|-------------|
| `/admin/scheduling-config/calendars` | CalendarListPage | Paginated calendar list |
| `/admin/scheduling-config/calendars/new` | CalendarFormPage | Create calendar |
| `/admin/scheduling-config/calendars/:calendarId` | CalendarDetailPage | Detail view with tabs + timeline |
| `/admin/scheduling-config/calendars/:calendarId/edit` | CalendarFormPage | Edit calendar metadata |
| `/admin/scheduling-config/calendars/:calendarId/holidays/new` | HolidayForm (modal/page) | Add holiday |
| `/admin/scheduling-config/calendars/:calendarId/exam-windows/new` | ExamWindowForm (modal/page) | Add exam window |
| `/admin/scheduling-config/calendars/:calendarId/special-periods/new` | SpecialPeriodForm (modal/page) | Add special period |
| `/admin/scheduling-config/grids` | GridListPage | Paginated grid list |
| `/admin/scheduling-config/grids/new` | GridFormPage | Create grid |
| `/admin/scheduling-config/grids/:gridId` | GridDetailPage | Detail with slot timeline + working days |
| `/admin/scheduling-config/grids/:gridId/edit` | GridFormPage | Edit grid metadata |
| `/admin/scheduling-config/grids/:gridId/slots/new` | SlotForm (inline/modal) | Add slot definition |

---

## 3. Page & Component Design

### 3.1 CalendarListPage

- **Route:** `/admin/scheduling-config/calendars`
- **Components:** `DataTable`, `FilterBar` (campus dropdown, academic year, semester type), `PaginationControls`, `ActionMenu`, `DeleteConfirmDialog`
- **Data Fetching:** `useCalendars(params)` — TanStack Query with pagination/filter/sort params
- **User Interactions:**
  - Filter by campus (dropdown from `useCampuses`), academic year (text input), semester type (select)
  - Column header click toggles sort (name, startDate, academicYear)
  - "Create Calendar" button navigates to `/calendars/new`
  - Row actions: View (→ detail), Edit (→ edit form), Delete (confirmation dialog)

### 3.2 CalendarFormPage

- **Route:** `/admin/scheduling-config/calendars/new` or `/:calendarId/edit`
- **Components:** `FormCard`, `TextField`, `SelectField`, `DatePickerField`, `SubmitButton`, `FormErrorBanner`
- **Data Fetching:**
  - Edit mode: `useCalendar(calendarId)` to prefill
  - Campus dropdown: `useCampuses()` for selection
- **Form Fields:** Name, Campus (dropdown), Academic Year (pattern YYYY-YYYY), Semester Type (ODD/EVEN/SUMMER), Start Date, End Date
- **Validation:**
  - Zod schema enforces start < end date
  - On 409 (overlap): inline conflict error message
- **Success:** Navigate to calendar detail view

### 3.3 CalendarDetailPage

- **Route:** `/admin/scheduling-config/calendars/:calendarId`
- **Components:** `CalendarTimeline`, `Tabs` (Holidays, Exam Windows, Special Periods), sub-resource lists
- **Data Fetching:** `useCalendar(calendarId)`, `useHolidays(calendarId)`, `useExamWindows(calendarId)`, `useSpecialPeriods(calendarId)`
- **Layout:**
  1. Header: Calendar name, campus, academic year, date range, status badge
  2. CalendarTimeline: Visual bar showing the full date range with plotted events
  3. Tabbed content: Each tab shows its sub-resource list + Add button

### 3.4 CalendarTimeline (Visual Component)

- Renders a horizontal bar representing the calendar's date range (start → end)
- Plots:
  - **Holidays** as red dots/markers at their date position
  - **Exam Windows** as orange bands spanning their date ranges
  - **Special Periods** as blue bands spanning their date ranges
- Hovering a marker/band shows a tooltip with the event name and dates
- Click navigates to the corresponding tab

### 3.5 HolidayTab + HolidayForm + ImpactWarningBanner

- **HolidayTab:** DataTable listing holidays with columns: Name, Date, Day Type, Recurring, Actions
- **HolidayForm:** Dialog/inline form with fields: Name, Date (date picker constrained to calendar range), Day Type (FULL_DAY/HALF_DAY_AM/HALF_DAY_PM), Is Recurring (checkbox)
- **ImpactWarningBanner:** After holiday creation, if the API returns impacted sessions, display a dismissible warning: "X sessions are scheduled on this date and will need rescheduling" with expandable list showing Course, Faculty, Room, Batch, Time Slot

### 3.6 ExamWindowTab + ExamWindowForm

- **ExamWindowTab:** DataTable with columns: Name, Exam Type, Start Date, End Date, Actions
- **ExamWindowForm:** Dialog with fields: Name, Exam Type (MID_SEM/END_SEM/SUPPLEMENTARY), Start Date, End Date (both constrained to calendar range)

### 3.7 SpecialPeriodTab + SpecialPeriodForm

- **SpecialPeriodTab:** DataTable with columns: Name, Period Type, Start Date, End Date, Actions
- **SpecialPeriodForm:** Dialog with fields: Name, Period Type (ORIENTATION/REGISTRATION/BREAK/REVISION), Start Date, End Date (constrained to calendar range)

### 3.8 GridListPage

- **Route:** `/admin/scheduling-config/grids`
- **Components:** `DataTable`, `FilterBar` (campus dropdown), `PaginationControls`, `ActionMenu`, `DeleteConfirmDialog`
- **Data Fetching:** `useGrids(params)`
- **Columns:** Name, Campus, Effective From, Status (badge: Active green / Inactive gray), Slot Count
- **Actions:** View, Edit, Delete, Activate (only on inactive grids with slots + working days)

### 3.9 GridFormPage

- **Route:** `/admin/scheduling-config/grids/new` or `/:gridId/edit`
- **Form Fields:** Name, Campus (dropdown), Effective From (date picker)
- **Success:** Navigate to grid detail view

### 3.10 GridDetailPage

- **Route:** `/admin/scheduling-config/grids/:gridId`
- **Layout:**
  1. Header: Grid name, campus, effective from, status badge, Activate button (conditional)
  2. SlotTimeline: Visual colored blocks on a horizontal timeline (08:00 → 18:00)
  3. Slot list: Table with Add Slot + Quick Setup buttons
  4. WorkingDayToggles: 7 day toggles with auto-save

### 3.11 SlotTimeline (Visual Component)

- Renders a horizontal timeline from earliest slot start to latest slot end
- Each slot displayed as a colored block:
  - LECTURE: blue (`bg-blue-500`)
  - TUTORIAL: green (`bg-green-500`)
  - PRACTICAL: orange (`bg-orange-500`)
  - BREAK: gray (`bg-gray-300`)
  - LUNCH: yellow (`bg-yellow-400`)
- Block shows: slot number, start–end time, duration, type label
- Proportional widths based on duration

### 3.12 SlotForm

- **Fields:** Slot Number (auto-incremented), Start Time (time picker), End Time (time picker), Slot Type (select)
- Duration auto-calculated and displayed
- On 409 (overlap): inline error "Slot times overlap with existing slot"

### 3.13 QuickSetupWizard

- Multi-step dialog:
  1. **Step 1:** Base configuration — First slot start time, slot duration (minutes), number of lecture slots
  2. **Step 2:** Breaks — After which slot number to insert a break, break duration; lunch slot number, lunch duration
  3. **Step 3:** Preview — Shows generated slots in a mini SlotTimeline; user confirms or adjusts
  4. **Step 4:** Submit — Calls `useBulkCreateSlots()` mutation
- Generates slot array from the pattern and submits via bulk endpoint

### 3.14 WorkingDayToggles

- 7 toggle buttons (Monday → Sunday), each showing ON (filled) or OFF (outline)
- Click immediately calls `useUpdateWorkingDays()` mutation (auto-save)
- The last remaining ON toggle is disabled (cannot create a 0-day week)
- Optimistic update: toggle flips immediately, reverts on error

### 3.15 GridActivationDialog

- Confirmation dialog: "Activating this grid will deactivate the current active grid for [Campus Name]. Continue?"
- Calls `useActivateGrid(gridId)` mutation
- On success: grid status badge updates, list refreshes to show new active state

---

## 4. API Client Layer

### 4.1 Calendar API Module

```typescript
// src/api/calendarApi.ts
import { apiClient } from './client';
import type {
  AcademicCalendar,
  CalendarCreateRequest,
  CalendarUpdateRequest,
  CalendarListParams,
  Holiday,
  HolidayCreateRequest,
  ExamWindow,
  ExamWindowCreateRequest,
  SpecialPeriod,
  SpecialPeriodCreateRequest,
  ImpactAnalysis,
  PaginatedResponse,
} from '@/types/scheduling-config';

export const calendarApi = {
  // Calendar CRUD
  getAll: (params: CalendarListParams): Promise<PaginatedResponse<AcademicCalendar>> =>
    apiClient.get('/academic-calendars', { params }).then((r) => r.data),

  getById: (id: string): Promise<AcademicCalendar> =>
    apiClient.get(`/academic-calendars/${id}`).then((r) => r.data),

  create: (data: CalendarCreateRequest): Promise<AcademicCalendar> =>
    apiClient.post('/academic-calendars', data).then((r) => r.data),

  update: (id: string, data: CalendarUpdateRequest): Promise<AcademicCalendar> =>
    apiClient.put(`/academic-calendars/${id}`, data).then((r) => r.data),

  delete: (id: string): Promise<void> =>
    apiClient.delete(`/academic-calendars/${id}`).then(() => undefined),

  // Holidays
  getHolidays: (calendarId: string): Promise<Holiday[]> =>
    apiClient.get(`/academic-calendars/${calendarId}/holidays`).then((r) => r.data),

  createHoliday: (calendarId: string, data: HolidayCreateRequest): Promise<Holiday> =>
    apiClient.post(`/academic-calendars/${calendarId}/holidays`, data).then((r) => r.data),

  deleteHoliday: (calendarId: string, holidayId: string): Promise<void> =>
    apiClient.delete(`/academic-calendars/${calendarId}/holidays/${holidayId}`).then(() => undefined),

  // Exam Windows
  getExamWindows: (calendarId: string): Promise<ExamWindow[]> =>
    apiClient.get(`/academic-calendars/${calendarId}/exam-windows`).then((r) => r.data),

  createExamWindow: (calendarId: string, data: ExamWindowCreateRequest): Promise<ExamWindow> =>
    apiClient.post(`/academic-calendars/${calendarId}/exam-windows`, data).then((r) => r.data),

  deleteExamWindow: (calendarId: string, windowId: string): Promise<void> =>
    apiClient.delete(`/academic-calendars/${calendarId}/exam-windows/${windowId}`).then(() => undefined),

  // Special Periods
  getSpecialPeriods: (calendarId: string): Promise<SpecialPeriod[]> =>
    apiClient.get(`/academic-calendars/${calendarId}/special-periods`).then((r) => r.data),

  createSpecialPeriod: (calendarId: string, data: SpecialPeriodCreateRequest): Promise<SpecialPeriod> =>
    apiClient.post(`/academic-calendars/${calendarId}/special-periods`, data).then((r) => r.data),

  deleteSpecialPeriod: (calendarId: string, periodId: string): Promise<void> =>
    apiClient.delete(`/academic-calendars/${calendarId}/special-periods/${periodId}`).then(() => undefined),

  // Impact Analysis
  getImpactAnalysis: (calendarId: string, date: string): Promise<ImpactAnalysis> =>
    apiClient.get(`/academic-calendars/${calendarId}/impact-analysis`, { params: { date } }).then((r) => r.data),
};
```

### 4.2 Grid API Module

```typescript
// src/api/gridApi.ts
import { apiClient } from './client';
import type {
  TimeSlotGrid,
  GridCreateRequest,
  GridUpdateRequest,
  GridListParams,
  SlotDefinition,
  SlotCreateRequest,
  BulkSlotCreateRequest,
  WorkingDays,
  WorkingDaysUpdateRequest,
  PaginatedResponse,
} from '@/types/scheduling-config';

export const gridApi = {
  // Grid CRUD
  getAll: (params: GridListParams): Promise<PaginatedResponse<TimeSlotGrid>> =>
    apiClient.get('/time-slot-grids', { params }).then((r) => r.data),

  getById: (id: string): Promise<TimeSlotGrid> =>
    apiClient.get(`/time-slot-grids/${id}`).then((r) => r.data),

  create: (data: GridCreateRequest): Promise<TimeSlotGrid> =>
    apiClient.post('/time-slot-grids', data).then((r) => r.data),

  update: (id: string, data: GridUpdateRequest): Promise<TimeSlotGrid> =>
    apiClient.put(`/time-slot-grids/${id}`, data).then((r) => r.data),

  delete: (id: string): Promise<void> =>
    apiClient.delete(`/time-slot-grids/${id}`).then(() => undefined),

  activate: (id: string): Promise<TimeSlotGrid> =>
    apiClient.put(`/time-slot-grids/${id}/activate`).then((r) => r.data),

  // Slots
  getSlots: (gridId: string): Promise<SlotDefinition[]> =>
    apiClient.get(`/time-slot-grids/${gridId}/slots`).then((r) => r.data),

  createSlot: (gridId: string, data: SlotCreateRequest): Promise<SlotDefinition> =>
    apiClient.post(`/time-slot-grids/${gridId}/slots`, data).then((r) => r.data),

  bulkCreateSlots: (gridId: string, data: BulkSlotCreateRequest): Promise<SlotDefinition[]> =>
    apiClient.post(`/time-slot-grids/${gridId}/slots/bulk`, data).then((r) => r.data),

  deleteSlot: (gridId: string, slotId: string): Promise<void> =>
    apiClient.delete(`/time-slot-grids/${gridId}/slots/${slotId}`).then(() => undefined),

  // Working Days
  getWorkingDays: (gridId: string): Promise<WorkingDays> =>
    apiClient.get(`/time-slot-grids/${gridId}/working-days`).then((r) => r.data),

  updateWorkingDays: (gridId: string, data: WorkingDaysUpdateRequest): Promise<WorkingDays> =>
    apiClient.put(`/time-slot-grids/${gridId}/working-days`, data).then((r) => r.data),

  // Active grid per campus
  getActiveGrid: (campusId: string): Promise<TimeSlotGrid> =>
    apiClient.get(`/campuses/${campusId}/active-grid`).then((r) => r.data),
};
```

### 4.3 Types

```typescript
// src/types/scheduling-config.ts

export interface AcademicCalendar {
  id: string;
  name: string;
  campusId: string;
  campusName: string;
  academicYear: string;
  semesterType: 'ODD' | 'EVEN' | 'SUMMER';
  startDate: string; // ISO date
  endDate: string;
  status: 'ACTIVE' | 'DRAFT';
  createdAt: string;
  updatedAt: string;
}

export interface Holiday {
  id: string;
  calendarId: string;
  name: string;
  date: string;
  dayType: 'FULL_DAY' | 'HALF_DAY_AM' | 'HALF_DAY_PM';
  isRecurring: boolean;
}

export interface ExamWindow {
  id: string;
  calendarId: string;
  name: string;
  examType: 'MID_SEM' | 'END_SEM' | 'SUPPLEMENTARY';
  startDate: string;
  endDate: string;
}

export interface SpecialPeriod {
  id: string;
  calendarId: string;
  name: string;
  periodType: 'ORIENTATION' | 'REGISTRATION' | 'BREAK' | 'REVISION';
  startDate: string;
  endDate: string;
}

export interface ImpactAnalysis {
  impactedSessions: ImpactedSession[];
  totalCount: number;
}

export interface ImpactedSession {
  sessionId: string;
  courseName: string;
  facultyName: string;
  roomName: string;
  batchName: string;
  timeSlot: string;
}

export interface TimeSlotGrid {
  id: string;
  name: string;
  campusId: string;
  campusName: string;
  effectiveFrom: string;
  status: 'ACTIVE' | 'INACTIVE';
  slotCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface SlotDefinition {
  id: string;
  gridId: string;
  slotNumber: number;
  startTime: string; // HH:mm
  endTime: string;   // HH:mm
  durationMinutes: number;
  slotType: 'LECTURE' | 'TUTORIAL' | 'PRACTICAL' | 'BREAK' | 'LUNCH';
}

export interface WorkingDayEntry {
  dayOfWeek: number; // 1=Monday, 7=Sunday
  dayName: string;
  isWorkingDay: boolean;
}

export interface WorkingDays {
  gridId: string;
  days: WorkingDayEntry[];
}

// Request types
export interface CalendarCreateRequest {
  name: string;
  campusId: string;
  academicYear: string;
  semesterType: 'ODD' | 'EVEN' | 'SUMMER';
  startDate: string;
  endDate: string;
}

export type CalendarUpdateRequest = CalendarCreateRequest;
export type CalendarListParams = {
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  campusId?: string;
  academicYear?: string;
  semesterType?: string;
};

export interface HolidayCreateRequest {
  name: string;
  date: string;
  dayType: 'FULL_DAY' | 'HALF_DAY_AM' | 'HALF_DAY_PM';
  isRecurring: boolean;
}

export interface ExamWindowCreateRequest {
  name: string;
  examType: 'MID_SEM' | 'END_SEM' | 'SUPPLEMENTARY';
  startDate: string;
  endDate: string;
}

export interface SpecialPeriodCreateRequest {
  name: string;
  periodType: 'ORIENTATION' | 'REGISTRATION' | 'BREAK' | 'REVISION';
  startDate: string;
  endDate: string;
}

export interface GridCreateRequest {
  name: string;
  campusId: string;
  effectiveFrom: string;
}

export type GridUpdateRequest = GridCreateRequest;
export type GridListParams = {
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  campusId?: string;
};

export interface SlotCreateRequest {
  slotNumber: number;
  startTime: string;
  endTime: string;
  slotType: 'LECTURE' | 'TUTORIAL' | 'PRACTICAL' | 'BREAK' | 'LUNCH';
}

export interface BulkSlotCreateRequest {
  slots: SlotCreateRequest[];
}

export interface WorkingDaysUpdateRequest {
  days: Array<{ dayOfWeek: number; isWorkingDay: boolean }>;
}
```

---

## 5. State Management

### 5.1 TanStack Query Keys

```typescript
// src/api/queryKeys.ts (extended)
export const queryKeys = {
  // ... existing keys from AID-325 ...

  calendars: {
    all: ['calendars'] as const,
    list: (params: CalendarListParams) => ['calendars', 'list', params] as const,
    detail: (id: string) => ['calendars', 'detail', id] as const,
    holidays: (calendarId: string) => ['calendars', calendarId, 'holidays'] as const,
    examWindows: (calendarId: string) => ['calendars', calendarId, 'exam-windows'] as const,
    specialPeriods: (calendarId: string) => ['calendars', calendarId, 'special-periods'] as const,
    impact: (calendarId: string, date: string) => ['calendars', calendarId, 'impact', date] as const,
  },

  grids: {
    all: ['grids'] as const,
    list: (params: GridListParams) => ['grids', 'list', params] as const,
    detail: (id: string) => ['grids', 'detail', id] as const,
    slots: (gridId: string) => ['grids', gridId, 'slots'] as const,
    workingDays: (gridId: string) => ['grids', gridId, 'working-days'] as const,
    active: (campusId: string) => ['grids', 'active', campusId] as const,
  },
} as const;
```

### 5.2 Zustand Store (Scheduling Config UI)

```typescript
// src/stores/schedulingConfigStore.ts
import { create } from 'zustand';

interface SchedulingConfigState {
  activeCalendarTab: 'holidays' | 'exam-windows' | 'special-periods';
  setActiveCalendarTab: (tab: SchedulingConfigState['activeCalendarTab']) => void;
  quickSetupOpen: boolean;
  setQuickSetupOpen: (open: boolean) => void;
}

export const useSchedulingConfigStore = create<SchedulingConfigState>((set) => ({
  activeCalendarTab: 'holidays',
  setActiveCalendarTab: (tab) => set({ activeCalendarTab: tab }),
  quickSetupOpen: false,
  setQuickSetupOpen: (open) => set({ quickSetupOpen: open }),
}));
```

### 5.3 Caching Strategy

| Query | Stale Time | GC Time | Refetch On |
|-------|-----------|---------|------------|
| Calendar list | 30 seconds | 5 minutes | Window focus, mutation success |
| Calendar detail | 60 seconds | 5 minutes | Window focus |
| Holidays/ExamWindows/SpecialPeriods | 30 seconds | 5 minutes | Mutation success |
| Impact analysis | 0 (always fresh) | 30 seconds | On demand only |
| Grid list | 30 seconds | 5 minutes | Window focus, mutation success |
| Grid detail | 60 seconds | 5 minutes | Window focus |
| Slots | 30 seconds | 5 minutes | Mutation success |
| Working days | 60 seconds | 5 minutes | Mutation success |
| Active grid | 60 seconds | 5 minutes | Grid activation |

### 5.4 Optimistic Updates

Applied selectively for instant-feeling interactions:

- **WorkingDayToggles:** Optimistic update on toggle click; revert on error with toast.
- **Grid Activation:** Not optimistic (wait for confirmation to avoid inconsistent multi-grid state).
- **All other mutations:** Invalidate on success (simpler, avoids rollback complexity).

---

## 6. Form Validation (Zod)

### 6.1 Calendar Schema

```typescript
// src/features/scheduling-config/calendar/schemas.ts
import { z } from 'zod';
import { isAfter, isValid, parse } from 'date-fns';

export const calendarSchema = z
  .object({
    name: z
      .string()
      .min(1, 'Calendar name is required')
      .max(200, 'Name must be 200 characters or fewer')
      .trim(),
    campusId: z
      .string()
      .uuid('Please select a campus'),
    academicYear: z
      .string()
      .min(1, 'Academic year is required')
      .regex(
        /^\d{4}-\d{4}$/,
        'Academic year must be in YYYY-YYYY format (e.g., 2026-2027)'
      )
      .refine((val) => {
        const [start, end] = val.split('-').map(Number);
        return end === start + 1;
      }, 'End year must be exactly one year after start year'),
    semesterType: z.enum(['ODD', 'EVEN', 'SUMMER'], {
      required_error: 'Semester type is required',
    }),
    startDate: z
      .string()
      .min(1, 'Start date is required')
      .refine((val) => {
        const parsed = parse(val, 'yyyy-MM-dd', new Date());
        return isValid(parsed);
      }, 'Invalid date format'),
    endDate: z
      .string()
      .min(1, 'End date is required')
      .refine((val) => {
        const parsed = parse(val, 'yyyy-MM-dd', new Date());
        return isValid(parsed);
      }, 'Invalid date format'),
  })
  .refine(
    (data) => {
      const start = parse(data.startDate, 'yyyy-MM-dd', new Date());
      const end = parse(data.endDate, 'yyyy-MM-dd', new Date());
      return isAfter(end, start);
    },
    {
      message: 'End date must be after start date',
      path: ['endDate'],
    }
  );

export type CalendarFormData = z.infer<typeof calendarSchema>;
```

### 6.2 Holiday Schema

```typescript
export const holidaySchema = z.object({
  name: z
    .string()
    .min(1, 'Holiday name is required')
    .max(200, 'Name must be 200 characters or fewer')
    .trim(),
  date: z
    .string()
    .min(1, 'Date is required')
    .refine((val) => isValid(parse(val, 'yyyy-MM-dd', new Date())), 'Invalid date'),
  dayType: z.enum(['FULL_DAY', 'HALF_DAY_AM', 'HALF_DAY_PM'], {
    required_error: 'Day type is required',
  }),
  isRecurring: z.boolean().default(false),
});

export type HolidayFormData = z.infer<typeof holidaySchema>;
```

### 6.3 Exam Window Schema

```typescript
export const examWindowSchema = z
  .object({
    name: z.string().min(1, 'Name is required').max(200).trim(),
    examType: z.enum(['MID_SEM', 'END_SEM', 'SUPPLEMENTARY'], {
      required_error: 'Exam type is required',
    }),
    startDate: z.string().min(1, 'Start date is required'),
    endDate: z.string().min(1, 'End date is required'),
  })
  .refine(
    (data) => {
      const start = parse(data.startDate, 'yyyy-MM-dd', new Date());
      const end = parse(data.endDate, 'yyyy-MM-dd', new Date());
      return isAfter(end, start);
    },
    { message: 'End date must be after start date', path: ['endDate'] }
  );

export type ExamWindowFormData = z.infer<typeof examWindowSchema>;
```

### 6.4 Special Period Schema

```typescript
export const specialPeriodSchema = z
  .object({
    name: z.string().min(1, 'Name is required').max(200).trim(),
    periodType: z.enum(['ORIENTATION', 'REGISTRATION', 'BREAK', 'REVISION'], {
      required_error: 'Period type is required',
    }),
    startDate: z.string().min(1, 'Start date is required'),
    endDate: z.string().min(1, 'End date is required'),
  })
  .refine(
    (data) => {
      const start = parse(data.startDate, 'yyyy-MM-dd', new Date());
      const end = parse(data.endDate, 'yyyy-MM-dd', new Date());
      return isAfter(end, start);
    },
    { message: 'End date must be after start date', path: ['endDate'] }
  );

export type SpecialPeriodFormData = z.infer<typeof specialPeriodSchema>;
```

### 6.5 Grid Schema

```typescript
// src/features/scheduling-config/grid/schemas.ts
import { z } from 'zod';

export const gridSchema = z.object({
  name: z
    .string()
    .min(1, 'Grid name is required')
    .max(200, 'Name must be 200 characters or fewer')
    .trim(),
  campusId: z.string().uuid('Please select a campus'),
  effectiveFrom: z
    .string()
    .min(1, 'Effective from date is required')
    .refine(
      (val) => isValid(parse(val, 'yyyy-MM-dd', new Date())),
      'Invalid date format'
    ),
});

export type GridFormData = z.infer<typeof gridSchema>;
```

### 6.6 Slot Schema

```typescript
export const slotSchema = z
  .object({
    slotNumber: z.coerce.number().int().min(1, 'Slot number must be at least 1').max(20),
    startTime: z
      .string()
      .min(1, 'Start time is required')
      .regex(/^\d{2}:\d{2}$/, 'Time must be in HH:mm format'),
    endTime: z
      .string()
      .min(1, 'End time is required')
      .regex(/^\d{2}:\d{2}$/, 'Time must be in HH:mm format'),
    slotType: z.enum(['LECTURE', 'TUTORIAL', 'PRACTICAL', 'BREAK', 'LUNCH'], {
      required_error: 'Slot type is required',
    }),
  })
  .refine(
    (data) => {
      const [sh, sm] = data.startTime.split(':').map(Number);
      const [eh, em] = data.endTime.split(':').map(Number);
      return eh * 60 + em > sh * 60 + sm;
    },
    { message: 'End time must be after start time', path: ['endTime'] }
  );

export type SlotFormData = z.infer<typeof slotSchema>;

export const bulkSlotsSchema = z.object({
  slots: z.array(slotSchema).min(1, 'At least one slot is required'),
});

export type BulkSlotsFormData = z.infer<typeof bulkSlotsSchema>;
```

### 6.7 Working Days Schema

```typescript
export const workingDaysSchema = z.object({
  days: z
    .array(
      z.object({
        dayOfWeek: z.number().int().min(1).max(7),
        isWorkingDay: z.boolean(),
      })
    )
    .length(7, 'Must include all 7 days')
    .refine(
      (days) => days.some((d) => d.isWorkingDay),
      'At least one working day is required'
    ),
});

export type WorkingDaysFormData = z.infer<typeof workingDaysSchema>;
```

---

## 7. Folder Structure

```
src/features/scheduling-config/
├── calendar/
│   ├── CalendarListPage.tsx
│   ├── CalendarFormPage.tsx
│   ├── CalendarDetailPage.tsx
│   ├── components/
│   │   ├── CalendarTimeline.tsx
│   │   ├── HolidayTab.tsx
│   │   ├── ExamWindowTab.tsx
│   │   ├── SpecialPeriodTab.tsx
│   │   ├── HolidayForm.tsx
│   │   ├── ExamWindowForm.tsx
│   │   ├── SpecialPeriodForm.tsx
│   │   └── ImpactWarningBanner.tsx
│   ├── hooks.ts
│   ├── schemas.ts
│   └── __tests__/
│       ├── CalendarListPage.test.tsx
│       ├── CalendarFormPage.test.tsx
│       ├── CalendarDetailPage.test.tsx
│       ├── CalendarTimeline.test.tsx
│       └── schemas.test.ts
├── grid/
│   ├── GridListPage.tsx
│   ├── GridFormPage.tsx
│   ├── GridDetailPage.tsx
│   ├── components/
│   │   ├── SlotTimeline.tsx
│   │   ├── SlotForm.tsx
│   │   ├── QuickSetupWizard.tsx
│   │   ├── WorkingDayToggles.tsx
│   │   └── GridActivationDialog.tsx
│   ├── hooks.ts
│   ├── schemas.ts
│   └── __tests__/
│       ├── GridListPage.test.tsx
│       ├── GridDetailPage.test.tsx
│       ├── SlotTimeline.test.tsx
│       ├── QuickSetupWizard.test.tsx
│       └── schemas.test.ts
└── routes.tsx              # Lazy-loaded route definitions
```

Additional files in shared locations:

```
src/api/
├── calendarApi.ts
├── gridApi.ts
└── queryKeys.ts           # Extended with calendar/grid keys

src/types/
└── scheduling-config.ts   # All types for this module

src/routes/
└── schedulingConfigRoutes.tsx  # Route config integrated into main router
```

---

## 8. Security

### 8.1 XSS Prevention

- **No `dangerouslySetInnerHTML`** in any component. All text rendered via JSX auto-escaping.
- **DOMPurify** sanitization applied to any API response text rendered in tooltips or banners.
- All form inputs validated via Zod with strict format allowlists before submission.

### 8.2 Content Security Policy

Inherits the existing CSP configuration from AID-325:
```
Content-Security-Policy:
  default-src 'self';
  script-src 'self';
  style-src 'self' 'unsafe-inline';
  img-src 'self' data:;
  connect-src 'self' ${API_BASE_URL};
  font-src 'self';
  frame-src 'none';
  object-src 'none';
```

### 8.3 Input Sanitization

- Calendar names, holiday names: validated with `.trim()` and max-length constraints.
- Date inputs: validated via `date-fns` parsing — rejects malformed dates.
- Time inputs: validated with regex `/^\d{2}:\d{2}$/` — strict format only.
- Enum fields: Zod `.enum()` restricts to exact allowed values.
- No user input is ever passed to `eval()`, `innerHTML`, or URL parameters without encoding.

### 8.4 Authorization

- Routes are guarded by the existing `AuthGuard` component.
- API calls include the JWT token via the shared Axios interceptor.
- Role-based visibility: "Activate" button only shown to Admin/Registrar roles (checked via `useAuth()` hook).

---

## 9. Non-Functional Design

### 9.1 Loading States

- **List pages:** Table body replaced with animated skeleton rows during fetch.
- **Detail pages:** Card skeleton with timeline placeholder while calendar/grid loads.
- **Sub-resource tabs:** Individual tab content shows skeleton while sub-resource data loads.
- **Mutations in progress:** Submit buttons disabled with spinner icon; toggles show loading indicator.

### 9.2 Error Boundaries

- Top-level `ErrorBoundary` wraps the scheduling-config route segment.
- Per-component `ErrorState` with "Retry" button for individual query failures.
- 409 (conflict) errors surfaced inline in forms with specific field-level messages.
- Network errors show toast notification with retry option.

### 9.3 Responsive Design

- **Desktop-first** layout optimized for admin workflows.
- **Tablet (768px–1023px):** Timeline components stack vertically; form fields go full-width.
- **Mobile (< 768px):** Timeline simplified to list view; toggle buttons stack vertically.
- `CalendarTimeline` and `SlotTimeline` have a minimum width and are horizontally scrollable on small screens.

### 9.4 Accessibility

- All interactive elements have ARIA labels.
- Timeline components use `role="img"` with `aria-label` describing the timeline content.
- Individual timeline items have `aria-describedby` for tooltip content.
- Tab navigation uses `role="tablist"`, `role="tab"`, `role="tabpanel"` with `aria-selected`.
- WorkingDayToggles use `role="switch"` with `aria-checked` and `aria-label` per day.
- Date pickers accessible via keyboard (arrow keys to navigate, Enter to select).
- Color contrast meets WCAG 2.1 AA (4.5:1 for text, 3:1 for UI components).
- Focus management: dialogs trap focus; closing returns focus to trigger element.
- SlotTimeline colors are supplemented with pattern/icon indicators for color-blind users.

### 9.5 Performance

- Route-level code splitting via `React.lazy()` for calendar and grid page bundles.
- Timeline components memoized with `React.memo()` — only re-render when data changes.
- `date-fns` tree-shaken — only import used functions (not the whole library).
- Bulk slot creation minimizes re-renders by batching state updates.

---

## 10. Testing Strategy

### 10.1 Unit / Component Tests (Vitest + React Testing Library)

| What | Coverage Target |
|------|----------------|
| Zod schemas (all 7 schemas) | 100% — valid/invalid cases, edge cases |
| CalendarTimeline rendering | 90%+ — correct positioning, colors, tooltips |
| SlotTimeline rendering | 90%+ — proportional widths, color mapping |
| QuickSetupWizard logic | 90%+ — slot generation algorithm, edge cases |
| WorkingDayToggles | 90%+ — toggle state, disable last day, auto-save |
| Form components | 80%+ — validation, submit, error mapping |
| Hook testing (renderHook) | 80%+ — query params, mutation callbacks |

### 10.2 Integration Tests (MSW)

- Mock all `/academic-calendars/*` and `/time-slot-grids/*` endpoints.
- Test flows:
  - Create calendar → navigate to detail → add holiday → verify timeline update
  - Create grid → add slots via Quick Setup → verify timeline → activate
  - Toggle working day → verify auto-save → verify disable-last behavior
  - Add holiday with impacted sessions → verify warning banner

### 10.3 E2E Tests (Playwright — Future Phase)

- Smoke tests: login → navigate to calendars → create calendar → add holiday → verify timeline
- Time-slot grid: create → quick setup → activate → verify active badge

### 10.4 Test File Structure

```
src/features/scheduling-config/calendar/__tests__/
├── CalendarListPage.test.tsx
├── CalendarFormPage.test.tsx
├── CalendarDetailPage.test.tsx
├── CalendarTimeline.test.tsx
├── HolidayForm.test.tsx
├── ImpactWarningBanner.test.tsx
└── schemas.test.ts

src/features/scheduling-config/grid/__tests__/
├── GridListPage.test.tsx
├── GridDetailPage.test.tsx
├── SlotTimeline.test.tsx
├── QuickSetupWizard.test.tsx
├── WorkingDayToggles.test.tsx
├── GridActivationDialog.test.tsx
└── schemas.test.ts
```

---

## 11. Requirement Traceability

| FR | Component(s) | Implementation Notes |
|----|-------------|---------------------|
| FR-1.1 | `CalendarListPage`, `DataTable` | Columns: Name, Campus, Academic Year, Semester Type, Start/End Date, Status |
| FR-1.2 | `FilterBar` (campus, academic year, semester type dropdowns) | Params sent to `useCalendars` query |
| FR-1.3 | `DataTable` column headers | Sort by name, startDate, academicYear |
| FR-1.4 | `ActionMenu` | View → detail, Edit → form, Delete → confirm dialog |
| FR-1.5 | `CalendarListPage` header button | Navigates to `/calendars/new` |
| FR-2.1 | `CalendarFormPage`, `calendarSchema` | All fields with Zod validation |
| FR-2.2 | `calendarSchema` `.refine()` | Cross-field date validation |
| FR-2.3 | Mutation `onError` + `form.setError()` | 409 → inline conflict message |
| FR-2.4 | Mutation `onSuccess` | `navigate(/calendars/${id})` |
| FR-3.1 | `CalendarDetailPage`, `Tabs` | Three tabbed sections |
| FR-3.2 | `HolidayTab`, `ExamWindowTab`, `SpecialPeriodTab` | List + Add/Delete actions |
| FR-3.3 | `CalendarTimeline` | Visual bar with colored markers/bands |
| FR-4.1 | `HolidayForm`, `holidaySchema` | All fields validated |
| FR-4.2 | `HolidayForm` date picker | `minDate`/`maxDate` props from calendar range |
| FR-4.3 | `ImpactWarningBanner` | Conditionally rendered after holiday creation |
| FR-4.4 | `ImpactWarningBanner` expandable list | Table: Course, Faculty, Room, Batch, Time Slot |
| FR-5.1 | `ExamWindowForm`, `examWindowSchema` | All fields validated |
| FR-5.2 | `ExamWindowForm` date pickers | Constrained to calendar range |
| FR-5.3 | `CalendarTimeline` | Orange bands for exam windows |
| FR-6.1 | `SpecialPeriodForm`, `specialPeriodSchema` | All fields validated |
| FR-6.2 | `SpecialPeriodForm` date pickers | Constrained to calendar range |
| FR-6.3 | `CalendarTimeline` | Blue bands for special periods |
| FR-7.1 | `GridListPage`, `DataTable` | Columns: Name, Campus, Effective From, Status, Slot Count |
| FR-7.2 | `FilterBar` (campus dropdown) | Param sent to `useGrids` query |
| FR-7.3 | `GridListPage` status badge | Green "Active" / Gray "Inactive" badges |
| FR-7.4 | `ActionMenu` + "Activate" button | Conditional visibility based on status + slot count |
| FR-8.1 | `GridDetailPage` | Metadata header + timeline + working days |
| FR-8.2 | `SlotTimeline` | Colored blocks on horizontal axis |
| FR-8.3 | `SlotTimeline` color mapping | Blue/Green/Orange/Gray/Yellow by type |
| FR-8.4 | `SlotTimeline` block content | Slot number, times, duration, type |
| FR-9.1 | `SlotForm`, `slotSchema` | All fields with validation |
| FR-9.2 | `SlotForm` duration display | Auto-calculated on time change |
| FR-9.3 | Mutation `onError` | 409 → inline overlap error |
| FR-9.4 | `QuickSetupWizard` | Multi-step dialog with pattern config + preview |
| FR-10.1 | `WorkingDayToggles` | 7 toggle buttons (Mon-Sun) |
| FR-10.2 | `WorkingDayToggles` disable logic | Last ON toggle disabled |
| FR-10.3 | `WorkingDayToggles` auto-save | Optimistic PUT on toggle click |
| FR-11.1 | `GridDetailPage` Activate button | Visible when inactive + has slots + working days |
| FR-11.2 | `GridActivationDialog` | Confirmation modal with warning text |
| FR-11.3 | `useActivateGrid` mutation `onSuccess` | Invalidates grid list + detail queries |
| FR-12.1 | `Sidebar` navigation | "Scheduling Config" section with sub-items |
| FR-12.2 | `Breadcrumbs` | Dynamic path from route params + entity names |
| FR-12.3 | Tailwind responsive classes | Desktop-first with breakpoints |
| FR-13.1 | `LoadingSkeleton` | Shown during `isLoading` state |
| FR-13.2 | `ErrorState` | Friendly message + Retry button |
| FR-13.3 | Axios 401 interceptor | Redirects to `/login` |

---

## 12. Code Examples

### 12.1 CalendarTimeline Component

```typescript
// src/features/scheduling-config/calendar/components/CalendarTimeline.tsx
import { useMemo } from 'react';
import {
  differenceInDays,
  format,
  parseISO,
  isWithinInterval,
} from 'date-fns';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import type { Holiday, ExamWindow, SpecialPeriod } from '@/types/scheduling-config';

interface CalendarTimelineProps {
  startDate: string;
  endDate: string;
  holidays: Holiday[];
  examWindows: ExamWindow[];
  specialPeriods: SpecialPeriod[];
}

export function CalendarTimeline({
  startDate,
  endDate,
  holidays,
  examWindows,
  specialPeriods,
}: CalendarTimelineProps) {
  const start = parseISO(startDate);
  const end = parseISO(endDate);
  const totalDays = differenceInDays(end, start);

  const getPositionPercent = (date: string): number => {
    const d = parseISO(date);
    const dayOffset = differenceInDays(d, start);
    return Math.max(0, Math.min(100, (dayOffset / totalDays) * 100));
  };

  const getRangeStyle = (rangeStart: string, rangeEnd: string) => {
    const leftPercent = getPositionPercent(rangeStart);
    const rightPercent = getPositionPercent(rangeEnd);
    const widthPercent = Math.max(0.5, rightPercent - leftPercent);
    return { left: `${leftPercent}%`, width: `${widthPercent}%` };
  };

  const monthMarkers = useMemo(() => {
    const markers: Array<{ label: string; percent: number }> = [];
    const current = new Date(start);
    current.setDate(1);
    current.setMonth(current.getMonth() + 1);

    while (current <= end) {
      const percent = (differenceInDays(current, start) / totalDays) * 100;
      if (percent > 0 && percent < 100) {
        markers.push({ label: format(current, 'MMM'), percent });
      }
      current.setMonth(current.getMonth() + 1);
    }
    return markers;
  }, [start, end, totalDays]);

  return (
    <TooltipProvider>
      <div
        className="relative w-full rounded-lg border bg-muted/30 p-4"
        role="img"
        aria-label={`Calendar timeline from ${format(start, 'MMM d, yyyy')} to ${format(end, 'MMM d, yyyy')}`}
      >
        {/* Date range labels */}
        <div className="mb-2 flex justify-between text-xs text-muted-foreground">
          <span>{format(start, 'MMM d, yyyy')}</span>
          <span>{format(end, 'MMM d, yyyy')}</span>
        </div>

        {/* Timeline bar */}
        <div className="relative h-12 rounded-md bg-gray-200">
          {/* Month markers */}
          {monthMarkers.map((marker) => (
            <div
              key={marker.label}
              className="absolute top-0 h-full border-l border-dashed border-gray-400"
              style={{ left: `${marker.percent}%` }}
            >
              <span className="absolute -top-5 -translate-x-1/2 text-[10px] text-muted-foreground">
                {marker.label}
              </span>
            </div>
          ))}

          {/* Exam windows (orange bands) */}
          {examWindows.map((ew) => (
            <Tooltip key={ew.id}>
              <TooltipTrigger asChild>
                <div
                  className="absolute top-1 h-4 rounded-sm bg-orange-400/70 hover:bg-orange-500/90 cursor-pointer transition-colors"
                  style={getRangeStyle(ew.startDate, ew.endDate)}
                  aria-label={`Exam window: ${ew.name}`}
                />
              </TooltipTrigger>
              <TooltipContent>
                <p className="font-medium">{ew.name}</p>
                <p className="text-xs">
                  {format(parseISO(ew.startDate), 'MMM d')} – {format(parseISO(ew.endDate), 'MMM d')}
                </p>
                <p className="text-xs text-muted-foreground">{ew.examType.replace('_', ' ')}</p>
              </TooltipContent>
            </Tooltip>
          ))}

          {/* Special periods (blue bands) */}
          {specialPeriods.map((sp) => (
            <Tooltip key={sp.id}>
              <TooltipTrigger asChild>
                <div
                  className="absolute top-7 h-4 rounded-sm bg-blue-400/70 hover:bg-blue-500/90 cursor-pointer transition-colors"
                  style={getRangeStyle(sp.startDate, sp.endDate)}
                  aria-label={`Special period: ${sp.name}`}
                />
              </TooltipTrigger>
              <TooltipContent>
                <p className="font-medium">{sp.name}</p>
                <p className="text-xs">
                  {format(parseISO(sp.startDate), 'MMM d')} – {format(parseISO(sp.endDate), 'MMM d')}
                </p>
                <p className="text-xs text-muted-foreground">{sp.periodType}</p>
              </TooltipContent>
            </Tooltip>
          ))}

          {/* Holidays (red dots) */}
          {holidays.map((h) => (
            <Tooltip key={h.id}>
              <TooltipTrigger asChild>
                <div
                  className="absolute top-1/2 h-3 w-3 -translate-y-1/2 rounded-full bg-red-500 hover:bg-red-600 ring-2 ring-white cursor-pointer transition-colors"
                  style={{ left: `${getPositionPercent(h.date)}%` }}
                  aria-label={`Holiday: ${h.name} on ${format(parseISO(h.date), 'MMM d')}`}
                />
              </TooltipTrigger>
              <TooltipContent>
                <p className="font-medium">{h.name}</p>
                <p className="text-xs">{format(parseISO(h.date), 'EEEE, MMM d, yyyy')}</p>
                <p className="text-xs text-muted-foreground">
                  {h.dayType.replace(/_/g, ' ')} {h.isRecurring ? '(Recurring)' : ''}
                </p>
              </TooltipContent>
            </Tooltip>
          ))}
        </div>

        {/* Legend */}
        <div className="mt-3 flex gap-4 text-xs text-muted-foreground">
          <div className="flex items-center gap-1">
            <div className="h-2.5 w-2.5 rounded-full bg-red-500" />
            <span>Holiday</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="h-2.5 w-4 rounded-sm bg-orange-400" />
            <span>Exam Window</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="h-2.5 w-4 rounded-sm bg-blue-400" />
            <span>Special Period</span>
          </div>
        </div>
      </div>
    </TooltipProvider>
  );
}
```

### 12.2 SlotTimeline Component

```typescript
// src/features/scheduling-config/grid/components/SlotTimeline.tsx
import { useMemo } from 'react';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import type { SlotDefinition } from '@/types/scheduling-config';

interface SlotTimelineProps {
  slots: SlotDefinition[];
}

const SLOT_COLORS: Record<SlotDefinition['slotType'], string> = {
  LECTURE: 'bg-blue-500 text-white',
  TUTORIAL: 'bg-green-500 text-white',
  PRACTICAL: 'bg-orange-500 text-white',
  BREAK: 'bg-gray-300 text-gray-700',
  LUNCH: 'bg-yellow-400 text-gray-800',
};

const SLOT_TYPE_LABELS: Record<SlotDefinition['slotType'], string> = {
  LECTURE: 'L',
  TUTORIAL: 'T',
  PRACTICAL: 'P',
  BREAK: 'Brk',
  LUNCH: 'Lch',
};

function timeToMinutes(time: string): number {
  const [hours, minutes] = time.split(':').map(Number);
  return hours * 60 + minutes;
}

export function SlotTimeline({ slots }: SlotTimelineProps) {
  const sortedSlots = useMemo(
    () => [...slots].sort((a, b) => timeToMinutes(a.startTime) - timeToMinutes(b.startTime)),
    [slots]
  );

  const { timelineStart, timelineEnd, totalMinutes } = useMemo(() => {
    if (sortedSlots.length === 0) {
      return { timelineStart: 480, timelineEnd: 1080, totalMinutes: 600 }; // 08:00-18:00 default
    }
    const start = timeToMinutes(sortedSlots[0].startTime);
    const end = timeToMinutes(sortedSlots[sortedSlots.length - 1].endTime);
    return { timelineStart: start, timelineEnd: end, totalMinutes: end - start };
  }, [sortedSlots]);

  const hourMarkers = useMemo(() => {
    const markers: Array<{ label: string; percent: number }> = [];
    const startHour = Math.floor(timelineStart / 60);
    const endHour = Math.ceil(timelineEnd / 60);

    for (let h = startHour; h <= endHour; h++) {
      const minuteOffset = h * 60 - timelineStart;
      const percent = (minuteOffset / totalMinutes) * 100;
      if (percent >= 0 && percent <= 100) {
        markers.push({
          label: `${h.toString().padStart(2, '0')}:00`,
          percent,
        });
      }
    }
    return markers;
  }, [timelineStart, timelineEnd, totalMinutes]);

  if (sortedSlots.length === 0) {
    return (
      <div className="rounded-lg border border-dashed p-8 text-center text-muted-foreground">
        No slots defined yet. Add slots manually or use Quick Setup.
      </div>
    );
  }

  return (
    <TooltipProvider>
      <div
        className="relative w-full overflow-x-auto rounded-lg border bg-muted/30 p-4"
        role="img"
        aria-label="Time slot grid timeline showing all slot definitions"
      >
        {/* Hour markers */}
        <div className="relative mb-1 h-4 min-w-[600px]">
          {hourMarkers.map((marker) => (
            <span
              key={marker.label}
              className="absolute -translate-x-1/2 text-[10px] text-muted-foreground"
              style={{ left: `${marker.percent}%` }}
            >
              {marker.label}
            </span>
          ))}
        </div>

        {/* Slot blocks */}
        <div className="relative flex min-w-[600px] gap-0.5 rounded-md bg-gray-100 p-1">
          {sortedSlots.map((slot) => {
            const startOffset = timeToMinutes(slot.startTime) - timelineStart;
            const widthPercent = (slot.durationMinutes / totalMinutes) * 100;
            const leftPercent = (startOffset / totalMinutes) * 100;

            return (
              <Tooltip key={slot.id}>
                <TooltipTrigger asChild>
                  <div
                    className={cn(
                      'absolute flex flex-col items-center justify-center rounded-md px-1 py-2 text-xs font-medium transition-opacity hover:opacity-90 cursor-default',
                      SLOT_COLORS[slot.slotType]
                    )}
                    style={{
                      left: `${leftPercent}%`,
                      width: `${widthPercent}%`,
                      minHeight: '3.5rem',
                    }}
                    aria-label={`Slot ${slot.slotNumber}: ${slot.slotType} from ${slot.startTime} to ${slot.endTime}`}
                  >
                    <span className="font-bold">
                      {SLOT_TYPE_LABELS[slot.slotType]}{slot.slotNumber}
                    </span>
                    <span className="text-[10px] opacity-80">
                      {slot.startTime}–{slot.endTime}
                    </span>
                    <span className="text-[10px] opacity-70">
                      {slot.durationMinutes}m
                    </span>
                  </div>
                </TooltipTrigger>
                <TooltipContent>
                  <p className="font-medium">
                    Slot {slot.slotNumber} — {slot.slotType}
                  </p>
                  <p className="text-xs">
                    {slot.startTime} → {slot.endTime} ({slot.durationMinutes} min)
                  </p>
                </TooltipContent>
              </Tooltip>
            );
          })}
        </div>

        {/* Legend */}
        <div className="mt-3 flex flex-wrap gap-3 text-xs text-muted-foreground">
          {Object.entries(SLOT_COLORS).map(([type, colorClass]) => (
            <div key={type} className="flex items-center gap-1">
              <div className={cn('h-3 w-5 rounded-sm', colorClass.split(' ')[0])} />
              <span>{type.charAt(0) + type.slice(1).toLowerCase()}</span>
            </div>
          ))}
        </div>
      </div>
    </TooltipProvider>
  );
}
```

### 12.3 QuickSetupWizard Logic

```typescript
// src/features/scheduling-config/grid/components/QuickSetupWizard.tsx
import { useState, useMemo } from 'react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useBulkCreateSlots } from '../hooks';
import { SlotTimeline } from './SlotTimeline';
import type { SlotCreateRequest, SlotDefinition } from '@/types/scheduling-config';

interface QuickSetupWizardProps {
  gridId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

interface WizardConfig {
  startTime: string;
  slotDuration: number;
  lectureCount: number;
  breakAfterSlot: number;
  breakDuration: number;
  lunchAfterSlot: number;
  lunchDuration: number;
  includeTutorial: boolean;
  tutorialDuration: number;
  includePractical: boolean;
  practicalDuration: number;
}

const DEFAULT_CONFIG: WizardConfig = {
  startTime: '08:00',
  slotDuration: 60,
  lectureCount: 6,
  breakAfterSlot: 2,
  breakDuration: 15,
  lunchAfterSlot: 4,
  lunchDuration: 45,
  includeTutorial: true,
  tutorialDuration: 60,
  includePractical: true,
  practicalDuration: 180,
};

function addMinutes(time: string, minutes: number): string {
  const [h, m] = time.split(':').map(Number);
  const total = h * 60 + m + minutes;
  const newH = Math.floor(total / 60);
  const newM = total % 60;
  return `${newH.toString().padStart(2, '0')}:${newM.toString().padStart(2, '0')}`;
}

function generateSlots(config: WizardConfig): SlotCreateRequest[] {
  const slots: SlotCreateRequest[] = [];
  let currentTime = config.startTime;
  let slotNumber = 1;
  let lecturesPlaced = 0;

  // Place lectures with breaks/lunch interspersed
  while (lecturesPlaced < config.lectureCount) {
    // Check if we need a break before this slot
    if (lecturesPlaced === config.breakAfterSlot && config.breakDuration > 0) {
      const breakEnd = addMinutes(currentTime, config.breakDuration);
      slots.push({
        slotNumber: slotNumber++,
        startTime: currentTime,
        endTime: breakEnd,
        slotType: 'BREAK',
      });
      currentTime = breakEnd;
    }

    // Check if we need lunch before this slot
    if (lecturesPlaced === config.lunchAfterSlot && config.lunchDuration > 0) {
      const lunchEnd = addMinutes(currentTime, config.lunchDuration);
      slots.push({
        slotNumber: slotNumber++,
        startTime: currentTime,
        endTime: lunchEnd,
        slotType: 'LUNCH',
      });
      currentTime = lunchEnd;
    }

    // Place lecture slot
    const lectureEnd = addMinutes(currentTime, config.slotDuration);
    slots.push({
      slotNumber: slotNumber++,
      startTime: currentTime,
      endTime: lectureEnd,
      slotType: 'LECTURE',
    });
    currentTime = lectureEnd;
    lecturesPlaced++;
  }

  // Optionally add tutorial slot
  if (config.includeTutorial) {
    const tutorialEnd = addMinutes(currentTime, config.tutorialDuration);
    slots.push({
      slotNumber: slotNumber++,
      startTime: currentTime,
      endTime: tutorialEnd,
      slotType: 'TUTORIAL',
    });
    currentTime = tutorialEnd;
  }

  // Optionally add practical slot
  if (config.includePractical) {
    const practicalEnd = addMinutes(currentTime, config.practicalDuration);
    slots.push({
      slotNumber: slotNumber++,
      startTime: currentTime,
      endTime: practicalEnd,
      slotType: 'PRACTICAL',
    });
  }

  return slots;
}

export function QuickSetupWizard({ gridId, open, onOpenChange }: QuickSetupWizardProps) {
  const [step, setStep] = useState(1);
  const [config, setConfig] = useState<WizardConfig>(DEFAULT_CONFIG);
  const bulkCreate = useBulkCreateSlots(gridId);

  const generatedSlots = useMemo(() => generateSlots(config), [config]);

  // Convert to preview format (with fake IDs for SlotTimeline)
  const previewSlots: SlotDefinition[] = useMemo(
    () =>
      generatedSlots.map((slot, index) => ({
        id: `preview-${index}`,
        gridId,
        slotNumber: slot.slotNumber,
        startTime: slot.startTime,
        endTime: slot.endTime,
        durationMinutes:
          timeToMinutes(slot.endTime) - timeToMinutes(slot.startTime),
        slotType: slot.slotType,
      })),
    [generatedSlots, gridId]
  );

  const handleSubmit = () => {
    bulkCreate.mutate(
      { slots: generatedSlots },
      {
        onSuccess: () => {
          onOpenChange(false);
          setStep(1);
          setConfig(DEFAULT_CONFIG);
        },
      }
    );
  };

  const updateConfig = (partial: Partial<WizardConfig>) => {
    setConfig((prev) => ({ ...prev, ...partial }));
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>
            Quick Setup Wizard — Step {step} of 3
          </DialogTitle>
        </DialogHeader>

        {step === 1 && (
          <div className="space-y-4">
            <h3 className="font-medium">Base Configuration</h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="startTime">First slot start time</Label>
                <Input
                  id="startTime"
                  type="time"
                  value={config.startTime}
                  onChange={(e) => updateConfig({ startTime: e.target.value })}
                />
              </div>
              <div>
                <Label htmlFor="slotDuration">Lecture duration (minutes)</Label>
                <Input
                  id="slotDuration"
                  type="number"
                  min={30}
                  max={180}
                  value={config.slotDuration}
                  onChange={(e) => updateConfig({ slotDuration: Number(e.target.value) })}
                />
              </div>
              <div>
                <Label htmlFor="lectureCount">Number of lecture slots</Label>
                <Input
                  id="lectureCount"
                  type="number"
                  min={1}
                  max={12}
                  value={config.lectureCount}
                  onChange={(e) => updateConfig({ lectureCount: Number(e.target.value) })}
                />
              </div>
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-4">
            <h3 className="font-medium">Breaks & Lunch</h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label htmlFor="breakAfterSlot">Break after lecture #</Label>
                <Input
                  id="breakAfterSlot"
                  type="number"
                  min={1}
                  max={config.lectureCount}
                  value={config.breakAfterSlot}
                  onChange={(e) => updateConfig({ breakAfterSlot: Number(e.target.value) })}
                />
              </div>
              <div>
                <Label htmlFor="breakDuration">Break duration (min)</Label>
                <Input
                  id="breakDuration"
                  type="number"
                  min={5}
                  max={30}
                  value={config.breakDuration}
                  onChange={(e) => updateConfig({ breakDuration: Number(e.target.value) })}
                />
              </div>
              <div>
                <Label htmlFor="lunchAfterSlot">Lunch after lecture #</Label>
                <Input
                  id="lunchAfterSlot"
                  type="number"
                  min={1}
                  max={config.lectureCount}
                  value={config.lunchAfterSlot}
                  onChange={(e) => updateConfig({ lunchAfterSlot: Number(e.target.value) })}
                />
              </div>
              <div>
                <Label htmlFor="lunchDuration">Lunch duration (min)</Label>
                <Input
                  id="lunchDuration"
                  type="number"
                  min={15}
                  max={90}
                  value={config.lunchDuration}
                  onChange={(e) => updateConfig({ lunchDuration: Number(e.target.value) })}
                />
              </div>
            </div>
          </div>
        )}

        {step === 3 && (
          <div className="space-y-4">
            <h3 className="font-medium">Preview ({generatedSlots.length} slots)</h3>
            <SlotTimeline slots={previewSlots} />
            <p className="text-sm text-muted-foreground">
              This will create {generatedSlots.length} slot definitions.
              Review the timeline above and click "Create All" to confirm.
            </p>
          </div>
        )}

        <DialogFooter className="flex justify-between">
          <div>
            {step > 1 && (
              <Button variant="outline" onClick={() => setStep((s) => s - 1)}>
                Back
              </Button>
            )}
          </div>
          <div className="flex gap-2">
            <Button variant="ghost" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            {step < 3 ? (
              <Button onClick={() => setStep((s) => s + 1)}>Next</Button>
            ) : (
              <Button
                onClick={handleSubmit}
                disabled={bulkCreate.isPending}
              >
                {bulkCreate.isPending ? 'Creating...' : 'Create All'}
              </Button>
            )}
          </div>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function timeToMinutes(time: string): number {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}
```

### 12.4 TanStack Query Hooks (Calendar)

```typescript
// src/features/scheduling-config/calendar/hooks.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { calendarApi } from '@/api/calendarApi';
import { queryKeys } from '@/api/queryKeys';
import { useToastStore } from '@/stores/toastStore';
import type {
  CalendarListParams,
  CalendarCreateRequest,
  CalendarUpdateRequest,
  HolidayCreateRequest,
  ExamWindowCreateRequest,
  SpecialPeriodCreateRequest,
} from '@/types/scheduling-config';

// ─── Calendar CRUD ───────────────────────────────────────────────────────────

export function useCalendars(params: CalendarListParams) {
  return useQuery({
    queryKey: queryKeys.calendars.list(params),
    queryFn: () => calendarApi.getAll(params),
    staleTime: 30_000,
    placeholderData: (previousData) => previousData,
  });
}

export function useCalendar(id: string | undefined) {
  return useQuery({
    queryKey: queryKeys.calendars.detail(id!),
    queryFn: () => calendarApi.getById(id!),
    enabled: Boolean(id),
    staleTime: 60_000,
  });
}

export function useCreateCalendar() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data: CalendarCreateRequest) => calendarApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.all });
      addToast({ type: 'success', message: 'Academic calendar created successfully' });
    },
  });
}

export function useUpdateCalendar(id: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data: CalendarUpdateRequest) => calendarApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.detail(id) });
      addToast({ type: 'success', message: 'Calendar updated successfully' });
    },
  });
}

export function useDeleteCalendar() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (id: string) => calendarApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.all });
      addToast({ type: 'success', message: 'Calendar deleted successfully' });
    },
  });
}

// ─── Holidays ────────────────────────────────────────────────────────────────

export function useHolidays(calendarId: string | undefined) {
  return useQuery({
    queryKey: queryKeys.calendars.holidays(calendarId!),
    queryFn: () => calendarApi.getHolidays(calendarId!),
    enabled: Boolean(calendarId),
    staleTime: 30_000,
  });
}

export function useCreateHoliday(calendarId: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data: HolidayCreateRequest) => calendarApi.createHoliday(calendarId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.holidays(calendarId) });
      addToast({ type: 'success', message: 'Holiday added successfully' });
    },
  });
}

export function useDeleteHoliday(calendarId: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (holidayId: string) => calendarApi.deleteHoliday(calendarId, holidayId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.holidays(calendarId) });
      addToast({ type: 'success', message: 'Holiday removed' });
    },
  });
}

// ─── Exam Windows ────────────────────────────────────────────────────────────

export function useExamWindows(calendarId: string | undefined) {
  return useQuery({
    queryKey: queryKeys.calendars.examWindows(calendarId!),
    queryFn: () => calendarApi.getExamWindows(calendarId!),
    enabled: Boolean(calendarId),
    staleTime: 30_000,
  });
}

export function useCreateExamWindow(calendarId: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data: ExamWindowCreateRequest) => calendarApi.createExamWindow(calendarId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.examWindows(calendarId) });
      addToast({ type: 'success', message: 'Exam window added successfully' });
    },
  });
}

export function useDeleteExamWindow(calendarId: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (windowId: string) => calendarApi.deleteExamWindow(calendarId, windowId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.examWindows(calendarId) });
      addToast({ type: 'success', message: 'Exam window removed' });
    },
  });
}

// ─── Special Periods ─────────────────────────────────────────────────────────

export function useSpecialPeriods(calendarId: string | undefined) {
  return useQuery({
    queryKey: queryKeys.calendars.specialPeriods(calendarId!),
    queryFn: () => calendarApi.getSpecialPeriods(calendarId!),
    enabled: Boolean(calendarId),
    staleTime: 30_000,
  });
}

export function useCreateSpecialPeriod(calendarId: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data: SpecialPeriodCreateRequest) =>
      calendarApi.createSpecialPeriod(calendarId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.specialPeriods(calendarId) });
      addToast({ type: 'success', message: 'Special period added successfully' });
    },
  });
}

export function useDeleteSpecialPeriod(calendarId: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (periodId: string) => calendarApi.deleteSpecialPeriod(calendarId, periodId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.specialPeriods(calendarId) });
      addToast({ type: 'success', message: 'Special period removed' });
    },
  });
}

// ─── Impact Analysis ─────────────────────────────────────────────────────────

export function useCalendarImpact(calendarId: string, date: string | null) {
  return useQuery({
    queryKey: queryKeys.calendars.impact(calendarId, date ?? ''),
    queryFn: () => calendarApi.getImpactAnalysis(calendarId, date!),
    enabled: Boolean(calendarId) && Boolean(date),
    staleTime: 0, // Always fresh
  });
}
```

### 12.5 TanStack Query Hooks (Grid)

```typescript
// src/features/scheduling-config/grid/hooks.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { gridApi } from '@/api/gridApi';
import { queryKeys } from '@/api/queryKeys';
import { useToastStore } from '@/stores/toastStore';
import type {
  GridListParams,
  GridCreateRequest,
  GridUpdateRequest,
  SlotCreateRequest,
  BulkSlotCreateRequest,
  WorkingDaysUpdateRequest,
} from '@/types/scheduling-config';

// ─── Grid CRUD ───────────────────────────────────────────────────────────────

export function useGrids(params: GridListParams) {
  return useQuery({
    queryKey: queryKeys.grids.list(params),
    queryFn: () => gridApi.getAll(params),
    staleTime: 30_000,
    placeholderData: (previousData) => previousData,
  });
}

export function useGrid(id: string | undefined) {
  return useQuery({
    queryKey: queryKeys.grids.detail(id!),
    queryFn: () => gridApi.getById(id!),
    enabled: Boolean(id),
    staleTime: 60_000,
  });
}

export function useCreateGrid() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data: GridCreateRequest) => gridApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.all });
      addToast({ type: 'success', message: 'Time-slot grid created successfully' });
    },
  });
}

export function useUpdateGrid(id: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data: GridUpdateRequest) => gridApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.detail(id) });
      addToast({ type: 'success', message: 'Grid updated successfully' });
    },
  });
}

export function useDeleteGrid() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (id: string) => gridApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.all });
      addToast({ type: 'success', message: 'Grid deleted successfully' });
    },
  });
}

export function useActivateGrid(id: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: () => gridApi.activate(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.detail(id) });
      queryClient.invalidateQueries({
        queryKey: queryKeys.grids.active(data.campusId),
      });
      addToast({ type: 'success', message: 'Grid activated successfully' });
    },
  });
}

// ─── Slots ───────────────────────────────────────────────────────────────────

export function useSlots(gridId: string | undefined) {
  return useQuery({
    queryKey: queryKeys.grids.slots(gridId!),
    queryFn: () => gridApi.getSlots(gridId!),
    enabled: Boolean(gridId),
    staleTime: 30_000,
  });
}

export function useCreateSlot(gridId: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data: SlotCreateRequest) => gridApi.createSlot(gridId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.slots(gridId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.detail(gridId) });
      addToast({ type: 'success', message: 'Slot added' });
    },
  });
}

export function useBulkCreateSlots(gridId: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data: BulkSlotCreateRequest) => gridApi.bulkCreateSlots(gridId, data),
    onSuccess: (slots) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.slots(gridId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.detail(gridId) });
      addToast({ type: 'success', message: `${slots.length} slots created via Quick Setup` });
    },
  });
}

export function useDeleteSlot(gridId: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (slotId: string) => gridApi.deleteSlot(gridId, slotId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.slots(gridId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.detail(gridId) });
      addToast({ type: 'success', message: 'Slot removed' });
    },
  });
}

// ─── Working Days ────────────────────────────────────────────────────────────

export function useWorkingDays(gridId: string | undefined) {
  return useQuery({
    queryKey: queryKeys.grids.workingDays(gridId!),
    queryFn: () => gridApi.getWorkingDays(gridId!),
    enabled: Boolean(gridId),
    staleTime: 60_000,
  });
}

export function useUpdateWorkingDays(gridId: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data: WorkingDaysUpdateRequest) => gridApi.updateWorkingDays(gridId, data),
    onMutate: async (newData) => {
      // Optimistic update
      await queryClient.cancelQueries({ queryKey: queryKeys.grids.workingDays(gridId) });
      const previous = queryClient.getQueryData(queryKeys.grids.workingDays(gridId));
      queryClient.setQueryData(queryKeys.grids.workingDays(gridId), (old: any) => ({
        ...old,
        days: newData.days.map((d, i) => ({
          ...old?.days?.[i],
          ...d,
        })),
      }));
      return { previous };
    },
    onError: (_err, _variables, context) => {
      // Rollback on error
      if (context?.previous) {
        queryClient.setQueryData(queryKeys.grids.workingDays(gridId), context.previous);
      }
      addToast({ type: 'error', message: 'Failed to update working days' });
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.workingDays(gridId) });
    },
  });
}

// ─── Active Grid ─────────────────────────────────────────────────────────────

export function useActiveGrid(campusId: string | undefined) {
  return useQuery({
    queryKey: queryKeys.grids.active(campusId!),
    queryFn: () => gridApi.getActiveGrid(campusId!),
    enabled: Boolean(campusId),
    staleTime: 60_000,
  });
}
```

---

## 13. Open Questions

| # | Question | Impact | Proposed Default |
|---|----------|--------|-----------------|
| 1 | Should the CalendarTimeline be clickable (click date → add holiday) or view-only? | UX complexity | View-only in Phase 1; interactive in Phase 2 |
| 2 | Should Quick Setup support saved templates (e.g., "Standard 8-period day")? | Feature scope | Defer; allow configuring each time (templates in Phase 2) |
| 3 | Should working day changes show a warning if published sessions exist on the removed day? | API dependency | Yes, if the API provides this info; otherwise show a generic warning |
| 4 | Should sub-resource forms (Holiday, ExamWindow, SpecialPeriod) be full pages or dialog modals? | UX pattern | Dialog modals (simpler context, fewer navigations) |
| 5 | Should the SlotTimeline support drag-to-resize for adjusting slot durations? | Implementation complexity | Defer to Phase 2; use form-based editing in Phase 1 |
| 6 | Should grids be deletable when active? | Business logic | No — API should reject; frontend hides Delete action on active grids |

---

## 14. Implementation Phases

| Phase | Scope | Estimate |
|-------|-------|----------|
| Phase 1 (Core) | Calendar list/create/edit + detail page with tabs structure | 2 days |
| Phase 2 (Sub-resources) | Holiday, Exam Window, Special Period forms + CalendarTimeline | 2 days |
| Phase 3 (Impact) | Impact analysis integration + ImpactWarningBanner | 0.5 days |
| Phase 4 (Grid Core) | Grid list/create/edit + detail page + SlotTimeline | 2 days |
| Phase 5 (Slots) | SlotForm + QuickSetupWizard + bulk creation | 1.5 days |
| Phase 6 (Working Days + Activation) | WorkingDayToggles (auto-save) + GridActivationDialog | 1 day |
| Phase 7 (Polish) | Responsive layout, accessibility audit, loading skeletons | 1 day |
| Phase 8 (Testing) | Component + integration tests (MSW) | 2 days |

**Total Estimated Effort:** 12 days

---

*End of Design Document*
