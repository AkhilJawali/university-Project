wha# Unit Test Results — Master Data: Campus & Department Hierarchy

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-179 |
| Subtask Key | AID-305 (Unit Tests & Integration Tests) |
| Story URL | https://akhiljawali.atlassian.net/browse/AID-179 |
| Date | 13 August 2026 |
| Author | Akhil Jawali |

---

## Summary

| Metric | Value |
|--------|-------|
| Total Test Classes | 6 |
| Total Test Methods | 39 |
| Framework | JUnit 5 + Mockito |
| Assertion Library | AssertJ |

---

## Test Classes & Results

### 1. CampusServiceTest (10 tests)

| # | Test Method | Scenario | Expected Result | Status |
|---|-------------|----------|-----------------|--------|
| 1 | `findAll_returnsPagedResults` | Valid pageable request | Returns paged CampusDto list | PASS |
| 2 | `findById_existingCampus_returnsCampusDto` | Campus exists and not deleted | Returns correct CampusDto | PASS |
| 3 | `findById_nonExistentCampus_throwsEntityNotFoundException` | ID not found | Throws EntityNotFoundException | PASS |
| 4 | `create_validRequest_returnsCampusDto` | Unique code, valid payload | Returns CampusDto, persists entity | PASS |
| 5 | `create_duplicateCode_throwsConflictException` | Code already exists | Throws ConflictException, no save | PASS |
| 6 | `update_existingCampus_returnsUpdatedDto` | Valid update on existing campus | Updates and returns CampusDto | PASS |
| 7 | `update_nonExistentCampus_throwsEntityNotFoundException` | ID not found | Throws EntityNotFoundException | PASS |
| 8 | `update_duplicateCodeOnDifferentCampus_throwsConflictException` | Code conflicts with another campus | Throws ConflictException | PASS |
| 9 | `delete_campusWithNoChildren_softDeletes` | No active departments | Sets deletedAt, saves | PASS |
| 10 | `delete_campusWithActiveDepartments_throwsConflictException` | 3 active departments exist | Throws ConflictException, no save | PASS |

---

### 2. DepartmentServiceTest (6 tests)

| # | Test Method | Scenario | Expected Result | Status |
|---|-------------|----------|-----------------|--------|
| 1 | `findById_existingDepartment_returnsDepartmentDto` | Department exists | Returns correct DepartmentDto | PASS |
| 2 | `findById_nonExistent_throwsEntityNotFoundException` | ID not found | Throws EntityNotFoundException | PASS |
| 3 | `create_validRequest_returnsDepartmentDto` | Valid campus FK, unique code | Returns DepartmentDto | PASS |
| 4 | `create_invalidCampusId_throwsValidationException` | Campus ID doesn't exist | Throws ValidationException | PASS |
| 5 | `create_duplicateCodeWithinCampus_throwsConflictException` | Code exists in same campus | Throws ConflictException | PASS |
| 6 | `delete_departmentWithNoChildren_softDeletes` | No active programs | Sets deletedAt | PASS |
| 7 | `delete_departmentWithActivePrograms_throwsConflictException` | 5 active programs exist | Throws ConflictException | PASS |

---

### 3. ProgramServiceTest (5 tests)

| # | Test Method | Scenario | Expected Result | Status |
|---|-------------|----------|-----------------|--------|
| 1 | `findById_existingProgram_returnsProgramDto` | Program exists | Returns ProgramDto with correct fields | PASS |
| 2 | `create_validRequest_returnsProgramDto` | Valid department FK, unique code | Returns ProgramDto | PASS |
| 3 | `create_invalidDepartmentId_throwsValidationException` | Department doesn't exist | Throws ValidationException | PASS |
| 4 | `create_duplicateCodeWithinDepartment_throwsConflictException` | Code exists in same dept | Throws ConflictException | PASS |
| 5 | `delete_programWithNoChildren_softDeletes` | No active batches | Soft-deletes | PASS |
| 6 | `delete_programWithActiveBatches_throwsConflictException` | 4 active batches exist | Throws ConflictException | PASS |

---

### 4. BatchServiceTest (5 tests)

| # | Test Method | Scenario | Expected Result | Status |
|---|-------------|----------|-----------------|--------|
| 1 | `findById_existingBatch_returnsBatchDto` | Batch exists | Returns BatchDto | PASS |
| 2 | `findById_nonExistent_throwsEntityNotFoundException` | ID not found | Throws EntityNotFoundException | PASS |
| 3 | `create_validRequest_returnsBatchDto` | Valid program FK | Returns BatchDto | PASS |
| 4 | `create_invalidProgramId_throwsValidationException` | Program doesn't exist | Throws ValidationException | PASS |
| 5 | `delete_batchWithNoChildren_softDeletes` | No active sections | Soft-deletes | PASS |
| 6 | `delete_batchWithActiveSections_throwsConflictException` | 2 active sections exist | Throws ConflictException | PASS |

---

### 5. SectionServiceTest (7 tests)

| # | Test Method | Scenario | Expected Result | Status |
|---|-------------|----------|-----------------|--------|
| 1 | `findById_existingSection_returnsSectionDto` | Section exists | Returns SectionDto | PASS |
| 2 | `findById_nonExistent_throwsEntityNotFoundException` | ID not found | Throws EntityNotFoundException | PASS |
| 3 | `create_validRequest_returnsSectionDtoWithNoWarnings` | Valid batch FK, under strength | Returns DTO, empty warnings | PASS |
| 4 | `create_exceedsBatchStrength_returnsSectionDtoWithWarning` | Total exceeds batch strength | Returns DTO + warning message | PASS |
| 5 | `create_invalidBatchId_throwsValidationException` | Batch doesn't exist | Throws ValidationException | PASS |
| 6 | `create_duplicateNameInBatch_throwsConflictException` | Name exists in same batch | Throws ConflictException | PASS |
| 7 | `delete_existingSection_softDeletes` | Section exists | Soft-deletes | PASS |
| 8 | `delete_nonExistentSection_throwsEntityNotFoundException` | ID not found | Throws EntityNotFoundException | PASS |

---

### 6. GlobalExceptionHandlerTest (6 tests)

| # | Test Method | Scenario | Expected Result | Status |
|---|-------------|----------|-----------------|--------|
| 1 | `handleValidationException_returns400WithFieldDetail` | ValidationException thrown | HTTP 400, field-level detail | PASS |
| 2 | `handleNotFound_returns404` | EntityNotFoundException thrown | HTTP 404 | PASS |
| 3 | `handleConflict_returns409` | ConflictException thrown | HTTP 409 | PASS |
| 4 | `handleAccessDenied_returns403` | AccessDeniedException thrown | HTTP 403, "Access denied" | PASS |
| 5 | `handleGeneral_returns500WithGenericMessage` | Unexpected RuntimeException | HTTP 500, no internals exposed | PASS |
| 6 | `errorResponse_includesPathAndTimestamp` | Any error | Response includes path + timestamp | PASS |

---

## Coverage by Requirement

| Requirement | Test Coverage |
|-------------|--------------|
| FR-1.1 (Campus CRUD) | CampusServiceTest: create, findById, update, delete |
| FR-1.3 (Unique code) | CampusServiceTest: create_duplicateCode, update_duplicateCode |
| FR-1.5 (Delete with children) | CampusServiceTest: delete_campusWithActiveDepartments |
| FR-2.3 (Department FK validation) | DepartmentServiceTest: create_invalidCampusId |
| FR-2.4 (Scoped uniqueness) | DepartmentServiceTest: create_duplicateCodeWithinCampus |
| FR-2.5 (Delete with children) | DepartmentServiceTest: delete_departmentWithActivePrograms |
| FR-3.3 (Program FK validation) | ProgramServiceTest: create_invalidDepartmentId |
| FR-3.4 (Scoped uniqueness) | ProgramServiceTest: create_duplicateCodeWithinDepartment |
| FR-3.5 (Delete with children) | ProgramServiceTest: delete_programWithActiveBatches |
| FR-4.3 (Batch FK validation) | BatchServiceTest: create_invalidProgramId |
| FR-4.5 (Delete with children) | BatchServiceTest: delete_batchWithActiveSections |
| FR-5.3 (Section FK validation) | SectionServiceTest: create_invalidBatchId |
| FR-5.4 (Strength warning) | SectionServiceTest: create_exceedsBatchStrength |
| FR-5.5 (Name uniqueness) | SectionServiceTest: create_duplicateNameInBatch |
| FR-6.2 (400 on invalid FK) | All: *_invalidXxxId tests |
| FR-6.3 (409 on delete with children) | All: *_throwsConflictException tests |
| NFR Security (no internals) | GlobalExceptionHandlerTest: handleGeneral_returns500 |

---

## Notes

- All tests use Mockito for repository/mapper isolation (true unit tests, no DB).
- Integration tests (Testcontainers) are planned as a separate phase.
- Test naming follows: `methodName_scenario_expectedResult()`.
