# Unit Test Results — Master Data: Faculty Management

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-181 |
| Subtask Key | AID-317 (Unit Tests) |
| Date | 14 August 2026 |
| Author | Akhil Jawali |

---

## Summary

| Metric | Value |
|--------|-------|
| Total Test Classes | 1 |
| Total Test Methods | 14 |
| Framework | JUnit 5 + Mockito + AssertJ |
| All Passing | Yes |

---

## FacultyServiceTest (14 tests)

| # | Test Method | Scenario | Expected Result | Status |
|---|-------------|----------|-----------------|--------|
| 1 | `findById_existingFaculty_returnsFacultyDto` | Faculty exists | Returns FacultyDto with correct fields | PASS |
| 2 | `findById_nonExistent_throwsEntityNotFoundException` | ID not found | Throws EntityNotFoundException | PASS |
| 3 | `create_validRequest_returnsFacultyDto` | Valid payload | Creates and returns FacultyDto | PASS |
| 4 | `create_duplicateEmployeeId_throwsConflictException` | Employee ID exists | Throws ConflictException | PASS |
| 5 | `create_duplicateEmail_throwsConflictException` | Email exists | Throws ConflictException | PASS |
| 6 | `create_invalidDepartmentId_throwsValidationException` | Dept not found | Throws ValidationException | PASS |
| 7 | `addAvailability_validRequest_returnsAvailabilityDto` | Valid time window | Creates and returns FacultyAvailabilityDto | PASS |
| 8 | `addAvailability_overlappingTime_throwsConflictException` | Overlapping window exists | Throws ConflictException | PASS |
| 9 | `addCompetency_validCourse_succeeds` | Valid course ID | Creates competency, returns CompetencyDto | PASS |
| 10 | `addCompetency_nonExistentCourse_throwsValidationException` | Course not found | Throws ValidationException | PASS |
| 11 | `addCampusAssociation_validCampus_succeeds` | Valid campus ID | Creates association, returns CampusAssociationDto | PASS |
| 12 | `addCampusAssociation_nonExistentCampus_throwsValidationException` | Campus not found | Throws ValidationException | PASS |
| 13 | `delete_existingFaculty_softDeletes` | Faculty exists | Sets deletedAt, isActive=false | PASS |
| 14 | `delete_nonExistent_throwsEntityNotFoundException` | ID not found | Throws EntityNotFoundException | PASS |

---

## Requirement Coverage

| Requirement | Covered By |
|-------------|-----------|
| FR-1 (Faculty CRUD) | create_validRequest, findById_existingFaculty, delete_existingFaculty |
| FR-2 (Unique employee ID) | create_duplicateEmployeeId_throwsConflictException |
| FR-3 (Unique email) | create_duplicateEmail_throwsConflictException |
| FR-4 (Department FK validation) | create_invalidDepartmentId_throwsValidationException |
| FR-5 (Availability management) | addAvailability_validRequest, addAvailability_overlappingTime |
| FR-6 (Subject competencies) | addCompetency_validCourse, addCompetency_nonExistentCourse |
| FR-7 (Multi-campus associations) | addCampusAssociation_validCampus, addCampusAssociation_nonExistentCampus |
| FR-8 (Soft delete) | delete_existingFaculty_softDeletes |
| FR-9 (Not found handling) | findById_nonExistent, delete_nonExistent |
