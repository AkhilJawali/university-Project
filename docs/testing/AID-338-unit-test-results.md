# Unit Test Results — AID-338 (Academic Calendar & Time-Slot Grid Frontend)

## Overview

| Field | Value |
|-------|-------|
| **Story Key** | AID-338 |
| **Subtask Key** | AID-369 |
| **Date** | 16 August 2026 |
| **Test Framework** | Vitest 2.1.8 |
| **Test Runner** | `npx vitest run src/features/scheduling-config` |
| **Result** | **All 36 tests PASSED** |
| **Duration** | 47.13s |

---

## Test Summary

| Test File | Tests | Passed | Failed |
|-----------|-------|--------|--------|
| `calendar/__tests__/schemas.test.js` | 18 | 18 | 0 |
| `grid/__tests__/schemas.test.js` | 18 | 18 | 0 |
| **Total** | **36** | **36** | **0** |

---

## Detailed Results

### Calendar Schemas (`calendar/__tests__/schemas.test.js`)

#### calendarSchema (8 tests)
- [PASS] should pass with valid data
- [PASS] should fail when name is missing
- [PASS] should fail when campusId is missing
- [PASS] should fail when academicYear has wrong format
- [PASS] should fail when academicYear uses slash instead of dash
- [PASS] should fail when startDate is after endDate
- [PASS] should fail when semesterType is invalid
- [PASS] should accept all valid semester types (ODD, EVEN, SUMMER)

#### holidaySchema (4 tests)
- [PASS] should pass with valid data
- [PASS] should fail when name is empty
- [PASS] should fail with invalid dayType
- [PASS] should default isRecurring to false

#### examWindowSchema (3 tests)
- [PASS] should pass with valid data
- [PASS] should fail when startDate is after endDate
- [PASS] should fail with invalid examType

#### specialPeriodSchema (3 tests)
- [PASS] should pass with valid data
- [PASS] should fail when startDate is after endDate
- [PASS] should fail with invalid periodType

---

### Grid Schemas (`grid/__tests__/schemas.test.js`)

#### gridSchema (4 tests)
- [PASS] should pass with valid data
- [PASS] should fail when name is empty
- [PASS] should fail when campusId is missing
- [PASS] should fail when effectiveFrom is empty

#### slotSchema (10 tests)
- [PASS] should pass with valid data
- [PASS] should fail when startTime is after endTime
- [PASS] should fail when times are equal
- [PASS] should fail with invalid time format (missing leading zero)
- [PASS] should fail with invalid time format (bad minutes)
- [PASS] should fail when slotNumber is 0
- [PASS] should fail when slotNumber is negative
- [PASS] should fail with invalid slotType
- [PASS] should accept all valid slot types (LECTURE, TUTORIAL, PRACTICAL, BREAK, LUNCH)
- [PASS] should accept 90-minute slots

#### workingDaysSchema (4 tests)
- [PASS] should pass with valid working days
- [PASS] should fail when no working day is set
- [PASS] should fail when fewer than 7 days are provided
- [PASS] should pass with only one working day

---

## Coverage Areas

| Category | What is Covered |
|----------|-----------------|
| Input validation | Required fields, format constraints, enum values |
| Date logic | Start-before-end refinement on all date range schemas |
| Time format | HH:mm regex validation with proper hour/minute bounds |
| Business rules | At least 1 working day, slot number >= 1, valid enums |
| Defaults | isRecurring defaults to false when omitted |

---

## Issues Found

None. All 36 tests passed on first run.

---

## Environment

- Node.js: 20.x LTS
- Vitest: 2.1.8
- Zod: runtime validation (plain JavaScript, no TypeScript)
- OS: Windows (AIDLC workspace)
