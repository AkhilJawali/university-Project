# Academic Calendar and Time-Slot Grid Module

This implementation delivers CRUD management for academic calendars (with holidays, exam windows, special periods as nested sub-resources) and a time-slot grid system (with slot definitions and working day configuration). The design follows a campus-scoped ownership model where each calendar and grid belongs to a single campus. Time-slot grids have an activation lifecycle: only one grid can be active per campus at a time, enforced both at the application level and via a partial unique index. The slot overlap validation engine is the critical invariant for downstream scheduling correctness.

Watch for: N+1 query risk in `AcademicCalendarMapper` which eagerly accesses lazy collections for count computation (confirmed); the `ExamWindowService.create` date validation condition is logically dead (confirmed); the `deactivateAllForCampus` bulk update bypasses JPA auditing for `updatedBy` (confirmed); soft-delete filtering is missing from uniqueness checks in both Holiday and SlotDefinition repositories (confirmed).

**Verdict**: NEEDS_CHANGES

## High-level view

The academic calendar aggregate is cleanly modeled with proper sub-resource nesting and campus-scoped overlap prevention. Sub-resource controllers enforce parent existence before any child operation, and date range bounds validation ensures children fall within their parent calendar's range.

The time-slot grid module uses a two-phase lifecycle: grids are created dormant, populated with slot definitions, then explicitly activated. Activation atomically deactivates all other grids for the same campus. The DB backs this with a partial unique index on `(campus_id) WHERE is_active = TRUE`, making the constraint race-proof.

Slot overlap detection uses correct interval math in parameterized JPQL. The bulk-create endpoint validates both internal consistency (no overlaps within the batch) and external consistency (no conflicts with existing DB records) before persisting. However, both the Holiday and SlotDefinition uniqueness checks fail to account for soft-deleted records, creating situations where valid re-creation is blocked.

Test coverage hits the critical validation paths but skips three of the six services entirely (ExamWindow, SpecialPeriod, WorkingDay) and none of the update methods.

<details>
<summary>Issues (8)</summary>

1. **Lazy collection access in mapper triggers N+1** (confirmed) — `AcademicCalendarMapper.toDto` calls `.getHolidays().size()` on a lazy collection without a fetch join. On a page of 20 calendars, this fires 60 extra queries. Use `@EntityGraph` or COUNT subqueries.
2. **Dead date validation in ExamWindowService.create** (confirmed) — The condition `!startDate.isBefore(endDate) && !startDate.equals(endDate)` is equivalent to `startDate.isAfter(endDate)` and is unreachable for same-day windows. Same issue in `SpecialPeriodService`. Replace with `startDate.isAfter(endDate)`.
3. **deactivateAllForCampus bypasses JPA audit for updatedBy** (confirmed) — The `@Modifying` JPQL UPDATE cannot access SecurityContext. Deactivated grids retain stale `updated_by`. Pass userId as a query parameter or load-and-save.
4. **Missing calendar change impact detection** (confirmed) — Task 4.6 requires flagging sessions affected when calendar dates shrink. Not implemented. Track as a follow-up.
5. **Holiday duplicate check includes soft-deleted records** (confirmed) — `existsByCalendarIdAndDate` has no `deleted_at IS NULL` filter. A soft-deleted holiday blocks re-creation of that date.
6. **Slot number uniqueness includes soft-deleted records** (confirmed) — `existsByGridIdAndSlotNumber` has no soft-delete filter, and the DB constraint `uq_slot_definitions_grid_number` is also not partial. Fix at both layers.
7. **No tests for ExamWindowService, SpecialPeriodService, or WorkingDayService** (confirmed) — These services contain date validation and state management logic with zero test coverage.
8. **Update operations reuse Create request DTO** (confirmed) — `update(id, CreateAcademicCalendarRequest)` requires `campusId` which is silently ignored, confusing the API contract. Either add a separate `UpdateAcademicCalendarRequest` or validate the sent campusId matches.

</details>

<details>
<summary>Details</summary>

## Lazy collection access in the calendar mapper

`AcademicCalendarMapper` computes child counts via Java expressions:

```java
@Mapping(target = "holidayCount", expression = "java(calendar.getHolidays() != null ? calendar.getHolidays().size() : 0)")
```

When `findWithFilters` returns a page of calendars without a fetch join, each `.size()` call initializes the lazy proxy with a separate SELECT. Three collections times N entities per page. The session is open (method is `@Transactional(readOnly = true)`) so it won't throw `LazyInitializationException`, but the query count explodes.

Preferred fix: replace the mapper expressions with repository-level COUNT projections or a custom JPQL constructor expression. This avoids loading child entities into memory just to count them.

## Dead validation in ExamWindowService and SpecialPeriodService

```java
if (!request.getStartDate().isBefore(request.getEndDate())
        && !request.getStartDate().equals(request.getEndDate())) {
```

De Morgan's law: this fires only when `startDate.isAfter(endDate)`. The compound form obscures intent and creates an inconsistency with `AcademicCalendarService.create`, which uses `!startDate.isBefore(endDate)` — rejecting equal dates at the parent level while children allow them. This asymmetry is intentional (a semester must span multiple days; an exam window can be a single day) but should be documented with a comment.

## Audit gap in bulk deactivation

```java
UPDATE TimeSlotGrid g SET g.isActive = false, g.updatedAt = CURRENT_TIMESTAMP
WHERE g.campus.id = :campusId AND g.isActive = true AND g.deletedAt IS NULL
```

The `@Modifying` query bypasses entity lifecycle callbacks including `@LastModifiedBy`. If audit reports need "who deactivated this grid", the stored `updated_by` will be whoever last saved the entity normally, not the user who triggered the activation of a different grid.

Fix options: (a) add `g.updatedBy = :currentUser` to the JPQL and pass the authenticated username from the service; (b) load all active grids for the campus, set inactive, and save through the entity lifecycle. Option (a) is one extra parameter and one extra line.

## Soft-delete filtering gaps in uniqueness checks

`HolidayRepository.existsByCalendarIdAndDate(calendarId, date)` uses Spring Data's derived query naming, which generates `WHERE calendar_id = ? AND date = ?` without filtering soft-deleted records. If a holiday is soft-deleted (via the DELETE endpoint), the user cannot re-add a holiday for that same date.

The same issue exists in `SlotDefinitionRepository.existsByGridIdAndSlotNumber` — derived query, no soft-delete filter. Additionally, the DB unique constraint `uq_slot_definitions_grid_number` on `(grid_id, slot_number)` is not partial, so even fixing the Java check would hit a DB constraint violation. The migration needs `WHERE deleted_at IS NULL` on that unique constraint.

For holidays, the DB constraint `uq_holidays_calendar_date UNIQUE (calendar_id, date)` is also not partial. Both constraints need to become partial unique indexes.

## Confusing API contract on update

`AcademicCalendarService.update` accepts `CreateAcademicCalendarRequest` which has `@NotNull campusId`. The update method ignores this field entirely and uses the existing entity's campus for the overlap check. A client reading the request schema assumes they can change the campus — they can't. An `UpdateAcademicCalendarRequest` without `campusId` (or with it optional) would make the contract honest.

Same pattern in `TimeSlotGridService.update` — `CreateTimeSlotGridRequest.campusId` is ignored on update.

## Test coverage

Tested scenarios cover the critical invariants: overlap detection, date range validation, not-found cases, soft-delete behavior, bulk create internal validation, and grid activation guards. The existing tests are well-structured with clear fixture setup and focused assertions.

Not tested:
- `ExamWindowService` — date range validation, calendar range check, update path
- `SpecialPeriodService` — same gaps
- `WorkingDayService` — at-least-one-working-day validation, not-found on invalid day_of_week
- All update paths across both modules
- `findActiveByCampusId` (grid with no active grid for campus)
- Controller-level tests verifying `@PreAuthorize` enforcement and `@Valid` rejection

</details>

<details>
<summary>File map</summary>

**Academic Calendar module** (`com.utms.masterdata.academiccalendar`):
- `AcademicCalendar.java` — Parent entity with campus FK, date range, child collections
- `AcademicCalendarService.java` — CRUD with overlap detection
- `AcademicCalendarController.java` — REST at `/api/v1/academic-calendars`
- `AcademicCalendarRepository.java` — Parameterized JPQL overlap queries
- `AcademicCalendarMapper.java` — MapStruct with lazy-collection count expressions
- `Holiday.java` / `HolidayService.java` / `HolidayController.java` / `HolidayRepository.java` — Holiday sub-resource
- `ExamWindow.java` / `ExamWindowService.java` / `ExamWindowController.java` / `ExamWindowRepository.java` — Exam window sub-resource
- `SpecialPeriod.java` / `SpecialPeriodService.java` / `SpecialPeriodController.java` / `SpecialPeriodRepository.java` — Special period sub-resource
- Enums: `SemesterType`, `DayType`, `ExamType`, `PeriodType`

**Time-Slot Grid module** (`com.utms.masterdata.timeslot`):
- `TimeSlotGrid.java` — Entity with activation flag and campus FK
- `TimeSlotGridService.java` — Lifecycle management with activation
- `TimeSlotGridController.java` — REST at `/api/v1/time-slot-grids`
- `TimeSlotGridRepository.java` — Includes `deactivateAllForCampus`
- `SlotDefinition.java` / `SlotDefinitionService.java` / `SlotDefinitionController.java` / `SlotDefinitionRepository.java` — Slot sub-resource with overlap engine
- `WorkingDay.java` / `WorkingDayService.java` / `WorkingDayController.java` / `WorkingDayRepository.java` — Working day config
- `SlotType.java` — Enum with `isSchedulable()` helper

**Migration**: `V10__create_academic_calendar_tables.sql` — 7 tables with CHECK constraints, FK constraints, indexes

**Tests**: `AcademicCalendarServiceTest` (7), `HolidayServiceTest` (6), `TimeSlotGridServiceTest` (6), `SlotDefinitionServiceTest` (8)

</details>
