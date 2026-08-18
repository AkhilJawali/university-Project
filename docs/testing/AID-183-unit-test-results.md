# Unit Test Results — Academic Calendar & Time-Slot Grid

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-183 |
| Subtask Key | AID-362 (Unit Tests) |
| Date | 16 August 2026 |
| Author | Akhil Jawali |

---

## Summary

| Metric | Value |
|--------|-------|
| Total Test Classes | 4 |
| Total Test Methods | 31 |
| Framework | JUnit 5 + Mockito + AssertJ |
| All Passing | Yes |

---

## AcademicCalendarServiceTest (8 tests)

| # | Test Method | Scenario | Status |
|---|-------------|----------|--------|
| 1 | create_validRequest_returnsCalendarDto | Valid payload, no overlap | PASS |
| 2 | create_overlappingDates_throwsConflictException | Same campus/semester with overlap | PASS |
| 3 | create_invalidCampusId_throwsValidationException | Campus not found | PASS |
| 4 | create_startAfterEnd_throwsValidationException | Start date > end date | PASS |
| 5 | findById_existing_returnsDto | Calendar exists | PASS |
| 6 | findById_nonExistent_throwsEntityNotFoundException | ID not found | PASS |
| 7 | delete_existing_softDeletes | Sets deletedAt | PASS |
| 8 | delete_nonExistent_throwsEntityNotFoundException | ID not found | PASS |

## HolidayServiceTest (7 tests)

| # | Test Method | Scenario | Status |
|---|-------------|----------|--------|
| 1 | create_validRequest_returnsHolidayDto | Date within range | PASS |
| 2 | create_dateOutsideRange_throwsValidationException | Date before calendar start | PASS |
| 3 | create_duplicateDate_throwsConflictException | Same date in calendar | PASS |
| 4 | create_calendarNotFound_throwsEntityNotFoundException | Invalid calendar ID | PASS |
| 5 | findById_existing_returnsDto | Holiday exists | PASS |
| 6 | findById_nonExistent_throwsEntityNotFoundException | ID not found | PASS |
| 7 | delete_existing_removes | Deletes holiday | PASS |

## TimeSlotGridServiceTest (7 tests)

| # | Test Method | Scenario | Status |
|---|-------------|----------|--------|
| 1 | create_validRequest_returnsGridDtoWithWorkingDays | Auto-seeds 7 working days | PASS |
| 2 | activate_deactivatesPreviousGrid_succeeds | Previous grid set inactive | PASS |
| 3 | activate_noSlots_throwsValidationException | Grid has 0 slots | PASS |
| 4 | activate_noWorkingDays_throwsValidationException | Grid has 0 working days | PASS |
| 5 | activate_alreadyActive_returnsWithoutChange | No-op if already active | PASS |
| 6 | findById_nonExistent_throwsEntityNotFoundException | ID not found | PASS |
| 7 | delete_existing_softDeletes | Sets deletedAt | PASS |

## SlotDefinitionServiceTest (9 tests)

| # | Test Method | Scenario | Status |
|---|-------------|----------|--------|
| 1 | create_validRequest_returnsSlotWithAutoCalcDuration | Duration = end - start | PASS |
| 2 | create_overlappingTimes_throwsConflictException | Time range overlaps existing | PASS |
| 3 | create_startAfterEnd_throwsValidationException | Start time > end time | PASS |
| 4 | create_duplicateSlotNumber_throwsConflictException | Slot number exists | PASS |
| 5 | bulkCreate_validRequest_returnsAllSlots | All slots created | PASS |
| 6 | bulkCreate_internalOverlap_throwsConflictException | Two slots overlap within request | PASS |
| 7 | bulkCreate_duplicateSlotNumbers_throwsValidationException | Repeated slot number | PASS |
| 8 | create_gridNotFound_throwsEntityNotFoundException | Invalid grid ID | PASS |
| 9 | delete_existing_removes | Deletes slot | PASS |

---

## Requirement Coverage

| Requirement | Covered By |
|-------------|-----------|
| FR-1.4 (start < end) | AcademicCalendarServiceTest: create_startAfterEnd |
| FR-1.5 (no overlap) | AcademicCalendarServiceTest: create_overlappingDates |
| FR-2.3 (holiday within range) | HolidayServiceTest: create_dateOutsideRange |
| FR-5.3 (single active grid) | TimeSlotGridServiceTest: activate_deactivatesPrevious |
| FR-6.4 (slot non-overlap) | SlotDefinitionServiceTest: create_overlappingTimes |
| FR-6.3 (duration auto-calc) | SlotDefinitionServiceTest: create_validRequest_returnsSlotWithAutoCalcDuration |
| FR-6.5 (slot number unique) | SlotDefinitionServiceTest: create_duplicateSlotNumber |
| FR-7.3 (default working days) | TimeSlotGridServiceTest: create_validRequest_returnsGridDtoWithWorkingDays |
