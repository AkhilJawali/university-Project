# Academic Calendar & Time-Slot Grid Frontend Module

This module delivers CRUD management for academic calendars (with holidays, exam windows, special periods) and time-slot grids (with slot definitions, working day configuration, and an activation lifecycle). It follows a list-detail-form page pattern, uses TanStack Query for server state, Zustand for UI state, and Zod + React Hook Form for validation. The architecture is clean and well-structured: feature-scoped hooks isolate data fetching, schemas validate on the client before submission, and both domains share a common visual language (timelines, tables, inline forms).

Watch for: (1) the Quick Setup Wizard's slot generation algorithm has a logic bug that skips or doubles breaks under certain configs (confirmed); (2) no overlap validation exists on the frontend for time-slot boundaries within a grid (likely); (3) working day toggles fire mutations on every click without debounce (confirmed); (4) holiday date-range validation against the parent calendar bounds is missing (confirmed).

**Verdict**: NEEDS_CHANGES

---

## High-level view

The module splits cleanly into two sibling domains under `features/scheduling-config/`: calendars and grids. Each mirrors the same internal structure (pages, hooks, schemas, components) and both plug into the AdminLayout sidebar and route tree without friction. The API layers (`calendarApi.js`, `gridApi.js`) are thin wrappers around `apiClient`, delegating response extraction consistently. Query keys are well-organized with proper hierarchy for targeted invalidation.

The calendar detail page is the most complex surface: it manages three child entity types (holidays, exam windows, special periods) via tabbed inline forms, displays a proportional timeline visualization, and handles the "impact warning" flow when holiday creation conflicts with existing sessions. The grid detail page parallels this with slot management, a visual timeline, working day toggles, and a 3-step Quick Setup Wizard for bulk slot generation.

State management follows the TanStack Query + Zustand split correctly. Only UI concerns (active tab, wizard open state) live in the Zustand store; all server data flows through query hooks. The optimistic update on working days is a good pattern that keeps the toggle UI snappy.

The validation layer is thorough for field-level constraints (required, format, enums, date ordering) but misses cross-entity rules: the frontend doesn't validate that a holiday date falls within the parent calendar's start/end range, or that new slots don't overlap existing ones in the same grid. These are server-validated but create a poor UX when the user only discovers the violation on submit.

The Quick Setup Wizard contains a generation algorithm that compares `lecturesPlaced` counts against `breakAfterSlot` and `lunchAfterSlot` using equality, which means it only fires the insertion once and can misorder or skip insertions depending on config values. This needs redesign for robustness.

---

<details>
<summary>Issues (7)</summary>

1. **Quick Setup break/lunch insertion logic** — The wizard uses `lecturesPlaced === config.breakAfterSlot` equality check inside a loop that also increments slot numbers, causing the break to fire only once and potentially after the wrong lecture when `breakAfterSlot >= lunchAfterSlot`. Restructure to use threshold-based insertion with priority ordering.

2. **No debounce on working day toggle** — Each click immediately fires a mutation. Rapid toggling (user clicking multiple days in sequence) generates multiple overlapping PUT requests. Add a 300-500ms debounce or batch updates.

3. **Holiday date not validated against calendar range** — The holiday form accepts any date string without checking it falls between the parent calendar's `startDate` and `endDate`. Add a Zod `.refine()` or `superRefine()` that receives the calendar bounds as context, or add a runtime check in the submit handler.

4. **No frontend overlap detection for time slots** — Users can add slots that overlap existing ones in the same grid. The server may reject this, but the user gets no feedback until submission fails. Consider checking against existing slot times before allowing submission.

5. **Missing PropTypes across all components** — Per frontend standards, components should have PropTypes for prop validation. None of the components (`CalendarTimeline`, `ImpactWarningBanner`, `SlotTimeline`, `WorkingDayToggles`, `GridActivationDialog`, `QuickSetupWizard`) define PropTypes.

6. **GridActivationDialog and QuickSetupWizard lack focus trapping** — Both render as modal dialogs (`role="dialog" aria-modal="true"`) but neither traps keyboard focus. A user tabbing through can reach elements behind the overlay, violating WCAG dialog requirements.

7. **Calendar form `campusId` coercion mismatch** — The form's `defaultValues` sets `campusId: ''` (string), the Zod schema expects `z.number()`, and the select element uses `valueAsNumber: true` via `register()`. When the user hasn't selected a value, the empty string coerces to `NaN`, which fails Zod validation with a confusing error. Consider using `.coerce()` or a transform.

</details>

---

<details>
<summary>Details</summary>

## Quick Setup Wizard generation algorithm

The `generatedSlots` computation in `QuickSetupWizard.jsx` uses this logic:

```javascript
while (lecturesPlaced < config.lectureCount) {
  if (slotNumber > 1 && lecturesPlaced === config.breakAfterSlot) {
    // insert break
  }
  if (lecturesPlaced === config.lunchAfterSlot) {
    // insert lunch
  }
  // insert lecture
  lecturesPlaced++;
}
```

The equality comparisons (`===`) mean that break and lunch are only inserted the first time `lecturesPlaced` matches the threshold. But if `breakAfterSlot` equals `lunchAfterSlot`, both a break and a lunch are inserted before the same lecture, which produces 15-min break immediately followed by 45-min lunch with no lecture between them. More critically, if the config has `breakAfterSlot > lunchAfterSlot` (e.g., break after 4, lunch after 3), the lunch fires first and the break fires at the correct count, but if `breakAfterSlot < lunchAfterSlot`, the break fires first and the sequence works. The algorithm doesn't handle the case where both fire at the same threshold or where `breakAfterSlot >= lectureCount` (break never fires because the loop ends).

A cleaner approach: build an ordered insertion plan before the loop, or check `lecturesPlaced` against thresholds in a sorted priority list.

## Working day toggle mutation strategy

`WorkingDayToggles` calls `onUpdate({ days: updatedDays })` synchronously on every click. The parent hook (`useUpdateWorkingDays`) fires a PUT with optimistic update. If a user toggles Mon, Tue, Wed in rapid succession (within 200ms), three PUT requests fire concurrently. The optimistic update applies each one against potentially stale `previous` snapshots, and `onSettled` invalidates on each completion, causing the final state to depend on server response ordering.

The optimistic update is correct for single clicks, but for rapid toggling the pattern should debounce the `onUpdate` call or batch changes with a "Save" button instead of auto-save.

## Missing cross-entity date validation

The `holidaySchema` validates format and required fields but accepts `date: '2099-12-31'` even when the parent calendar runs from `2025-07-01` to `2025-12-15`. Similarly, `examWindowSchema` and `specialPeriodSchema` don't validate that their date ranges fall within the parent calendar bounds.

This is a UX gap rather than a security gap (the server should still reject out-of-range dates), but it means users only learn about the violation after a round-trip. Since the `CalendarDetailPage` already has the calendar's `startDate` and `endDate` in scope, passing them as bounds to the form schemas or adding a pre-submit check would catch this immediately.

## Modal focus trapping

Both `GridActivationDialog` and `QuickSetupWizard` render an overlay with `role="dialog" aria-modal="true"`, but neither implements focus trapping. When the wizard is open, a keyboard user pressing Tab can focus on elements in the page behind the overlay. The `DeleteConfirmDialog` and `ConflictErrorDialog` (imported from shared components) likely handle this via a shared dialog primitive, but the feature-local dialogs don't.

The fix: wrap dialog content in a focus-trap component (e.g., `@radix-ui/react-focus-scope` or a custom hook) and return focus to the trigger element on close.

## CalendarTimeline and SlotTimeline accessibility

Both timeline components render purely visual `<div>` blocks with no text alternative beyond a `title` attribute and an `aria-label` on the container. Screen reader users can't perceive the timeline content. Consider adding a visually-hidden summary (e.g., "6 holidays, 2 exam windows, 3 special periods spanning July to December") as supplementary text.

## API layer conventions

Both `calendarApi.js` and `gridApi.js` consistently extract `r.data` from responses, returning unwrapped payloads to the hooks. This aligns with the backend's `{ data, meta }` envelope pattern. The `delete` methods return `undefined` explicitly, preventing accidental data access on void responses.

One inconsistency: `gridApi.activate` uses `PUT` without a request body, which is fine for an action endpoint, but the API standards document suggests action endpoints use verb sub-resources (`/activate`). The path `/time-slot-grids/{id}/activate` matches this convention.

## Route completeness

All pages are reachable through the router: list, create (`/new`), detail (`/:id`), and edit (`/:id/edit`) for both calendars and grids. The AdminLayout sidebar links to both list pages. The `index` route under `/admin` falls back to `CampusListPage`, which is reasonable as a landing page but means navigating to `/admin` doesn't indicate the scheduling config section exists. This isn't a bug, just a navigation discoverability observation.

## Test coverage

Schema tests are thorough: all schemas have positive and negative cases, edge cases for boundary values, enum validation, and refinement logic. Missing from tests: component rendering tests, hook integration tests, and the Quick Setup Wizard generation logic (which is where the actual bug lives). The generation algorithm in `QuickSetupWizard` has no unit test despite being the most complex piece of logic in this module.

</details>

---

<details>
<summary>File map</summary>

| File | Description |
|------|-------------|
| `features/scheduling-config/calendar/CalendarListPage.jsx` | Paginated table of calendars with campus filter, delete with conflict handling |
| `features/scheduling-config/calendar/CalendarFormPage.jsx` | Create/edit form with Zod validation, server error mapping |
| `features/scheduling-config/calendar/CalendarDetailPage.jsx` | Tabbed detail view managing holidays, exam windows, special periods |
| `features/scheduling-config/calendar/hooks.js` | TanStack Query hooks for calendar + child entity CRUD |
| `features/scheduling-config/calendar/schemas.js` | Zod schemas for calendar, holiday, exam window, special period |
| `features/scheduling-config/calendar/components/CalendarTimeline.jsx` | Proportional timeline visualization |
| `features/scheduling-config/calendar/components/ImpactWarningBanner.jsx` | Expandable alert for session impact on holiday creation |
| `features/scheduling-config/grid/GridListPage.jsx` | Paginated grid table with activate/delete actions |
| `features/scheduling-config/grid/GridFormPage.jsx` | Create/edit form for grid metadata |
| `features/scheduling-config/grid/GridDetailPage.jsx` | Detail view with slot management, working days, activation |
| `features/scheduling-config/grid/hooks.js` | TanStack Query hooks for grid + slots + working days (includes optimistic update) |
| `features/scheduling-config/grid/schemas.js` | Zod schemas for grid, slot, working days |
| `features/scheduling-config/grid/components/SlotTimeline.jsx` | Color-coded proportional slot visualization |
| `features/scheduling-config/grid/components/QuickSetupWizard.jsx` | 3-step wizard for bulk slot generation |
| `features/scheduling-config/grid/components/WorkingDayToggles.jsx` | Toggle buttons with auto-save |
| `features/scheduling-config/grid/components/GridActivationDialog.jsx` | Confirmation dialog for grid activation |
| `api/calendarApi.js` | Calendar REST client (CRUD + holidays + exam windows + special periods) |
| `api/gridApi.js` | Grid REST client (CRUD + slots + bulk + working days + activate) |
| `api/queryKeys.js` | Centralized query key registry for calendars and grids |
| `stores/schedulingConfigStore.js` | Zustand store for calendar tab and wizard open state |
| `routes/index.jsx` | Route definitions for calendar and grid pages |
| `layouts/AdminLayout.jsx` | Sidebar layout with scheduling config navigation links |

</details>
