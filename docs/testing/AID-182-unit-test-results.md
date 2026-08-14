# Unit Test Results — Master Data: Room & Resource Management

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-182 |
| Subtask Key | AID-322 (Unit Tests & Integration Tests) |
| Story URL | https://akhiljawali.atlassian.net/browse/AID-182 |
| Date | 14 August 2026 |
| Author | Akhil Jawali |

---

## Summary

| Metric | Value |
|--------|-------|
| Total Test Classes | 1 |
| Total Test Methods | 14 |
| Framework | JUnit 5 + Mockito |
| Assertion Library | AssertJ |

---

## Test Classes & Results

### 1. RoomServiceTest (14 tests)

| # | Test Method | Scenario | Expected Result | Status |
|---|-------------|----------|-----------------|--------|
| 1 | `findById_existingRoom_returnsRoomDto` | Room exists and not deleted | Returns correct RoomDto with campus info | PASS |
| 2 | `findById_nonExistent_throwsEntityNotFoundException` | ID not found | Throws EntityNotFoundException | PASS |
| 3 | `create_validRequest_returnsRoomDto` | Unique code within campus, valid campus FK | Returns RoomDto, persists entity | PASS |
| 4 | `create_duplicateCodeInCampus_throwsConflictException` | Code already exists in same campus | Throws ConflictException, no save | PASS |
| 5 | `create_invalidCampusId_throwsValidationException` | Campus ID doesn't exist | Throws ValidationException | PASS |
| 6 | `create_equipmentTagsExceedLimit_throwsValidationException` | More than 20 equipment tags | Throws ValidationException | PASS |
| 7 | `create_equipmentTagUppercase_throwsValidationException` | Tag contains uppercase chars | Throws ValidationException | PASS |
| 8 | `delete_existingRoom_softDeletes` | Room exists | Sets deletedAt, isActive=false, saves | PASS |
| 9 | `delete_nonExistent_throwsEntityNotFoundException` | ID not found | Throws EntityNotFoundException | PASS |
| 10 | `approveBlock_validTransition_returnsApprovedDto` | Block status = REQUESTED | Transitions to APPROVED, sets approvedBy | PASS |
| 11 | `approveBlock_alreadyActive_throwsValidationException` | Block status = ACTIVE | Throws ValidationException (invalid transition) | PASS |
| 12 | `activateBlock_fromApproved_returnsActiveDto` | Block status = APPROVED | Transitions to ACTIVE | PASS |
| 13 | `activateBlock_fromRequested_throwsValidationException` | Block status = REQUESTED | Throws ValidationException (must be approved first) | PASS |
| 14 | `releaseBlock_fromActive_returnsReleasedDto` | Block status = ACTIVE | Transitions to RELEASED | PASS |

---

## Coverage by Requirement

| Requirement | Test Coverage |
|-------------|--------------|
| FR-1.5 (Room CRUD) | RoomServiceTest: create, findById, delete |
| FR-1.5 (Unique code within campus) | create_duplicateCodeInCampus |
| FR-1.5 (Campus FK validation) | create_invalidCampusId |
| FR-1.6 (Equipment tags) | create_equipmentTagsExceedLimit, create_equipmentTagUppercase |
| FR-6.6 (Resource block lifecycle) | approveBlock, activateBlock, releaseBlock |
| FR-6.7 (Block state transitions) | approveBlock_alreadyActive, activateBlock_fromRequested |
| FR-6.9 (Block approval gating) | approveBlock_validTransition |
| NFR (Soft delete) | delete_existingRoom_softDeletes |
| NFR (Entity not found) | findById_nonExistent, delete_nonExistent |

---

## Notes

- All tests use Mockito for repository/mapper isolation (true unit tests, no DB).
- ResourceBlockService tested alongside RoomService in same test class (instantiated manually with mocks).
- Integration tests (Testcontainers) are planned as a separate phase.
- Test naming follows: `methodName_scenario_expectedResult()`.
