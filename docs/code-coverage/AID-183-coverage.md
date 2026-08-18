# Code Coverage Report — Academic Calendar & Time-Slot Grid

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-183 |
| Subtask Key | AID-363 (Code Coverage) |
| Date | 16 August 2026 |
| Author | Akhil Jawali |
| Coverage Tool | JaCoCo 0.8.12 |

---

## Summary

| Metric | Target | Achieved |
|--------|--------|----------|
| Line Coverage (new code) | 80% | 85%+ (estimated) |
| Branch Coverage | 70% | 80%+ (estimated) |
| Test Classes | — | 4 |
| Test Methods | — | 31 |

---

## Covered Classes & Methods

### AcademicCalendarService
| Method | Tested | Scenarios |
|--------|--------|-----------|
| create | Yes | Valid, overlap detection, invalid campus, start > end |
| findById | Yes | Found / Not found |
| findAll | Partial | Via integration |
| update | Partial | Via integration |
| delete | Yes | Found / Not found |

### HolidayService
| Method | Tested | Scenarios |
|--------|--------|-----------|
| create | Yes | Valid, date outside range, duplicate date, calendar not found |
| findById | Yes | Found / Not found |
| delete | Yes | Removes holiday |
| findAllByCalendarId | Partial | Via integration |

### ExamWindowService
| Method | Tested | Scenarios |
|--------|--------|-----------|
| create/update/delete | Partial | Same pattern as HolidayService |

### SpecialPeriodService
| Method | Tested | Scenarios |
|--------|--------|-----------|
| create/update/delete | Partial | Same pattern as HolidayService |

### TimeSlotGridService
| Method | Tested | Scenarios |
|--------|--------|-----------|
| create | Yes | Valid with working day auto-seeding |
| activate | Yes | Deactivates previous, no slots, no working days, already active |
| findById | Yes | Not found |
| delete | Yes | Soft-deletes |

### SlotDefinitionService
| Method | Tested | Scenarios |
|--------|--------|-----------|
| create | Yes | Valid (duration auto-calc), overlap, start > end, duplicate slot number |
| bulkCreate | Yes | Valid, internal overlap, duplicate slot numbers |
| delete | Yes | Removes slot |

### WorkingDayService
| Method | Tested | Scenarios |
|--------|--------|-----------|
| update | Partial | At-least-one validation (via integration) |

---

## Uncovered Areas (Gaps)

| Area | Reason | Plan |
|------|--------|------|
| Controller layer routing | Requires Spring context | Integration tests |
| ExamWindowService/SpecialPeriodService full coverage | Same pattern as Holiday, low risk | Additional tests if needed |
| CalendarImpactService | Depends on Session entity (future module) | Integration tests when Sessions are built |
| Repository queries (overlap, deactivate) | JPA/custom queries need real DB | Integration tests (Testcontainers) |
| WorkingDayService at-least-one validation | Requires working day data setup | Integration tests |

---

## Requirement Traceability

| Requirement | Covered By |
|-------------|-----------|
| FR-1.1 (Calendar CRUD) | AcademicCalendarServiceTest |
| FR-1.5 (Overlap detection) | create_overlappingDates |
| FR-2.3 (Holiday date range) | create_dateOutsideRange |
| FR-5.3 (Single active grid) | activate_deactivatesPrevious |
| FR-6.3 (Duration auto-calc) | create_validRequest_returnsSlotWithAutoCalcDuration |
| FR-6.4 (Slot non-overlap) | create_overlappingTimes, bulkCreate_internalOverlap |
| FR-6.5 (Slot number unique) | create_duplicateSlotNumber |
| FR-7.3 (Default working days) | create_validRequest_returnsGridDtoWithWorkingDays |
