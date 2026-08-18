# Master Data Admin Panel — React Frontend + Seed Data

This change introduces a complete React frontend for the UTMS master data admin panel, covering the campus hierarchy (campus, department, program, batch, section) with CRUD operations, a hierarchy tree view, and a development seed SQL file. The approach uses TanStack Query for server state, Zustand for UI state, React Hook Form + Zod for form validation, and a feature-scoped module layout with lazy-loaded routes. The code is clean, typed, and follows the project's established patterns closely.

Watch for: incomplete route coverage (confirmed) — most list/form pages for program, batch, and section lack router entries; the `DeleteConfirmDialog` does not trap focus or handle Escape key (confirmed); the conflict-error modal pattern is duplicated verbatim across four list pages without extraction (confirmed); and the seed SQL uses hardcoded `campus_id` integers that are fragile if sequence values diverge (likely).

**Verdict**: NEEDS_CHANGES

## High-level view

The frontend is structured as a feature-based React SPA with lazy route splitting and a shared API layer. The API client maps cleanly to the Spring Boot `/api/v1` resource endpoints, types mirror the backend DTOs, and query hooks handle cache invalidation correctly on mutations. The Zustand usage is minimal and appropriate — sidebar toggle only — and the toast store is a reasonable lightweight pub/sub for cross-cutting feedback.

The routing is the most visible gap. Only campuses (list/create/edit) and the hierarchy tree have functional routes. Programs, batches, and sections have fully implemented pages, hooks, schemas, and API modules — but their routes are never registered in the router. Navigation from the hierarchy tree generates URLs that will 404.

The dialog and modal UX has accessibility deficits. The `DeleteConfirmDialog` and the inline conflict-error modals render `position: fixed` overlays but lack focus trapping, Escape key handling, and `aria-labelledby`/`aria-describedby` attributes. The delete dialog uses `autoFocus` on Cancel (good), but focus is not locked within the dialog.

The seed SQL is a useful development convenience, but it assumes auto-increment IDs start at 1. If any prior migration or test run has advanced the sequences, the foreign key references (`campus_id`, `department_id`, `program_id`, `batch_id`) will point to wrong or nonexistent rows.

<details>
<summary>Issues (8)</summary>

1. **Incomplete route registration** — Program, batch, and section list/form pages are implemented but have no entries in `routes/index.tsx`. Add routes or the hierarchy tree's `navigate()` calls will 404. [confirmed]
2. **No focus trapping in dialogs** — `DeleteConfirmDialog` and the four inline conflict-error modals render portals without trapping focus or responding to Escape key. Use Radix Dialog (already a dependency) or implement `onKeyDown` + focus-lock. [confirmed]
3. **Duplicated conflict-error modal** — The same "Cannot Delete" overlay pattern is copy-pasted in `CampusListPage`, `BatchListPage`, `ProgramListPage`, and `SectionListPage`. Extract a shared `ConflictErrorDialog` component. [confirmed]
4. **Seed SQL hardcodes sequential IDs** — `departments` reference `campus_id = 1,2` and `programs` reference `department_id = 1..4` assuming auto-increment starts at 1. Use `currval()`/subqueries or explicit ID assignment to be resilient. [likely]
5. **`useUpdateCampus(numericId!)` called unconditionally** — In `CampusFormPage`, the hook is always called even in create mode, passing `NaN` as `id`. The hook itself is safe (mutation doesn't fire until `mutate()`), but the non-null assertion on undefined is misleading and could cause issues if the hook's internals change. Same pattern in Program/Batch/Section forms. [confirmed]
6. **Stale time doesn't match guidelines** — The state-management standard specifies 5 min stale time for master data that "rarely changes during a session," but hooks use 30s. This is a minor inconsistency; 30s is more conservative but generates more network traffic. [confirmed]
7. **No global error boundary** — The app's `main.tsx` does not wrap the router in an `ErrorBoundary` component, which the frontend standards require at the app root. Unhandled render errors will crash the entire app. [confirmed]
8. **No `.gitignore` for `node_modules`** — The `code/frontend/node_modules` directory appears in the untracked file listing and would be committed if staged carelessly. Ensure a `.gitignore` at the frontend root or repo root excludes it. [confirmed]

</details>

<details>
<summary>Details</summary>

## Routing gap between implemented pages and registered routes

The router in `routes/index.tsx` registers only three page-level paths: campus list, campus form (create/edit), department list/form, and the hierarchy tree. However, the hierarchy tree's `onClick` handlers generate URLs like `/admin/master-data/departments/:deptId/programs/:programId`, `/admin/master-data/programs/:programId/batches/:batchId`, and `/admin/master-data/batches/:batchId/sections/:sectionId` — none of which have corresponding route entries. The `ProgramListPage`, `ProgramFormPage`, `BatchListPage`, `BatchFormPage`, `SectionListPage`, and `SectionFormPage` components exist and are fully functional, but unreachable via the router.

This means the app compiles and runs fine at `/admin/master-data/campuses`, but clicking any node in the hierarchy tree below the department level navigates to a blank page (no route match, no 404 handler). There's also no catch-all or "not found" route defined.

## Dialog accessibility deficits

The `DeleteConfirmDialog` component renders a manual portal-style overlay:

```tsx
<div className="fixed inset-0 z-50 flex items-center justify-center">
  <div className="fixed inset-0 bg-black/50" onClick={onCancel} />
  <div ... role="dialog" aria-modal="true">
```

This is semantically close but has gaps:
- No `aria-labelledby` linking to the "Confirm Delete" heading
- No `aria-describedby` linking to the confirmation text
- Focus is not trapped — Tab can escape to background elements
- Pressing Escape does not dismiss the dialog
- The backdrop `onClick={onCancel}` is good, but the equivalent keyboard escape path is missing

The project already has `@radix-ui/react-dialog` as a dependency (version 1.1.4 in package.json). Using it would solve all of these issues at once while reducing custom code.

The conflict-error modal (the "Cannot Delete" overlay that appears on 409 responses) has the same problems, compounded by the fact that it doesn't even have `role="dialog"` or `aria-modal="true"`.

## Repeated conflict-error overlay pattern

Four list pages (campus, program, batch, section) contain this identical ~15-line block:

```tsx
{conflictMessage && (
  <div className="fixed inset-0 z-50 flex items-center justify-center">
    <div className="fixed inset-0 bg-black/50" />
    <div className="relative bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4">
      <h3 className="text-lg font-semibold text-red-700 mb-2">Cannot Delete</h3>
      <p className="text-sm text-gray-600 mb-6">{conflictMessage}</p>
      <button onClick={() => setConflictMessage(null)} ...>Acknowledge</button>
    </div>
  </div>
)}
```

This is a textbook extraction target. The pattern is identical — only the state setter name varies. A `ConflictErrorDialog` component (or reuse of the existing `DeleteConfirmDialog` with a variant mode) would eliminate the duplication and make it easier to fix the accessibility issues in one place.

## Seed SQL fragility with sequential IDs

The seed file's `INSERT INTO utms.departments` uses literal `campus_id` values (1, 2):

```sql
('Computer Science & Engineering', 'CSE', 1, true, 'admin', 'admin'),
...
('Information Science', 'ISE', 2, true, 'admin', 'admin'),
```

If the `campuses` table's `bigserial` sequence has already advanced (from a prior failed seed run, from tests, or from the application inserting and rolling back), these IDs won't match the actual campus rows. The safer pattern is either:
- Use explicit `id` values in the `campuses` INSERT (with `OVERRIDING SYSTEM VALUE` or an `id` column in the INSERT list)
- Use subqueries: `(SELECT id FROM utms.campuses WHERE code = 'MAIN')`
- Wrap in a transaction that resets sequences first (only for dev seeds)

The same fragility cascades down: programs reference department IDs 1-4, batches reference program IDs 1-3, sections reference batch IDs 1-3.

## Hook invocation with non-null assertion on undefined

In `CampusFormPage`:

```tsx
const numericId = campusId ? Number(campusId) : undefined;
...
const updateMutation = useUpdateCampus(numericId!);
```

When `campusId` is absent (create mode), `numericId` is `undefined`, and the non-null assertion passes `undefined as number` (which becomes `NaN` at runtime). The hook creates a mutation with `batchApi.update(NaN, data)` as its `mutationFn` — it never fires because the code only calls `updateMutation.mutate()` when `isEdit` is true, but the assertion is misleading. Consider conditionally calling the hook, or accepting `undefined` in the hook signature with a guard.

## Missing Error Boundary

The frontend standards require: "Global ErrorBoundary at app root with fallback UI" and "Per-feature error boundaries for isolation." The current `main.tsx` renders:

```tsx
<React.StrictMode>
  <QueryClientProvider client={queryClient}>
    <RouterProvider router={router} />
  </QueryClientProvider>
</React.StrictMode>
```

No `ErrorBoundary` wraps the tree. A component-level rendering error (e.g., a null dereference in `HierarchyTreePage` if the API returns unexpected shape) will unmount the entire app with no recovery path.

## Test coverage scope

Tests exist for:
- `DeleteConfirmDialog` (render, callbacks, loading state) — solid
- `campusSchema` (validation rules, transforms, defaults) — solid
- `useCampuses` / `useCreateCampus` (mocked API, success/error paths)

Not tested:
- Any list page rendering or user interaction (search, pagination, delete flow)
- Form pages (submission, server-error mapping to fields, edit mode population)
- Hierarchy tree (campus selection, tree expansion, navigation)
- Program/batch/section schemas and hooks
- Toast store behavior
- API client interceptor logic (401 redirect, field error extraction)

The existing tests are well-structured and use proper mocking patterns, but coverage is minimal relative to the codebase size.

</details>

<details>
<summary>File map</summary>

| Path | What it does |
|------|------|
| `code/frontend/package.json` | React 18 SPA with TanStack Query, Zustand, React Hook Form, Zod, Radix UI, Tailwind |
| `code/frontend/vite.config.ts` | Vite dev server on :3000, proxy `/api` to :8080 |
| `code/frontend/vitest.config.ts` | Vitest with jsdom, path aliases, global test setup |
| `code/frontend/tailwind.config.ts` | Standard Tailwind config scanning `src/**/*.{ts,tsx}` |
| `code/frontend/src/main.tsx` | App entry: QueryClient + RouterProvider, no ErrorBoundary |
| `code/frontend/src/routes/index.tsx` | Lazy routes for campuses and hierarchy only |
| `code/frontend/src/api/client.ts` | Axios instance with 401 redirect and field-error enrichment |
| `code/frontend/src/api/campusApi.ts` | Campus CRUD + hierarchy endpoint |
| `code/frontend/src/api/departmentApi.ts` | Department CRUD |
| `code/frontend/src/api/programApi.ts` | Program CRUD |
| `code/frontend/src/api/batchApi.ts` | Batch CRUD |
| `code/frontend/src/api/sectionApi.ts` | Section CRUD |
| `code/frontend/src/api/queryKeys.ts` | Centralized TanStack Query key factory |
| `code/frontend/src/types/api.ts` | `ApiError`, `PaginatedResponse`, `FieldErrors` types |
| `code/frontend/src/types/master-data.ts` | All entity interfaces + request/list params |
| `code/frontend/src/stores/uiStore.ts` | Sidebar toggle (Zustand) |
| `code/frontend/src/stores/toastStore.ts` | Toast queue (Zustand) |
| `code/frontend/src/hooks/useDebounce.ts` | Generic debounce hook |
| `code/frontend/src/layouts/AdminLayout.tsx` | Sidebar + main content shell |
| `code/frontend/src/components/LoadingSkeleton.tsx` | Animated skeleton placeholder |
| `code/frontend/src/components/ErrorState.tsx` | Error illustration + retry button |
| `code/frontend/src/components/DeleteConfirmDialog.tsx` | Modal delete confirmation (no focus trap) |
| `code/frontend/src/features/master-data/campus/` | Campus list, form, hooks, schemas, tests |
| `code/frontend/src/features/master-data/department/` | Placeholder pages (not implemented) |
| `code/frontend/src/features/master-data/program/` | Program list, form, hooks, schemas |
| `code/frontend/src/features/master-data/batch/` | Batch list, form, hooks, schemas |
| `code/frontend/src/features/master-data/section/` | Section list, form, hooks, schemas |
| `code/frontend/src/features/master-data/hierarchy/` | Tree view + TreeNode component |
| `code/utms/src/main/resources/db/seed/V100__seed_campus_data.sql` | Dev seed for campuses → departments → programs → batches → sections |

</details>
