# Design: Master Data — Course Management

**Jira Reference:** AID-180
**Source Requirements:** docs/requirements/AID-180-course-management-requirements.md
**Application:** Existing (Spring Boot modular monolith already scaffolded)
**Stack:** Java 17 · Spring Boot 3.x · Maven · PostgreSQL 15+ · Flyway
**Generated:** 13 August 2026

---

## 1. Overview

This design covers the course/subject master data module for the University Timetable Management System (UTMS). Courses are the primary teaching units that get scheduled — each has a defined L-T-P (Lecture-Tutorial-Practical) structure that determines how many sessions of each type need to be scheduled per week.

The module provides full CRUD for courses with L-T-P validation, prerequisite management with circular dependency detection, equipment tag handling, department association, and paginated querying with filtering. The scheduling engine depends on this data to determine session counts, room equipment requirements, and prerequisite chains.

---

## 2. Architecture

### High-Level Component Diagram

```
                         ┌─────────────────────────┐
                         │     API Gateway / JWT    │
                         └────────────┬────────────┘
                                      │
                         ┌────────────▼────────────┐
                         │   Controller Layer       │
                         │  (CourseController)      │
                         └────────────┬────────────┘
                                      │
                         ┌────────────▼────────────┐
                         │   Service Layer          │
                         │  (CourseService)         │
                         └────────────┬────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                   │
         ┌──────────▼───┐   ┌────────▼────────┐   ┌────▼──────────┐
         │  Repository   │   │  Audit Service  │   │  MapStruct    │
         │  Layer (JPA)  │   │  (same tx)      │   │  Mappers      │
         └──────────┬───┘   └────────┬────────┘   └───────────────┘
                    │                 │
         ┌──────────▼─────────────────▼───┐
         │        PostgreSQL (utms)        │
         │  courses, audit_events          │
         └────────────────────────────────┘
```

### Key Design Decisions

- **Synchronous REST** — all operations are standard request/response; no async needed for CRUD.
- **Single transaction** — each mutation (create/update/delete) writes both the entity and the audit event within one DB transaction.
- **Soft-delete** — setting `deleted_at` timestamp; active-only queries use a `@Where` clause or explicit filter.
- **RLS via Spring Security context** — department scoping injected from JWT claims, enforced in repository queries.
- **JSONB for prerequisites and equipment_tags** — leverages PostgreSQL's native JSON support with GIN indexes for efficient querying.
- **Circular dependency detection** — graph traversal at service layer before persisting prerequisite changes.

---

## 3. API Design

### 3.1 Course Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/courses` | List courses (paginated, filtered) | Authenticated | FR-6.1, FR-6.2, FR-6.3 |
| GET | `/api/v1/courses/{id}` | Get single course | Authenticated | FR-1.1 |
| POST | `/api/v1/courses` | Create course | ADMIN, HOD | FR-1.1, FR-1.2 |
| PUT | `/api/v1/courses/{id}` | Update course | ADMIN, HOD | FR-1.1 |
| DELETE | `/api/v1/courses/{id}` | Soft-delete course | ADMIN | FR-1.4, FR-1.5 |

#### Create Course Request DTO
```java
public record CreateCourseRequest(
    @NotBlank @Size(min = 2, max = 20) @Pattern(regexp = "^[A-Z0-9\\-]+$") String code,
    @NotBlank @Size(min = 1, max = 200) String name,
    @NotNull Long departmentId,
    @NotNull @Min(0) @Max(10) Integer lectureHours,
    @NotNull @Min(0) @Max(10) Integer tutorialHours,
    @NotNull @Min(0) @Max(10) Integer practicalHours,
    @NotNull @Min(1) @Max(20) Integer creditHours,
    @NotNull CourseType courseType,
    Boolean isCrossListed,
    @Size(max = 10) List<@NotBlank @Size(max = 50) @Pattern(regexp = "^[a-z][a-z0-9\\-]*$") String> equipmentTags,
    List<Long> prerequisites
) {}
```

#### Update Course Request DTO
```java
public record UpdateCourseRequest(
    @NotBlank @Size(min = 1, max = 200) String name,
    @NotNull Long departmentId,
    @NotNull @Min(0) @Max(10) Integer lectureHours,
    @NotNull @Min(0) @Max(10) Integer tutorialHours,
    @NotNull @Min(0) @Max(10) Integer practicalHours,
    @NotNull @Min(1) @Max(20) Integer creditHours,
    @NotNull CourseType courseType,
    Boolean isCrossListed,
    @Size(max = 10) List<@NotBlank @Size(max = 50) @Pattern(regexp = "^[a-z][a-z0-9\\-]*$") String> equipmentTags,
    List<Long> prerequisites
) {}
```

#### Course Response DTO
```java
public record CourseDto(
    Long id,
    String code,
    String name,
    Long departmentId,
    String departmentName,
    Integer lectureHours,
    Integer tutorialHours,
    Integer practicalHours,
    Integer creditHours,
    CourseType courseType,
    Boolean isCrossListed,
    List<String> equipmentTags,
    List<PrerequisiteCourseDto> prerequisites,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

public record PrerequisiteCourseDto(
    Long id,
    String code,
    String name
) {}
```

#### CourseType Enum
```java
public enum CourseType {
    CORE, ELECTIVE, AUDIT, LAB
}
```

#### Error Responses
- `400` — Validation failure (invalid code format, L-T-P all zero, invalid equipment tags, non-existent prerequisite, circular dependency)
- `409` — Duplicate course code (FR-1.3)
- `409` — Cannot delete course assigned to published timetable (FR-1.5)
- `401` — No/invalid JWT
- `403` — Insufficient role

#### Specific Validation Error Examples
```json
{
  "timestamp": "2026-08-13T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/courses",
  "details": [
    { "field": "lectureHours,tutorialHours,practicalHours", "message": "At least one of L, T, P must be greater than 0", "rejectedValue": "0-0-0" }
  ]
}
```

```json
{
  "timestamp": "2026-08-13T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/courses",
  "details": [
    { "field": "prerequisites", "message": "Circular prerequisite dependency detected: CS101 → CS201 → CS101", "rejectedValue": [1, 5] }
  ]
}
```

---

### 3.2 Common Query Parameters (List Endpoint)

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | 0 | Page number (0-indexed) |
| `size` | int | 20 | Page size (max 100) |
| `sort` | string | `name,asc` | Sort field and direction |
| `isActive` | boolean | true | Filter by active status |
| `includeDeleted` | boolean | false | Include soft-deleted records |
| `search` | string | — | Full-text search on name/code |
| `departmentId` | long | — | Filter by department |
| `courseType` | string | — | Filter by course type |

---

## 4. Data Model

### 4.1 Entity-Relationship Diagram

```
Department (1) ──→ (N) Course
Course (N) ──→ (N) Course (prerequisites - self-referencing via JSONB)
```

### 4.2 Table Definition

#### `utms.courses`
```sql
CREATE TABLE utms.courses (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    department_id   BIGINT NOT NULL,
    lecture_hours   INTEGER NOT NULL CHECK (lecture_hours >= 0 AND lecture_hours <= 10),
    tutorial_hours  INTEGER NOT NULL CHECK (tutorial_hours >= 0 AND tutorial_hours <= 10),
    practical_hours INTEGER NOT NULL CHECK (practical_hours >= 0 AND practical_hours <= 10),
    credit_hours    INTEGER NOT NULL CHECK (credit_hours >= 1 AND credit_hours <= 20),
    course_type     VARCHAR(20) NOT NULL CHECK (course_type IN ('CORE', 'ELECTIVE', 'AUDIT', 'LAB')),
    is_cross_listed BOOLEAN NOT NULL DEFAULT FALSE,
    equipment_tags  JSONB DEFAULT '[]'::jsonb,
    prerequisites   JSONB DEFAULT '[]'::jsonb,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_courses_departments FOREIGN KEY (department_id) REFERENCES utms.departments(id),
    CONSTRAINT uq_courses_code UNIQUE (code),
    CONSTRAINT chk_courses_ltp CHECK (lecture_hours + tutorial_hours + practical_hours > 0)
);

CREATE INDEX idx_courses_department_id ON utms.courses(department_id);
CREATE INDEX idx_courses_course_type ON utms.courses(course_type);
CREATE INDEX idx_courses_is_active ON utms.courses(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_courses_equipment_tags ON utms.courses USING GIN (equipment_tags);
CREATE INDEX idx_courses_code ON utms.courses(code);
```

### 4.3 Migration Strategy

- **Tool:** Flyway
- **Naming:** `V{N}__create_courses_table.sql`
- **Planned migrations:**
  - `V6__create_courses_table.sql`
- All migrations are reversible (DROP TABLE IF EXISTS in undo scripts)
- Schema: `utms` (set via Flyway `defaultSchema` config)

---

## 5. Service / Business Logic

### 5.1 Service Classes

| Service | Responsibility | Traces to |
|---------|---------------|-----------|
| `CourseService` | Course CRUD, L-T-P validation, uniqueness, soft-delete with published session check | FR-1.x, FR-2.x |
| `PrerequisiteValidator` | Circular dependency detection via DFS graph traversal | FR-3.x |
| `EquipmentTagValidator` | Tag format and count validation | FR-4.x |

### 5.2 Key Business Rules

#### L-T-P Validation (FR-2.2)
```java
// In CourseService.validateLtp():
if (request.lectureHours() + request.tutorialHours() + request.practicalHours() == 0) {
    throw new ValidationException("lectureHours,tutorialHours,practicalHours",
        "At least one of L, T, P must be greater than 0", "0-0-0");
}
```

#### Credit Hour Warning (FR-2.3)
```java
// In CourseService.create():
int totalContactHours = request.lectureHours() + request.tutorialHours() + request.practicalHours();
List<String> warnings = new ArrayList<>();
if (request.creditHours() != totalContactHours) {
    warnings.add(String.format(
        "Credit hours (%d) does not match L+T+P sum (%d). This is allowed but may indicate a configuration issue.",
        request.creditHours(), totalContactHours));
}
```

#### Circular Prerequisite Detection (FR-3.3)
```java
// In PrerequisiteValidator.validateNoCircularDependency():
public void validateNoCircularDependency(Long courseId, List<Long> newPrerequisites) {
    // Build prerequisite graph from existing courses
    // Run DFS from each new prerequisite to detect if courseId is reachable
    Set<Long> visited = new HashSet<>();
    for (Long prereqId : newPrerequisites) {
        if (detectCycle(prereqId, courseId, visited)) {
            throw new ValidationException("prerequisites",
                "Circular prerequisite dependency detected", newPrerequisites);
        }
    }
}

private boolean detectCycle(Long current, Long target, Set<Long> visited) {
    if (current.equals(target)) return true;
    if (!visited.add(current)) return false;
    List<Long> prereqs = courseRepository.findPrerequisiteIds(current);
    for (Long prereq : prereqs) {
        if (detectCycle(prereq, target, visited)) return true;
    }
    return false;
}
```

#### Prerequisite Existence Validation (FR-3.2)
```java
// In CourseService.validatePrerequisites():
for (Long prereqId : request.prerequisites()) {
    courseRepository.findByIdAndDeletedAtIsNull(prereqId)
        .orElseThrow(() -> new ValidationException("prerequisites",
            "Prerequisite course not found or inactive", prereqId));
}
```

#### Soft-Delete with Published Session Check (FR-1.5)
```java
// In CourseService.delete():
boolean hasPublishedSessions = sessionRepository
    .existsByCourseIdAndTimetableStatus(courseId, TimetableStatus.PUBLISHED);
if (hasPublishedSessions) {
    throw new ConflictException("Cannot delete course: it is assigned to published timetable sessions.");
}
course.setDeletedAt(LocalDateTime.now());
```

#### Equipment Tag Validation (FR-4.3)
```java
// In EquipmentTagValidator.validate():
if (tags != null && tags.size() > 10) {
    throw new ValidationException("equipmentTags", "Maximum 10 equipment tags allowed", tags.size());
}
for (String tag : tags) {
    if (!tag.matches("^[a-z][a-z0-9\\-]*$") || tag.length() > 50) {
        throw new ValidationException("equipmentTags",
            "Tags must be lowercase alphanumeric with hyphens, max 50 characters", tag);
    }
}
```

### 5.3 Validation Rules (Jakarta Validation)

All validation is applied at the controller layer via `@Valid` on request DTOs. Custom validators:

| Validator | Purpose |
|-----------|---------|
| `@UniqueCode` | Checks course code global uniqueness |
| `@ActiveParent` | Validates that referenced department exists and is not soft-deleted |
| `@ValidLtp` | Custom class-level annotation ensuring L+T+P > 0 |
| `@ValidEquipmentTags` | Tag format, count, and length validation |

### 5.4 Transaction Boundaries

- Every `create`, `update`, `delete` method is annotated `@Transactional`
- Audit event is written in the same transaction (via `AuditEventPublisher`)
- Read operations use `@Transactional(readOnly = true)`

---

## 6. Cross-cutting Concerns

### 6.1 Error Handling

`@RestControllerAdvice` in `com.utms.common.exception.GlobalExceptionHandler`:

| Exception | HTTP Status | Traces to |
|-----------|-------------|-----------|
| `MethodArgumentNotValidException` | 400 | Validation Rules |
| `ValidationException` (custom) | 400 | FR-2.2, FR-3.2, FR-3.3, FR-4.3 |
| `EntityNotFoundException` | 404 | — |
| `ConflictException` (custom) | 409 | FR-1.3, FR-1.5 |
| `AccessDeniedException` | 403 | NFR Security |
| `Exception` (catch-all) | 500 | NFR Security (no internals exposed) |

Response format:
```json
{
  "timestamp": "2026-08-13T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/courses",
  "details": [
    { "field": "departmentId", "message": "Department not found or has been deleted", "rejectedValue": 999 }
  ]
}
```

### 6.2 Security

- **Authentication:** JWT extracted via Spring Security filter; user ID and roles in claims.
- **Authorization:** `@PreAuthorize` on controller methods:
  - Write operations: `hasAnyRole('ADMIN', 'HOD')`
  - Read operations: `isAuthenticated()`
- **Data Segregation (RLS):** Repository queries include department scope derived from the authenticated user's JWT claims. HODs and Coordinators see only courses in their department.
- **Input Sanitization:** All string inputs trimmed; code field forced to uppercase; no HTML allowed in any field.

### 6.3 Audit Trail

Every mutation (create, update, soft-delete) triggers an audit event stored in the `audit_events` table within the same transaction:

```java
auditEventPublisher.record("COURSE", course.getId(), "CREATE",
    null, mapper.toDto(course), currentUserId);
```

Fields: `entity_type`, `entity_id`, `action` (CREATE/UPDATE/DELETE), `previous_value` (JSON), `new_value` (JSON), `user_id`, `timestamp`.

### 6.4 Configuration

- Database URL, credentials via environment variables
- Flyway default schema: `utms`
- Spring profiles: `local`, `dev`, `staging`, `prod`
- Pagination defaults in `application.yml`:
  ```yaml
  utms:
    pagination:
      default-size: 20
      max-size: 100
  ```

### 6.5 Logging

- SLF4J + Logback with structured JSON in deployed environments
- Log: entity type, entity ID, operation, user ID, request ID
- Never log: passwords, tokens, full request bodies with PII
- Security events (access denials, validation failures) logged at WARN level

---

## 7. Non-Functional Design

| NFR | How It's Met |
|-----|-------------|
| Performance (< 200ms single CRUD) | Direct JPA queries with indexes on FKs, code, course_type, and GIN index on equipment_tags; no N+1 |
| Security (RBAC) | `@PreAuthorize` annotations on every endpoint; role hierarchy configured in Spring Security |
| Security (RLS / Data Segregation) | Repository methods include department filter from SecurityContext |
| Input Validation (allowlist) | Jakarta Validation + `@Pattern` for code format; reject unknown fields via Jackson `FAIL_ON_UNKNOWN_PROPERTIES` |
| Parameterized Queries | Spring Data JPA method queries and `@Query` with named parameters — no string concatenation |
| Audit (same transaction) | Audit events persisted within the `@Transactional` boundary of the mutation |

---

## 8. Testing Strategy

### 8.1 Unit Tests (JUnit 5 + Mockito)

| Layer | What to Test |
|-------|-------------|
| Service | L-T-P validation (reject 0-0-0), credit hour warning, soft-delete with session check, department FK validation |
| PrerequisiteValidator | Circular dependency detection (A→B→A), transitive cycles (A→B→C→A), valid chains, self-reference |
| EquipmentTagValidator | Max count, format validation, length limits |
| Mapper | DTO ↔ Entity mapping correctness |

Naming: `methodName_scenario_expectedResult()`

Examples:
```java
@Test
void create_courseWithZeroLtp_throwsValidationException() { ... }

@Test
void create_courseWithCircularPrerequisites_throwsValidationException() { ... }

@Test
void delete_courseWithPublishedSessions_throwsConflictException() { ... }

@Test
void create_courseWithCreditMismatch_returnsWarning() { ... }
```

### 8.2 Integration Tests (Testcontainers + Spring Boot Test)

| Scenario | Covers |
|----------|--------|
| CRUD happy path | FR-1.1, AC-1 |
| Create with invalid department FK | FR-5.1, AC-2 |
| Duplicate code rejection | FR-1.3, AC-3 |
| L-T-P all zero rejection | FR-2.2, AC-4 |
| Circular prerequisite rejection | FR-3.3, AC-5 |
| Equipment tag storage and query | FR-4.1, AC-6 |
| Department-scoped listing (RLS) | FR-6.2, AC-7 |
| Pagination, filtering, sorting | FR-6.1, FR-6.2 |
| Soft-delete with published session block | FR-1.5 |
| Unauthenticated access → 401 | NFR Security |

### 8.3 Coverage Target

- 80%+ line coverage on new code (enforced by JaCoCo)

---

## 9. Requirement Traceability

| Requirement | Design Element(s) |
|-------------|-------------------|
| FR-1.1 | CourseController CRUD endpoints, CourseService |
| FR-1.2 | Course entity fields, `CreateCourseRequest` DTO |
| FR-1.3 | `uq_courses_code` constraint, uniqueness check in service |
| FR-1.4 | Soft-delete via `deleted_at`, `CourseService.delete()` |
| FR-1.5 | Published session check in `CourseService.delete()` |
| FR-2.1 | `lecture_hours`, `tutorial_hours`, `practical_hours` columns with CHECK constraints |
| FR-2.2 | `chk_courses_ltp` CHECK constraint + service-layer validation |
| FR-2.3 | Credit hour warning logic in `CourseService.create()/update()` |
| FR-3.1 | `prerequisites` JSONB column storing array of course IDs |
| FR-3.2 | Prerequisite existence validation in `CourseService.validatePrerequisites()` |
| FR-3.3 | Circular dependency detection in `PrerequisiteValidator` |
| FR-4.1 | `equipment_tags` JSONB column |
| FR-4.2 | GIN index on equipment_tags for scheduling engine queries |
| FR-4.3 | `EquipmentTagValidator` format, count, and length checks |
| FR-5.1 | `department_id` FK column |
| FR-5.2 | FK validation in `CourseService.create()` against active departments |
| FR-5.3 | `is_cross_listed` boolean column |
| FR-6.1 | Paginated list endpoint with `PagedResponse` wrapper |
| FR-6.2 | Query parameter filters: `departmentId`, `courseType`, `isActive`, `search` |
| FR-6.3 | Default filter `deleted_at IS NULL`; override with `includeDeleted=true` |
| NFR Performance | Indexed queries, pagination, < 200ms target |
| NFR Security | JWT auth, `@PreAuthorize`, RLS scoping |
| NFR Input Validation | Jakarta Validation, allowlist patterns, sanitized strings |
| NFR Parameterized Queries | Spring Data JPA (no raw concatenation) |
| NFR Audit | `AuditEventPublisher` in same transaction |
| NFR Data Segregation | Repository-level department filtering from SecurityContext |

---

## 10. Open Questions

| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | Should `credit_hours` be auto-calculated from L+T+P or manually entered? Current design: manual entry with warning. | Product Owner | Open |
| 2 | How should cross-listed courses be handled in the scheduling engine — counted once or per department? | Tech Lead | Open |
| 3 | Maximum number of prerequisites allowed per course? Current design: no limit beyond JSONB capacity. | Product Owner | Open |
| 4 | Should equipment tags be a controlled vocabulary (predefined list) or free-form? Current design: free-form with format validation. | Product Owner | Open |
| 5 | Should prerequisite changes trigger re-validation of existing student registrations? | Product Owner | Open |
