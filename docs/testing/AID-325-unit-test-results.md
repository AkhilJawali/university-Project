# Unit Test Results — AID-325 (Master Data Admin Panel Frontend)

## Document Control

| Field | Value |
|-------|-------|
| Story Key | AID-325 |
| Subtask Key | AID-336 |
| Date | 15 August 2026 |
| Author | Akhil Jawali |
| Framework | Vitest 2.1.8 + React Testing Library 16.1.0 |
| Environment | jsdom |

---

## Summary

| Metric | Value |
|--------|-------|
| Test Files | 3 |
| Test Classes | 4 (campusSchema, useCampuses, useCreateCampus, DeleteConfirmDialog) |
| Test Methods | 17 |
| Passed | 17 |
| Failed | 0 |
| Skipped | 0 |

---

## Test Results

### 1. Campus Schema Validation (`schemas.test.ts`)

| # | Scenario | Expected Result | Status |
|---|----------|-----------------|--------|
| 1 | Valid campus data with all fields | Schema parses successfully | PASS |
| 2 | Missing name (empty string) | Validation error: "Campus name is required" | PASS |
| 3 | Invalid code format (lowercase) | Validation error: "must contain only uppercase" | PASS |
| 4 | Code too short (1 char) | Validation error: "at least 2 characters" | PASS |
| 5 | Code auto-uppercase transform | Code value transformed to uppercase | PASS |
| 6 | Empty timezone | Validation error on timezone field | PASS |
| 7 | Optional fields default to empty string | address, city, state default to '' | PASS |

### 2. Campus Hooks (`hooks.test.ts`)

| # | Scenario | Expected Result | Status |
|---|----------|-----------------|--------|
| 1 | useCampuses returns data on success | Query returns mocked campus data | PASS |
| 2 | useCampuses handles error state | isError is true, error is defined | PASS |
| 3 | useCreateCampus calls API and invalidates cache | campusApi.create called with correct data | PASS |
| 4 | useCreateCampus handles creation error | isError is true after rejection | PASS |

### 3. DeleteConfirmDialog Component (`DeleteConfirmDialog.test.tsx`)

| # | Scenario | Expected Result | Status |
|---|----------|-----------------|--------|
| 1 | Renders dialog when open=true | Dialog element present in DOM | PASS |
| 2 | Does not render when open=false | Container innerHTML is empty | PASS |
| 3 | Calls onConfirm when delete clicked | onConfirm called once | PASS |
| 4 | Calls onCancel when cancel clicked | onCancel called once | PASS |
| 5 | Shows loading state when isLoading=true | Delete button disabled with "Deleting..." text | PASS |
| 6 | Displays entity name in message | Entity name rendered in confirmation text | PASS |

---

## Requirement Coverage Mapping

| Requirement | Test Coverage |
|-------------|--------------|
| 1.1 — Campus CRUD | Schema validation, hook queries, delete confirmation |
| 1.2 — Hierarchical integrity | Schema code format validation ensures consistent identifiers |
| 23.1 — Component library patterns | DeleteConfirmDialog renders correctly with Radix-style patterns |
| 23.3 — State management (TanStack Query) | Hook tests verify query/mutation lifecycle |

---

## Test Execution Command

```bash
pnpm test
```

---

## Notes

- All tests are deterministic and independent (no shared mutable state)
- Tests use vi.mock for API module isolation
- QueryClient configured with `retry: false` and `gcTime: 0` for test stability
- user-event library used for realistic user interaction simulation
