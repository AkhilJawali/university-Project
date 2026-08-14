# Unit Test Results — Master Data: Course Management

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-180 |
| Subtask Key | AID-311 (Unit Tests) |
| Date | 14 August 2026 |
| Author | Akhil Jawali |

---

## Summary

| Metric | Value |
|--------|-------|
| Total Test Classes | 1 |
| Total Test Methods | 12 |
| Framework | JUnit 5 + Mockito + AssertJ |
| All Passing | Yes |

---

## CourseServiceTest (12 tests)

| # | Test Method | Scenario | Expected Result | Status |
|---|-------------|----------|-----------------|--------|
| 1 | `findById_existingCourse_returnsCourseDto` | Course exists | Returns CourseDto | PASS |
| 2 | `findById_nonExistent_throwsEntityNotFoundException` | ID not found | Throws EntityNotFoundException | PASS |
| 3 | `create_validRequest_returnsCourseDto` | Valid payload | Creates and returns CourseDto | PASS |
| 4 | `create_duplicateCode_throwsConflictException` | Code exists | Throws ConflictException | PASS |
| 5 | `create_invalidDepartmentId_throwsValidationException` | Dept not found | Throws ValidationException | PASS |
| 6 | `create_ltpAllZero_throwsValidationException` | L=0, T=0, P=0 | Throws ValidationException | PASS |
| 7 | `create_creditMismatch_returnsWarning` | credit != L+T+P | Returns warning, still creates | PASS |
| 8 | `create_selfPrerequisite_throwsValidationException` | Course references itself | Throws ValidationException | PASS |
| 9 | `create_nonExistentPrerequisite_throwsValidationException` | Prereq ID doesn't exist | Throws ValidationException | PASS |
| 10 | `create_equipmentTagsExceedLimit_throwsValidationException` | 11 tags | Throws ValidationException | PASS |
| 11 | `create_equipmentTagUppercase_throwsValidationException` | Tag not lowercase | Throws ValidationException | PASS |
| 12 | `delete_existingCourse_softDeletes` | Course exists | Sets deletedAt, isActive=false | PASS |

---

## Requirement Coverage

| Requirement | Covered By |
|-------------|-----------|
| FR-1.1 (Course CRUD) | create, findById, delete tests |
| FR-1.3 (Unique code) | create_duplicateCode |
| FR-2.1 (L-T-P non-negative) | Jakarta Validation on DTO |
| FR-2.2 (At least one > 0) | create_ltpAllZero |
| FR-2.3 (Credit mismatch) | create_creditMismatch_returnsWarning |
| FR-3.2 (Prerequisites exist) | create_nonExistentPrerequisite |
| FR-3.3 (Circular detection) | create_selfPrerequisite |
| FR-4.3 (Tag validation) | create_equipmentTagsExceedLimit, create_equipmentTagUppercase |
| FR-5.2 (Dept FK validation) | create_invalidDepartmentId |
