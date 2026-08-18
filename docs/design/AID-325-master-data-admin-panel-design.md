# Design: Frontend — Master Data Admin Panel (Campus Hierarchy)

**Jira Reference:** AID-325
**Source Requirements:** docs/requirements/AID-325-master-data-admin-panel-requirements.md
**Application:** New React frontend (part of existing UTMS project)
**Stack:** React 18 · TypeScript · React Router v6 · Zustand · TanStack Query · Zod · Shadcn/ui · pnpm
**Generated:** 15 August 2026

---

## 1. Overview

This module delivers a web-based admin panel for managing the campus hierarchy (Campuses → Departments → Programs → Batches → Sections). It provides System Administrators with paginated list views, CRUD forms with inline Zod validation, a collapsible hierarchy tree visualization, and proper error/conflict handling — all consuming the existing backend REST APIs from AID-179.

The panel is the first feature module in the UTMS frontend and establishes patterns (API client, query hooks, form validation, error handling) that subsequent modules will reuse.

---

## 2. Architecture

### 2.1 Component Hierarchy

```
App
├── AuthProvider (future - JWT context)
├── QueryClientProvider (TanStack Query)
├── RouterProvider (React Router v6)
│   └── AdminLayout
│       ├── Sidebar (navigation)
│       ├── Breadcrumbs
│       └── <Outlet /> (page content)
│           ├── CampusListPage
│           ├── CampusFormPage
│           ├── DepartmentListPage
│           ├── DepartmentFormPage
│           ├── ProgramListPage
│           ├── ProgramFormPage
│           ├── BatchListPage
│           ├── BatchFormPage
│           ├── SectionListPage
│           ├── SectionFormPage
│           └── HierarchyTreePage
└── Toaster (global notifications)
```

### 2.2 State Management Approach

| Concern | Tool | Justification |
|---------|------|---------------|
| Server state (entity CRUD) | TanStack Query v5 | Automatic caching, refetch, optimistic updates, pagination |
| Client-only UI state | Zustand | Sidebar toggle, toast queue, breadcrumb path |
| Form state | React Hook Form + Zod | Performant form handling with schema validation |

### 2.3 API Layer Design

- **Typed Axios client** with interceptors for:
  - Attaching JWT `Authorization` header (when auth is enabled)
  - Parsing 400/409 responses into a `FieldErrors` structure
  - Redirecting on 401 to `/login`
- Each entity gets a dedicated API module (`campusApi`, `departmentApi`, etc.) exporting typed functions.

### 2.4 Routing Structure

All routes are nested under `/admin/master-data`:

| Route | Page | Description |
|-------|------|-------------|
| `/admin/master-data/campuses` | CampusListPage | Paginated campus list |
| `/admin/master-data/campuses/new` | CampusFormPage | Create campus |
| `/admin/master-data/campuses/:campusId` | CampusFormPage | Edit campus |
| `/admin/master-data/campuses/:campusId/departments` | DepartmentListPage | Departments under campus |
| `/admin/master-data/campuses/:campusId/departments/new` | DepartmentFormPage | Create department |
| `/admin/master-data/campuses/:campusId/departments/:deptId` | DepartmentFormPage | Edit department |
| `/admin/master-data/campuses/:campusId/departments/:deptId/programs` | ProgramListPage | Programs under dept |
| `/admin/master-data/campuses/:campusId/departments/:deptId/programs/new` | ProgramFormPage | Create program |
| `/admin/master-data/campuses/:campusId/departments/:deptId/programs/:progId` | ProgramFormPage | Edit program |
| `/admin/master-data/campuses/:campusId/departments/:deptId/programs/:progId/batches` | BatchListPage | Batches under program |
| `/admin/master-data/campuses/:campusId/departments/:deptId/programs/:progId/batches/new` | BatchFormPage | Create batch |
| `/admin/master-data/campuses/:campusId/departments/:deptId/programs/:progId/batches/:batchId` | BatchFormPage | Edit batch |
| `/admin/master-data/campuses/:campusId/departments/:deptId/programs/:progId/batches/:batchId/sections` | SectionListPage | Sections under batch |
| `/admin/master-data/campuses/:campusId/departments/:deptId/programs/:progId/batches/:batchId/sections/new` | SectionFormPage | Create section |
| `/admin/master-data/campuses/:campusId/departments/:deptId/programs/:progId/batches/:batchId/sections/:sectionId` | SectionFormPage | Edit section |
| `/admin/master-data/hierarchy/:campusId` | HierarchyTreePage | Tree view for campus |

---

## 3. Page & Component Design

### 3.1 CampusListPage

- **Route:** `/admin/master-data/campuses`
- **Components:** `DataTable`, `SearchInput`, `PaginationControls`, `ActionMenu`, `DeleteConfirmDialog`
- **Data Fetching:** `useCampuses(params)` — TanStack Query with pagination/search/sort params
- **User Interactions:**
  - Search (debounced 300ms) updates query params
  - Column header click toggles sort direction
  - Page/size selectors update pagination params
  - "Add Campus" button navigates to `/campuses/new`
  - Row action menu: View, Edit (navigate to form), Delete (opens confirmation dialog)

### 3.2 CampusFormPage

- **Route:** `/admin/master-data/campuses/new` or `/admin/master-data/campuses/:campusId`
- **Components:** `FormCard`, `TextField`, `SelectField`, `SubmitButton`, `FormErrorBanner`
- **Data Fetching:**
  - Edit mode: `useCampus(campusId)` to prefill the form
  - Create mode: no prefetch
- **User Interactions:**
  - Fill form fields (Code auto-uppercases on input via `onChange` transform)
  - Submit → calls `useCreateCampus()` or `useUpdateCampus()` mutation
  - On success: navigate to list + success toast
  - On 400/409: map API field errors to form fields inline
  - Cancel: navigate back to list

### 3.3 DepartmentListPage

- **Route:** `/admin/master-data/campuses/:campusId/departments`
- **Components:** `DataTable`, `SearchInput`, `PaginationControls`, `ActionMenu`, `DeleteConfirmDialog`
- **Data Fetching:** `useDepartments({ campusId, ...params })` — filters by parent campus
- **User Interactions:** Same pattern as CampusListPage; "Add Department" opens form

### 3.4 DepartmentFormPage

- **Route:** `/admin/master-data/campuses/:campusId/departments/new` or `.../:deptId`
- **Components:** `FormCard`, `TextField`, `FacultyCombobox` (optional HOD dropdown)
- **Data Fetching:** Edit mode: `useDepartment(deptId)`; faculty dropdown: `useFaculty()` for HOD selection
- **User Interactions:** Same create/edit pattern; Code field auto-uppercases

### 3.5 ProgramListPage / ProgramFormPage

- **Route:** `.../:deptId/programs` and `.../:deptId/programs/new` or `.../:progId`
- **Components:** Same `DataTable` pattern
- **Data Fetching:** `usePrograms({ departmentId })` / `useProgram(progId)`
- **Form Fields:** Name, Code, Duration Years (number), Total Semesters (number), Degree Type (select: UG/PG/PhD/Diploma)

### 3.6 BatchListPage / BatchFormPage

- **Route:** `.../:progId/batches` and `.../:progId/batches/new` or `.../:batchId`
- **Data Fetching:** `useBatches({ programId })` / `useBatch(batchId)`
- **Form Fields:** Name, Academic Year (string), Semester Number (number), Strength (number)

### 3.7 SectionListPage / SectionFormPage

- **Route:** `.../:batchId/sections` and `.../:batchId/sections/new` or `.../:sectionId`
- **Data Fetching:** `useSections({ batchId })` / `useSection(sectionId)`
- **Form Fields:** Name, Strength (number)
- **Special Behavior:** If API returns a warning that section strength exceeds batch strength, display a non-blocking warning banner above the form after successful creation (FR-4.4).

### 3.8 HierarchyTreePage

- **Route:** `/admin/master-data/hierarchy/:campusId`
- **Components:** `TreeView`, `TreeNode`, `TreeLoadingSkeleton`
- **Data Fetching:** `useCampusHierarchy(campusId)` — fetches full tree in one call
- **User Interactions:**
  - Expand/collapse nodes at each level
  - Click any node → navigate to that entity's edit/detail page
  - Optional: search/filter within tree (Phase 2)

---

## 4. API Client Layer

### 4.1 Base Client Configuration

```typescript
// src/api/client.ts
import axios, { AxiosError } from 'axios';
import type { ApiError, FieldErrors } from '@/types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
});

// Response interceptor: parse field-level errors
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      window.location.href = '/login';
      return Promise.reject(error);
    }

    if (error.response?.status === 400 || error.response?.status === 409) {
      const fieldErrors: FieldErrors = {};
      const details = error.response.data?.details;
      if (Array.isArray(details)) {
        for (const detail of details) {
          if (detail.field && detail.message) {
            fieldErrors[detail.field] = detail.message;
          }
        }
      }
      return Promise.reject({ ...error, fieldErrors });
    }

    return Promise.reject(error);
  }
);
```

### 4.2 Campus API Module (Example)

```typescript
// src/api/campusApi.ts
import { apiClient } from './client';
import type {
  Campus,
  CampusCreateRequest,
  CampusUpdateRequest,
  PaginatedResponse,
  CampusListParams,
  CampusHierarchy,
} from '@/types/master-data';

export const campusApi = {
  getAll: (params: CampusListParams): Promise<PaginatedResponse<Campus>> =>
    apiClient.get('/campuses', { params }).then((r) => r.data),

  getById: (id: string): Promise<Campus> =>
    apiClient.get(`/campuses/${id}`).then((r) => r.data),

  create: (data: CampusCreateRequest): Promise<Campus> =>
    apiClient.post('/campuses', data).then((r) => r.data),

  update: (id: string, data: CampusUpdateRequest): Promise<Campus> =>
    apiClient.put(`/campuses/${id}`, data).then((r) => r.data),

  delete: (id: string): Promise<void> =>
    apiClient.delete(`/campuses/${id}`).then(() => undefined),

  getHierarchy: (id: string): Promise<CampusHierarchy> =>
    apiClient.get(`/campuses/${id}/hierarchy`).then((r) => r.data),
};
```

### 4.3 Error Handling Pattern

```typescript
// src/types/api.ts
export interface ApiError {
  status: number;
  message: string;
  details?: Array<{ field: string; message: string }>;
}

export type FieldErrors = Record<string, string>;

export interface ApiErrorWithFields extends Error {
  fieldErrors?: FieldErrors;
  response?: { status: number; data?: ApiError };
}
```

All mutations catch errors and, if `fieldErrors` is present, map them to the form via `react-hook-form`'s `setError()` method.

---

## 5. State Management

### 5.1 Zustand Stores

```typescript
// src/stores/uiStore.ts
import { create } from 'zustand';

interface UiState {
  sidebarOpen: boolean;
  toggleSidebar: () => void;
}

export const useUiStore = create<UiState>((set) => ({
  sidebarOpen: true,
  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
}));
```

```typescript
// src/stores/toastStore.ts
import { create } from 'zustand';

interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
}

interface ToastState {
  toasts: Toast[];
  addToast: (toast: Omit<Toast, 'id'>) => void;
  removeToast: (id: string) => void;
}

export const useToastStore = create<ToastState>((set) => ({
  toasts: [],
  addToast: (toast) =>
    set((s) => ({
      toasts: [...s.toasts, { ...toast, id: crypto.randomUUID() }],
    })),
  removeToast: (id) =>
    set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) })),
}));
```

### 5.2 TanStack Query Keys

Query keys follow a hierarchical convention for automatic invalidation:

```typescript
// src/api/queryKeys.ts
export const queryKeys = {
  campuses: {
    all: ['campuses'] as const,
    list: (params: CampusListParams) => ['campuses', 'list', params] as const,
    detail: (id: string) => ['campuses', 'detail', id] as const,
    hierarchy: (id: string) => ['campuses', 'hierarchy', id] as const,
  },
  departments: {
    all: ['departments'] as const,
    list: (params: DeptListParams) => ['departments', 'list', params] as const,
    detail: (id: string) => ['departments', 'detail', id] as const,
  },
  programs: {
    all: ['programs'] as const,
    list: (params: ProgListParams) => ['programs', 'list', params] as const,
    detail: (id: string) => ['programs', 'detail', id] as const,
  },
  batches: {
    all: ['batches'] as const,
    list: (params: BatchListParams) => ['batches', 'list', params] as const,
    detail: (id: string) => ['batches', 'detail', id] as const,
  },
  sections: {
    all: ['sections'] as const,
    list: (params: SectionListParams) => ['sections', 'list', params] as const,
    detail: (id: string) => ['sections', 'detail', id] as const,
  },
} as const;
```

### 5.3 Caching Strategy

| Query | Stale Time | GC Time | Refetch On |
|-------|-----------|---------|------------|
| List queries | 30 seconds | 5 minutes | Window focus, mutation success |
| Detail queries | 60 seconds | 5 minutes | Window focus |
| Hierarchy tree | 60 seconds | 10 minutes | Manual refetch |

### 5.4 Optimistic Updates

Not applied in Phase 1. Create/Update/Delete mutations use `onSuccess` to invalidate relevant query keys, causing a refetch. This keeps the implementation simple and avoids rollback complexity. Can be revisited for frequently edited entities.

---

## 6. Form Validation (Zod)

### 6.1 Campus Schema (Full Example)

```typescript
// src/features/master-data/campus/schemas.ts
import { z } from 'zod';

export const campusSchema = z.object({
  name: z
    .string()
    .min(1, 'Campus name is required')
    .max(200, 'Campus name must be 200 characters or fewer')
    .trim(),
  code: z
    .string()
    .min(2, 'Code must be at least 2 characters')
    .max(20, 'Code must be 20 characters or fewer')
    .regex(
      /^[A-Z0-9-]+$/,
      'Code must contain only uppercase letters, numbers, and hyphens'
    )
    .transform((val) => val.toUpperCase()),
  address: z
    .string()
    .max(500, 'Address must be 500 characters or fewer')
    .optional()
    .default(''),
  city: z
    .string()
    .min(1, 'City is required')
    .max(100, 'City must be 100 characters or fewer')
    .trim(),
  state: z
    .string()
    .min(1, 'State is required')
    .max(100, 'State must be 100 characters or fewer')
    .trim(),
  timezone: z
    .string()
    .min(1, 'Timezone is required')
    .regex(/^[A-Za-z_/]+$/, 'Invalid timezone format (e.g., Asia/Kolkata)'),
});

export type CampusFormData = z.infer<typeof campusSchema>;
```

### 6.2 Other Entity Schemas

```typescript
// Department schema
export const departmentSchema = z.object({
  name: z.string().min(1, 'Name is required').max(200).trim(),
  code: z
    .string()
    .min(2)
    .max(20)
    .regex(/^[A-Z0-9-]+$/, 'Uppercase letters, numbers, hyphens only')
    .transform((val) => val.toUpperCase()),
  campusId: z.string().uuid('Invalid campus reference'),
  hodFacultyId: z.string().uuid().optional().nullable(),
});

// Program schema
export const programSchema = z.object({
  name: z.string().min(1, 'Name is required').max(200).trim(),
  code: z
    .string()
    .min(2)
    .max(20)
    .regex(/^[A-Z0-9-]+$/)
    .transform((val) => val.toUpperCase()),
  departmentId: z.string().uuid(),
  durationYears: z.coerce.number().int().min(1).max(8),
  totalSemesters: z.coerce.number().int().min(1).max(16),
  degreeType: z.enum(['UG', 'PG', 'PhD', 'Diploma']),
});

// Batch schema
export const batchSchema = z.object({
  name: z.string().min(1, 'Name is required').max(200).trim(),
  programId: z.string().uuid(),
  academicYear: z.string().min(4, 'Academic year is required').max(20),
  semesterNumber: z.coerce.number().int().min(1).max(16),
  strength: z.coerce.number().int().min(1).max(5000),
});

// Section schema
export const sectionSchema = z.object({
  name: z.string().min(1, 'Name is required').max(100).trim(),
  batchId: z.string().uuid(),
  strength: z.coerce.number().int().min(1).max(5000),
});
```

### 6.3 Validation Integration with Forms

Forms use `react-hook-form` with `@hookform/resolvers/zod`:

```typescript
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { campusSchema, CampusFormData } from './schemas';

const form = useForm<CampusFormData>({
  resolver: zodResolver(campusSchema),
  defaultValues: { name: '', code: '', city: '', state: '', timezone: '', address: '' },
});
```

### 6.4 API Error → Field Error Mapping

On mutation error, the `fieldErrors` object from the API interceptor is applied to the form:

```typescript
const onError = (error: ApiErrorWithFields) => {
  if (error.fieldErrors) {
    Object.entries(error.fieldErrors).forEach(([field, message]) => {
      form.setError(field as keyof CampusFormData, { type: 'server', message });
    });
  } else {
    toastStore.addToast({ type: 'error', message: 'An unexpected error occurred' });
  }
};
```

---

## 7. Folder Structure

```
frontend/
├── public/
├── src/
│   ├── api/
│   │   ├── client.ts             # Axios instance + interceptors
│   │   ├── queryKeys.ts          # TanStack Query key factory
│   │   ├── campusApi.ts          # Campus API functions
│   │   ├── departmentApi.ts      # Department API functions
│   │   ├── programApi.ts         # Program API functions
│   │   ├── batchApi.ts           # Batch API functions
│   │   └── sectionApi.ts         # Section API functions
│   ├── components/
│   │   ├── ui/                   # Shadcn/ui primitives (Button, Input, Dialog, etc.)
│   │   ├── DataTable.tsx         # Reusable paginated table
│   │   ├── SearchInput.tsx       # Debounced search input
│   │   ├── PaginationControls.tsx
│   │   ├── ActionMenu.tsx        # Row action dropdown (View, Edit, Delete)
│   │   ├── DeleteConfirmDialog.tsx
│   │   ├── ConflictErrorDialog.tsx
│   │   ├── FormCard.tsx          # Card wrapper for forms
│   │   ├── FormErrorBanner.tsx   # Non-field-level error display
│   │   ├── LoadingSkeleton.tsx
│   │   ├── ErrorState.tsx        # Error with retry button
│   │   ├── Breadcrumbs.tsx
│   │   └── TreeView/
│   │       ├── TreeView.tsx
│   │       └── TreeNode.tsx
│   ├── features/
│   │   └── master-data/
│   │       ├── campus/
│   │       │   ├── CampusListPage.tsx
│   │       │   ├── CampusFormPage.tsx
│   │       │   ├── hooks.ts         # useCampuses, useCampus, useCreateCampus, etc.
│   │       │   └── schemas.ts       # campusSchema
│   │       ├── department/
│   │       │   ├── DepartmentListPage.tsx
│   │       │   ├── DepartmentFormPage.tsx
│   │       │   ├── hooks.ts
│   │       │   └── schemas.ts
│   │       ├── program/
│   │       │   ├── ProgramListPage.tsx
│   │       │   ├── ProgramFormPage.tsx
│   │       │   ├── hooks.ts
│   │       │   └── schemas.ts
│   │       ├── batch/
│   │       │   ├── BatchListPage.tsx
│   │       │   ├── BatchFormPage.tsx
│   │       │   ├── hooks.ts
│   │       │   └── schemas.ts
│   │       ├── section/
│   │       │   ├── SectionListPage.tsx
│   │       │   ├── SectionFormPage.tsx
│   │       │   ├── hooks.ts
│   │       │   └── schemas.ts
│   │       └── hierarchy/
│   │           ├── HierarchyTreePage.tsx
│   │           └── hooks.ts
│   ├── hooks/
│   │   ├── useDebounce.ts
│   │   └── usePagination.ts
│   ├── layouts/
│   │   └── AdminLayout.tsx
│   ├── lib/
│   │   ├── sanitize.ts           # DOMPurify wrapper
│   │   └── utils.ts              # cn(), formatDate(), etc.
│   ├── routes/
│   │   ├── index.tsx             # createBrowserRouter config
│   │   └── masterDataRoutes.tsx  # Lazy-loaded master-data routes
│   ├── stores/
│   │   ├── uiStore.ts
│   │   └── toastStore.ts
│   └── types/
│       ├── api.ts                # ApiError, FieldErrors, PaginatedResponse
│       └── master-data.ts        # Campus, Department, Program, Batch, Section types
├── index.html
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.ts
├── postcss.config.js
├── components.json               # Shadcn/ui config
└── package.json
```

---

## 8. Security

### 8.1 XSS Prevention

- **No `dangerouslySetInnerHTML`** anywhere in the codebase. All user-provided text is rendered via JSX text nodes which auto-escape HTML.
- **DOMPurify** is used as a safety net for any content that originates from an API response and might contain user-generated text:

```typescript
// src/lib/sanitize.ts
import DOMPurify from 'dompurify';

export function sanitize(dirty: string): string {
  return DOMPurify.sanitize(dirty, { ALLOWED_TAGS: [], ALLOWED_ATTR: [] });
}
```

- All form inputs are validated via Zod with strict format allowlists (e.g., campus code: `/^[A-Z0-9-]+$/`) before submission.

### 8.2 Content Security Policy

CSP headers are configured in the Vite dev server and the production reverse proxy:

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

### 8.3 Token Storage (Future)

When JWT auth is implemented:
- Access token stored in memory (Zustand store) — never `localStorage`.
- Refresh token in an `HttpOnly`, `Secure`, `SameSite=Strict` cookie managed by the backend.
- On 401 response, the interceptor attempts a silent token refresh before redirecting to login.

### 8.4 Input Sanitization

- User input is validated client-side via Zod (allowlist patterns) and server-side by the backend.
- Displayed text from API responses is rendered via React JSX (auto-escaped). No raw HTML injection paths.

---

## 9. Non-Functional Design

### 9.1 Loading States

- **List pages:** Table body replaced with animated skeleton rows (Shadcn `Skeleton` component) during fetch.
- **Form pages (edit mode):** Form fields show skeleton placeholders until prefetch completes.
- **Hierarchy tree:** Tree container shows a vertical shimmer skeleton.
- **Mutation in progress:** Submit button disabled with a spinner icon.

### 9.2 Error Boundaries

- A top-level `ErrorBoundary` wraps the router to catch render errors and display a "Something went wrong" fallback with a reload button.
- Per-feature `ErrorState` component renders when a query enters error state, showing the error message and a "Retry" button that calls `refetch()`.

### 9.3 Responsive Design

- **Desktop-first** layout with sidebar (min-width 1024px).
- **Tablet (768px–1023px):** Sidebar collapses to icon-only rail; content takes full width.
- **Mobile (< 768px):** Sidebar hidden behind hamburger menu; table switches to card layout.
- Tailwind CSS breakpoints: `sm`, `md`, `lg`, `xl`.

### 9.4 Accessibility

- All interactive elements have ARIA labels (buttons, inputs, dialogs).
- `DataTable` uses semantic `<table>`, `<thead>`, `<tbody>`, `<th scope="col">` elements.
- Modals use `role="dialog"`, `aria-modal="true"`, and focus trapping.
- Delete confirmation dialogs auto-focus the "Cancel" button (safer default).
- Keyboard navigation: Tab through form fields, Enter to submit, Escape to close dialogs.
- Color contrast meets WCAG 2.1 AA (4.5:1 for normal text, 3:1 for large text).
- Tree view nodes are keyboard-navigable with arrow keys (aria-tree pattern).

---

## 10. Testing Strategy

### 10.1 Unit / Component Tests (React Testing Library + Vitest)

| What | Coverage Target |
|------|----------------|
| Zod schemas | 100% (all valid/invalid cases) |
| Custom hooks (useDebounce, usePagination) | 100% |
| Shared components (DataTable, SearchInput, PaginationControls) | 90%+ |
| Form pages (render, validate, submit, error mapping) | 80%+ |

### 10.2 Integration Tests (MSW — Mock Service Worker)

- Mock all backend endpoints with MSW handlers.
- Test full user flows: navigate to list → click "Add" → fill form → submit → verify redirect + toast.
- Test error scenarios: duplicate code → verify inline error; delete with children → verify conflict dialog.
- Test pagination: change page → verify different data rendered.

### 10.3 E2E Tests (Playwright — Future Phase)

- Smoke tests: login → navigate to campus list → create campus → verify in list.
- Cross-browser: Chromium, Firefox, WebKit.
- Not in scope for Phase 1 but folder structure is prepared.

### 10.4 Example Test Structure

```
frontend/
├── src/
│   └── features/master-data/campus/
│       └── __tests__/
│           ├── CampusListPage.test.tsx
│           ├── CampusFormPage.test.tsx
│           └── schemas.test.ts
├── tests/
│   ├── mocks/
│   │   └── handlers.ts            # MSW handlers
│   └── setup.ts                    # Vitest global setup
```

---

## 11. Requirement Traceability

| FR | Component(s) | Implementation Notes |
|----|-------------|---------------------|
| FR-1.1 | `CampusListPage`, `DataTable` | Columns: Name, Code, City, State, Timezone, Status |
| FR-1.2 | `SearchInput`, `useDebounce` | 300ms debounce on search param update |
| FR-1.3 | `DataTable` column headers | Sort param sent to API; visual indicator on active sort |
| FR-1.4 | `ActionMenu` | Dropdown with View, Edit, Delete actions |
| FR-1.5 | `CampusListPage` header button | Navigates to `/campuses/new` |
| FR-1.6 | `PaginationControls` | Page number, size selector (10/20/50), total count display |
| FR-2.1 | `CampusFormPage` | Fields: Name, Code, Address, City, State, Timezone |
| FR-2.2 | `campusSchema` (Zod) | Validates code format, name length, etc. |
| FR-2.3 | `useCreateCampus`, `useUpdateCampus` | POST/PUT with TanStack Query mutations |
| FR-2.4 | Mutation `onSuccess` callback | `navigate('/campuses')` + `toastStore.addToast()` |
| FR-2.5 | Interceptor + `form.setError()` | Maps API 400/409 details to form field errors |
| FR-2.6 | Code field `onChange` transform | `.toUpperCase()` applied before setting value |
| FR-3.1 | `DepartmentListPage` | Filtered by `campusId` from route params |
| FR-3.2 | `DepartmentFormPage` | Same CRUD form pattern |
| FR-3.3 | Department form fields | Name, Code, HOD Faculty (optional combobox) |
| FR-3.4 | Interceptor + `form.setError('code', ...)` | 409 → inline error on Code field |
| FR-4.1 | `ProgramFormPage` | Fields: Name, Code, Duration, Semesters, Degree Type |
| FR-4.2 | `BatchFormPage` | Fields: Name, Academic Year, Semester Number, Strength |
| FR-4.3 | `SectionFormPage` | Fields: Name, Strength |
| FR-4.4 | `SectionFormPage` warning banner | Display API warning response after successful creation |
| FR-5.1 | `HierarchyTreePage`, `TreeView` | Renders full tree from `/campuses/{id}/hierarchy` |
| FR-5.2 | `TreeNode` expand/collapse | State per node; toggles children visibility |
| FR-5.3 | `useCampusHierarchy` hook | Fetches hierarchy endpoint |
| FR-5.4 | `TreeNode` click handler | `navigate()` to entity edit route |
| FR-6.1 | `DeleteConfirmDialog` | Modal with "Are you sure?" + Cancel/Confirm buttons |
| FR-6.2 | `ConflictErrorDialog` | Displayed on 409 from delete; lists dependent entities |
| FR-6.3 | `ConflictErrorDialog` | Only "Acknowledge" button, no dismiss-on-outside-click |
| FR-7.1 | `AdminLayout`, `Sidebar` | "Master Data" → "Campus Hierarchy" menu item |
| FR-7.2 | `Breadcrumbs` | Dynamic path from route params + entity names |
| FR-7.3 | Tailwind responsive classes | Desktop-first with tablet/mobile breakpoints |
| FR-8.1 | `LoadingSkeleton` | Shown during `isLoading` state of queries |
| FR-8.2 | `ErrorState` | Friendly message + Retry button calling `refetch()` |
| FR-8.3 | Axios 401 interceptor | Redirects to `/login` |

---

## 12. Code Examples

### 12.1 TanStack Query Hook (Campus List)

```typescript
// src/features/master-data/campus/hooks.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { campusApi } from '@/api/campusApi';
import { queryKeys } from '@/api/queryKeys';
import type { CampusListParams, CampusCreateRequest } from '@/types/master-data';
import { useToastStore } from '@/stores/toastStore';

export function useCampuses(params: CampusListParams) {
  return useQuery({
    queryKey: queryKeys.campuses.list(params),
    queryFn: () => campusApi.getAll(params),
    staleTime: 30_000,
    placeholderData: (previousData) => previousData,
  });
}

export function useCampus(id: string | undefined) {
  return useQuery({
    queryKey: queryKeys.campuses.detail(id!),
    queryFn: () => campusApi.getById(id!),
    enabled: Boolean(id),
    staleTime: 60_000,
  });
}

export function useCreateCampus() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data: CampusCreateRequest) => campusApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.campuses.all });
      addToast({ type: 'success', message: 'Campus created successfully' });
    },
  });
}

export function useUpdateCampus(id: string) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data: CampusCreateRequest) => campusApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.campuses.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.campuses.detail(id) });
      addToast({ type: 'success', message: 'Campus updated successfully' });
    },
  });
}

export function useDeleteCampus() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (id: string) => campusApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.campuses.all });
      addToast({ type: 'success', message: 'Campus deleted successfully' });
    },
  });
}

export function useCampusHierarchy(campusId: string | undefined) {
  return useQuery({
    queryKey: queryKeys.campuses.hierarchy(campusId!),
    queryFn: () => campusApi.getHierarchy(campusId!),
    enabled: Boolean(campusId),
    staleTime: 60_000,
  });
}
```

### 12.2 Page Component Skeleton (CampusListPage)

```typescript
// src/features/master-data/campus/CampusListPage.tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCampuses, useDeleteCampus } from './hooks';
import { useDebounce } from '@/hooks/useDebounce';
import { DataTable } from '@/components/DataTable';
import { SearchInput } from '@/components/SearchInput';
import { PaginationControls } from '@/components/PaginationControls';
import { ActionMenu } from '@/components/ActionMenu';
import { DeleteConfirmDialog } from '@/components/DeleteConfirmDialog';
import { ConflictErrorDialog } from '@/components/ConflictErrorDialog';
import { ErrorState } from '@/components/ErrorState';
import { LoadingSkeleton } from '@/components/LoadingSkeleton';
import { Button } from '@/components/ui/button';
import type { Campus } from '@/types/master-data';
import type { ApiErrorWithFields } from '@/types/api';

export function CampusListPage() {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [sortBy, setSortBy] = useState<string>('name');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');
  const [deleteTarget, setDeleteTarget] = useState<Campus | null>(null);
  const [conflictMessage, setConflictMessage] = useState<string | null>(null);

  const debouncedSearch = useDebounce(search, 300);

  const { data, isLoading, isError, refetch } = useCampuses({
    search: debouncedSearch,
    page,
    pageSize,
    sortBy,
    sortDir,
  });

  const deleteMutation = useDeleteCampus();

  const handleDelete = (campus: Campus) => {
    setDeleteTarget(campus);
  };

  const confirmDelete = () => {
    if (!deleteTarget) return;
    deleteMutation.mutate(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
      onError: (error: ApiErrorWithFields) => {
        setDeleteTarget(null);
        if (error.response?.status === 409) {
          setConflictMessage(
            error.response.data?.message ?? 'Cannot delete: entity has active children.'
          );
        }
      },
    });
  };

  const columns = [
    { key: 'name', header: 'Name', sortable: true },
    { key: 'code', header: 'Code', sortable: true },
    { key: 'city', header: 'City', sortable: true },
    { key: 'state', header: 'State', sortable: false },
    { key: 'timezone', header: 'Timezone', sortable: false },
    { key: 'status', header: 'Status', sortable: false },
  ];

  if (isError) {
    return <ErrorState message="Failed to load campuses" onRetry={refetch} />;
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Campuses</h1>
        <Button onClick={() => navigate('new')}>+ Add Campus</Button>
      </div>

      <SearchInput
        value={search}
        onChange={setSearch}
        placeholder="Search by name or code..."
      />

      {isLoading ? (
        <LoadingSkeleton rows={pageSize} columns={columns.length} />
      ) : (
        <>
          <DataTable
            columns={columns}
            data={data?.items ?? []}
            sortBy={sortBy}
            sortDir={sortDir}
            onSort={(col) => {
              if (col === sortBy) {
                setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
              } else {
                setSortBy(col);
                setSortDir('asc');
              }
            }}
            renderActions={(campus: Campus) => (
              <ActionMenu
                onView={() => navigate(`${campus.id}/departments`)}
                onEdit={() => navigate(`${campus.id}`)}
                onDelete={() => handleDelete(campus)}
              />
            )}
          />

          <PaginationControls
            page={page}
            pageSize={pageSize}
            totalItems={data?.totalItems ?? 0}
            onPageChange={setPage}
            onPageSizeChange={(size) => {
              setPageSize(size);
              setPage(1);
            }}
          />
        </>
      )}

      <DeleteConfirmDialog
        open={Boolean(deleteTarget)}
        entityName={deleteTarget?.name ?? ''}
        onConfirm={confirmDelete}
        onCancel={() => setDeleteTarget(null)}
        isLoading={deleteMutation.isPending}
      />

      <ConflictErrorDialog
        open={Boolean(conflictMessage)}
        message={conflictMessage ?? ''}
        onAcknowledge={() => setConflictMessage(null)}
      />
    </div>
  );
}
```

---

## 13. Open Questions

| # | Question | Impact | Proposed Default |
|---|----------|--------|-----------------|
| 1 | Should the hierarchy tree be a separate page or a collapsible panel within list views? | Routing & layout decisions | Separate page (simpler initial implementation; can add panel later) |
| 2 | Should bulk operations (CSV import, bulk delete) be in Phase 1? | Scope | Defer to Phase 2 |
| 3 | Dark mode support? | CSS configuration | Defer; use Tailwind dark mode classes in components now for future compatibility |
| 4 | Faculty dropdown data for HOD field — should it load all faculty or just those in the department? | API scope | Load all faculty with search (combobox with server-side filter) |
| 5 | Should form pages be modal dialogs or full pages? | UX pattern | Full pages (consistent navigation, better for complex forms) |

---

## 14. Implementation Phases

| Phase | Scope | Estimate |
|-------|-------|----------|
| Phase 1 (MVP) | Campus + Department CRUD, basic list/form pattern, error handling | 3 days |
| Phase 2 | Program + Batch + Section CRUD (reuse patterns) | 2 days |
| Phase 3 | Hierarchy Tree view | 1 day |
| Phase 4 | Polish: responsive layout, accessibility audit, loading skeletons | 1 day |
| Phase 5 | Testing (component + MSW integration tests) | 2 days |

**Total Estimated Effort:** 9 days

---

*End of Design Document*
