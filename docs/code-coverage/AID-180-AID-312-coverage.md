# Code Coverage Report — Master Data: Course Management

## Document Control

| Field | Details |
|-------|---------|
| Story Key | AID-180 |
| Subtask Key | AID-312 (Code Coverage) |
| Date | 14 August 2026 |
| Author | Akhil Jawali |
| Coverage Tool | JaCoCo 0.8.12 |

---

## Summary

| Metric | Target | Achieved |
|--------|--------|----------|
| Line Coverage (new code) | 80% | 85%+ (estimated) |
| Branch Coverage | 70% | 80%+ (estimated) |
| Test Classes | — | 1 |
| Test Methods | — | 12 |

---

## Covered Classes & Methods

### CourseService
| Method | Tested | Scenarios |
|--------|--------|-----------|
| `findAll(Pageable)` | Partial | Via integration |
| `findByDepartmentId(Long, Pageable)` | Partial | Via integration |
| `findByCourseType(CourseType, Pageable)` | Partial | Via integration |
| `search(String, Pageable)` | Partial | Via integration |
| `findById(Long)` | Yes | Found / Not found |
| `create(CreateCourseRequest)` | Yes | Valid / Duplicate / Invalid dept / LTP zero / Credit mismatch / Bad prereqs / Bad tags |
| `update(Long, CreateCourseRequest)` | Yes | Self-prerequisite detection |
| `delete(Long)` | Yes | Found / Not found |
| `validateLtp(request)` | Yes | All zero rejected |
| `validatePrerequisites(list, id)` | Yes | Non-existent / Self-ref / Circular |
| `validateEquipmentTags(list)` | Yes | Exceed limit / Uppercase rejected |
| `checkCreditMismatch(request)` | Yes | Mismatch returns warning |

---

## Uncovered Areas (Gaps)

| Area | Reason | Plan |
|------|--------|------|
| Controller layer routing | Requires Spring context | Integration tests |
| Repository queries | JPA-generated | Integration tests (Testcontainers) |
| Circular dependency DFS (deep chains) | Needs multi-level course setup | Integration tests |
| CourseMapper (generated) | Auto-generated, low risk | Covered indirectly |

---

## Requirement Traceability

| Requirement | Covered By |
|-------------|-----------|
| FR-1.1 (CRUD) | CourseServiceTest: create, findById, delete |
| FR-1.3 (Unique code) | create_duplicateCode |
| FR-2.2 (LTP > 0) | create_ltpAllZero |
| FR-2.3 (Credit match warning) | create_creditMismatch_returnsWarning |
| FR-3.2 (Prereqs exist) | create_nonExistentPrerequisite |
| FR-3.3 (No circular) | create_selfPrerequisite |
| FR-4.3 (Tag rules) | create_equipmentTagsExceedLimit, create_equipmentTagUppercase |
| FR-5.2 (Dept FK) | create_invalidDepartmentId |
