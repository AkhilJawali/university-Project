# Design: Master Data — Faculty Management

**Jira Reference:** AID-181
**Source Requirements:** docs/requirements/AID-181-faculty-management-requirements.md
**Application:** Existing (Spring Boot modular monolith already scaffolded)
**Stack:** Java 17 · Spring Boot 3.x · Maven · PostgreSQL 15+ · Flyway
**Generated:** 13 August 2026

---

## 1. Overview

This design covers the faculty management module for the University Timetable Management System (UTMS). Faculty data includes profiles, availability preferences, subject competencies, multi-campus associations, and workload configuration. All of this feeds into the scheduling engine as hard and soft constraints.

The module provides:
- Full CRUD for faculty profiles with department association
- Availability window management with overlap detection and hard/soft constraint types
- Subject competency mapping (faculty-to-course junction)
- Multi-campus associations with travel-time buffers
- Workload configuration per cadre with min/max weekly hour limits

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
                         │  (FacultyController,     │
                         │   AvailabilityController,│
                         │   CompetencyController,  │
                         │   CampusAssocController) │
                         └────────────┬────────────┘
                                      │
                         ┌────────────▼────────────┐
                         │   Service Layer          │
                         │  (FacultyService,        │
                         │   AvailabilityService,   │
                         │   CompetencyService,     │
                         │   CampusAssocService)    │
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
         │  faculty, faculty_availability_ │
         │  windows, faculty_competencies, │
         │  faculty_campus_associations,   │
         │  audit_events                   │
         └────────────────────────────────┘
```

### Key Design Decisions

- **Synchronous REST** — all operations are standard request/response; no async needed for CRUD.
- **Single transaction** — each mutation writes both the entity and the audit event within one DB transaction.
- **Soft-delete** — setting `deleted_at` timestamp; active-only queries use explicit filter.
- **RLS via Spring Security context** — department scoping injected from JWT claims, enforced in repository queries.
- **Separate sub-resource endpoints** — availability, competencies, and campus associations managed via dedicated endpoints under `/api/v1/faculty/{id}/...` for clarity and independent lifecycle management.
- **Constraint type enum** — availability windows classified as HARD_UNAVAILABLE, SOFT_PREFERRED, SOFT_AVOID to enable scheduling engine differentiation.

---

## 3. API Design

### 3.1 Faculty Profile Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/faculty` | List faculty (paginated, filtered) | Authenticated | FR-6.1, FR-6.2 |
| GET | `/api/v1/faculty/{id}` | Get single faculty (basic) | Authenticated | FR-1.1 |
| GET | `/api/v1/faculty/{id}/full` | Get full profile (includes availability, competencies, associations) | Authenticated | FR-6.3 |
| POST | `/api/v1/faculty` | Create faculty | ADMIN, HOD | FR-1.1, FR-1.2 |
| PUT | `/api/v1/faculty/{id}` | Update faculty | ADMIN, HOD | FR-1.1 |
| DELETE | `/api/v1/faculty/{id}` | Soft-delete faculty | ADMIN | FR-1.4, FR-1.5 |

#### Create Faculty Request DTO
```java
public record CreateFacultyRequest(
    @NotBlank @Size(min = 2, max = 20) @Pattern(regexp = "^[A-Z0-9]+$") String employeeId,
    @NotBlank @Size(min = 1, max = 100) String firstName,
    @NotBlank @Size(min = 1, max = 100) String lastName,
    @NotBlank @Email @Size(max = 200) String email,
    @Size(min = 10, max = 15) @Pattern(regexp = "^[0-9]+$") String phone,
    @NotNull Long departmentId,
    @NotNull Cadre cadre,
    @Size(max = 200) String qualification
) {}
```

#### Faculty Response DTO
```java
public record FacultyDto(
    Long id,
    String employeeId,
    String firstName,
    String lastName,
    String email,
    String phone,
    Long departmentId,
    String departmentName,
    Cadre cadre,
    String qualification,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

#### Full Faculty Profile Response DTO
```java
public record FacultyFullProfileDto(
    FacultyDto faculty,
    List<AvailabilityWindowDto> availabilityWindows,
    List<CompetencyCourseDto> competencies,
    List<CampusAssociationDto> campusAssociations,
    WorkloadConfigDto workloadConfig
) {}
```

#### Cadre Enum
```java
public enum Cadre {
    PROFESSOR, ASSOCIATE_PROFESSOR, ASSISTANT_PROFESSOR, LECTURER, VISITING
}
```

#### Error Responses
- `400` — Validation failure (invalid email, missing department, overlapping availability)
- `409` — Duplicate employee_id or email (FR-1.3)
- `409` — Cannot delete faculty with published timetable sessions (FR-1.5)
- `401` — No/invalid JWT
- `403` — Insufficient role

---

### 3.2 Availability Window Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/faculty/{id}/availability` | List all availability windows | Authenticated | FR-2.1 |
| POST | `/api/v1/faculty/{id}/availability` | Create availability window | ADMIN, HOD, FACULTY (own) | FR-2.1, FR-2.2 |
| PUT | `/api/v1/faculty/{id}/availability/{windowId}` | Update availability window | ADMIN, HOD, FACULTY (own) | FR-2.1 |
| DELETE | `/api/v1/faculty/{id}/availability/{windowId}` | Delete availability window | ADMIN, HOD, FACULTY (own) | FR-2.1 |

#### Create Availability Window Request DTO
```java
public record CreateAvailabilityWindowRequest(
    @NotNull @Min(1) @Max(7) Integer dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @NotNull ConstraintType constraintType
) {}

public enum ConstraintType {
    HARD_UNAVAILABLE, SOFT_PREFERRED, SOFT_AVOID
}
```

#### Availability Window Response DTO
```java
public record AvailabilityWindowDto(
    Long id,
    Integer dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    ConstraintType constraintType
) {}
```

---

### 3.3 Competency Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/faculty/{id}/competencies` | List competencies | Authenticated | FR-3.1 |
| POST | `/api/v1/faculty/{id}/competencies` | Add competency | ADMIN, HOD | FR-3.1, FR-3.2 |
| DELETE | `/api/v1/faculty/{id}/competencies/{courseId}` | Remove competency | ADMIN, HOD | FR-3.1 |

#### Add Competency Request DTO
```java
public record AddCompetencyRequest(
    @NotNull Long courseId
) {}
```

#### Competency Response DTO
```java
public record CompetencyCourseDto(
    Long courseId,
    String courseCode,
    String courseName
) {}
```

---

### 3.4 Campus Association Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/faculty/{id}/campus-associations` | List campus associations | Authenticated | FR-4.1 |
| POST | `/api/v1/faculty/{id}/campus-associations` | Add campus association | ADMIN, HOD | FR-4.1, FR-4.2 |
| PUT | `/api/v1/faculty/{id}/campus-associations/{assocId}` | Update travel time | ADMIN, HOD | FR-4.2 |
| DELETE | `/api/v1/faculty/{id}/campus-associations/{assocId}` | Remove association | ADMIN, HOD | FR-4.1 |

#### Create Campus Association Request DTO
```java
public record CreateCampusAssociationRequest(
    @NotNull Long campusId,
    @NotNull @Min(0) @Max(480) Integer travelTimeMinutes
) {}
```

#### Campus Association Response DTO
```java
public record CampusAssociationDto(
    Long id,
    Long campusId,
    String campusName,
    Integer travelTimeMinutes
) {}
```

---

### 3.5 Common Query Parameters (List Endpoint)

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | 0 | Page number (0-indexed) |
| `size` | int | 20 | Page size (max 100) |
| `sort` | string | `lastName,asc` | Sort field and direction |
| `isActive` | boolean | true | Filter by active status |
| `includeDeleted` | boolean | false | Include soft-deleted records |
| `search` | string | — | Full-text search on name/email/employeeId |
| `departmentId` | long | — | Filter by department |
| `cadre` | string | — | Filter by cadre |
| `campusId` | long | — | Filter by campus association |

---

## 4. Data Model

### 4.1 Entity-Relationship Diagram

```
Department (1) ──→ (N) Faculty
Faculty (1) ──→ (N) FacultyAvailabilityWindow
Faculty (N) ──→ (N) Course (via faculty_competencies)
Faculty (N) ──→ (N) Campus (via faculty_campus_associations)
```

### 4.2 Table Definitions

#### `utms.faculty`
```sql
CREATE TABLE utms.faculty (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     VARCHAR(20) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(200) NOT NULL,
    phone           VARCHAR(15),
    department_id   BIGINT NOT NULL,
    cadre           VARCHAR(30) NOT NULL CHECK (cadre IN ('PROFESSOR', 'ASSOCIATE_PROFESSOR', 'ASSISTANT_PROFESSOR', 'LECTURER', 'VISITING')),
    qualification   VARCHAR(200),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_faculty_departments FOREIGN KEY (department_id) REFERENCES utms.departments(id),
    CONSTRAINT uq_faculty_employee_id UNIQUE (employee_id),
    CONSTRAINT uq_faculty_email UNIQUE (email)
);

CREATE INDEX idx_faculty_department_id ON utms.faculty(department_id);
CREATE INDEX idx_faculty_cadre ON utms.faculty(cadre);
CREATE INDEX idx_faculty_is_active ON utms.faculty(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_faculty_email ON utms.faculty(email);
```

#### `utms.faculty_availability_windows`
```sql
CREATE TABLE utms.faculty_availability_windows (
    id              BIGSERIAL PRIMARY KEY,
    faculty_id      BIGINT NOT NULL,
    day_of_week     INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    constraint_type VARCHAR(20) NOT NULL CHECK (constraint_type IN ('HARD_UNAVAILABLE', 'SOFT_PREFERRED', 'SOFT_AVOID')),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,

    CONSTRAINT fk_availability_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id) ON DELETE CASCADE,
    CONSTRAINT chk_availability_time CHECK (start_time < end_time)
);

CREATE INDEX idx_availability_faculty_id ON utms.faculty_availability_windows(faculty_id);
CREATE INDEX idx_availability_faculty_day ON utms.faculty_availability_windows(faculty_id, day_of_week);
```

#### `utms.faculty_competencies`
```sql
CREATE TABLE utms.faculty_competencies (
    id              BIGSERIAL PRIMARY KEY,
    faculty_id      BIGINT NOT NULL,
    course_id       BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,

    CONSTRAINT fk_competencies_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id) ON DELETE CASCADE,
    CONSTRAINT fk_competencies_courses FOREIGN KEY (course_id) REFERENCES utms.courses(id),
    CONSTRAINT uq_faculty_competency UNIQUE (faculty_id, course_id)
);

CREATE INDEX idx_competencies_faculty_id ON utms.faculty_competencies(faculty_id);
CREATE INDEX idx_competencies_course_id ON utms.faculty_competencies(course_id);
```

#### `utms.faculty_campus_associations`
```sql
CREATE TABLE utms.faculty_campus_associations (
    id                  BIGSERIAL PRIMARY KEY,
    faculty_id          BIGINT NOT NULL,
    campus_id           BIGINT NOT NULL,
    travel_time_minutes INTEGER NOT NULL CHECK (travel_time_minutes >= 0 AND travel_time_minutes <= 480),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,

    CONSTRAINT fk_campus_assoc_faculty FOREIGN KEY (faculty_id) REFERENCES utms.faculty(id) ON DELETE CASCADE,
    CONSTRAINT fk_campus_assoc_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id),
    CONSTRAINT uq_faculty_campus UNIQUE (faculty_id, campus_id)
);

CREATE INDEX idx_campus_assoc_faculty_id ON utms.faculty_campus_associations(faculty_id);
CREATE INDEX idx_campus_assoc_campus_id ON utms.faculty_campus_associations(campus_id);
```

#### `utms.workload_configs`
```sql
CREATE TABLE utms.workload_configs (
    id                  BIGSERIAL PRIMARY KEY,
    cadre               VARCHAR(30) NOT NULL UNIQUE CHECK (cadre IN ('PROFESSOR', 'ASSOCIATE_PROFESSOR', 'ASSISTANT_PROFESSOR', 'LECTURER', 'VISITING')),
    min_weekly_hours    INTEGER NOT NULL CHECK (min_weekly_hours >= 0),
    max_weekly_hours    INTEGER NOT NULL CHECK (max_weekly_hours > 0),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,

    CONSTRAINT chk_workload_min_max CHECK (max_weekly_hours >= min_weekly_hours)
);
```

### 4.3 Migration Strategy

- **Tool:** Flyway
- **Naming:** `V{N}__{description}.sql`
- **Planned migrations:**
  - `V7__create_faculty_table.sql`
  - `V8__create_faculty_availability_windows_table.sql`
  - `V9__create_faculty_competencies_table.sql`
  - `V10__create_faculty_campus_associations_table.sql`
  - `V11__create_workload_configs_table.sql`
  - `V12__seed_workload_configs.sql` (default min/max per cadre)
- All migrations are reversible (DROP TABLE IF EXISTS in undo scripts)
- Schema: `utms` (set via Flyway `defaultSchema` config)

---

## 5. Service / Business Logic

### 5.1 Service Classes

| Service | Responsibility | Traces to |
|---------|---------------|-----------|
| `FacultyService` | Faculty CRUD, uniqueness, soft-delete with session check, full profile assembly | FR-1.x, FR-6.x |
| `AvailabilityService` | Availability CRUD, overlap detection, constraint type management | FR-2.x |
| `CompetencyService` | Competency mapping CRUD, course existence validation | FR-3.x |
| `CampusAssociationService` | Campus association CRUD, campus existence validation | FR-4.x |
| `WorkloadConfigService` | Cadre-level workload configuration management | FR-5.x |

### 5.2 Key Business Rules

#### Uniqueness Validation (FR-1.3)
```java
// In FacultyService.create():
if (facultyRepository.existsByEmployeeIdAndDeletedAtIsNull(request.employeeId())) {
    throw new ConflictException("Faculty with employee_id '" + request.employeeId() + "' already exists.");
}
if (facultyRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
    throw new ConflictException("Faculty with email '" + request.email() + "' already exists.");
}
```

#### Availability Overlap Detection (FR-2.5)
```java
// In AvailabilityService.create():
public void validateNoOverlap(Long facultyId, Integer dayOfWeek, LocalTime startTime, LocalTime endTime, Long excludeId) {
    List<FacultyAvailabilityWindow> existing = availabilityRepository
        .findByFacultyIdAndDayOfWeek(facultyId, dayOfWeek);

    for (FacultyAvailabilityWindow window : existing) {
        if (excludeId != null && window.getId().equals(excludeId)) continue;
        if (startTime.isBefore(window.getEndTime()) && endTime.isAfter(window.getStartTime())) {
            throw new ValidationException("startTime,endTime",
                String.format("Availability window overlaps with existing window (%s - %s) on day %d",
                    window.getStartTime(), window.getEndTime(), dayOfWeek),
                startTime + "-" + endTime);
        }
    }
}
```

#### Soft-Delete with Published Session Check (FR-1.5)
```java
// In FacultyService.delete():
boolean hasPublishedSessions = sessionRepository
    .existsByFacultyIdAndTimetableStatus(facultyId, TimetableStatus.PUBLISHED);
if (hasPublishedSessions) {
    throw new ConflictException("Cannot delete faculty: assigned to published timetable sessions.");
}
faculty.setDeletedAt(LocalDateTime.now());
```

#### Competency Validation (FR-3.3)
```java
// In CompetencyService.addCompetency():
Course course = courseRepository.findByIdAndDeletedAtIsNull(request.courseId())
    .orElseThrow(() -> new ValidationException("courseId",
        "Course not found or inactive", request.courseId()));

if (competencyRepository.existsByFacultyIdAndCourseId(facultyId, request.courseId())) {
    throw new ConflictException("Faculty already has competency for course: " + course.getCode());
}
```

#### Campus Association Validation (FR-4.2)
```java
// In CampusAssociationService.create():
Campus campus = campusRepository.findByIdAndDeletedAtIsNull(request.campusId())
    .orElseThrow(() -> new ValidationException("campusId",
        "Campus not found or inactive", request.campusId()));

if (campusAssocRepository.existsByFacultyIdAndCampusId(facultyId, request.campusId())) {
    throw new ConflictException("Faculty already associated with campus: " + campus.getName());
}
```

#### Workload Configuration (FR-5.1, FR-5.2)
```java
// In WorkloadConfigService.getForCadre():
public WorkloadConfigDto getForCadre(Cadre cadre) {
    return workloadConfigRepository.findByCadre(cadre.name())
        .map(mapper::toDto)
        .orElseThrow(() -> new EntityNotFoundException("Workload config not found for cadre: " + cadre));
}
```

### 5.3 Validation Rules (Jakarta Validation)

All validation is applied at the controller layer via `@Valid` on request DTOs. Custom validators:

| Validator | Purpose |
|-----------|---------|
| `@UniqueEmployeeId` | Checks employee_id uniqueness |
| `@UniqueEmail` | Checks email uniqueness |
| `@ActiveParent` | Validates that referenced department/campus/course exists and is not soft-deleted |
| `@ValidTimeRange` | Ensures startTime < endTime |

### 5.4 Transaction Boundaries

- Every `create`, `update`, `delete` method is annotated `@Transactional`
- Audit event is written in the same transaction (via `AuditEventPublisher`)
- Read operations use `@Transactional(readOnly = true)`
- Full profile endpoint (`/full`) uses `@Transactional(readOnly = true)` with eager fetch

---

## 6. Cross-cutting Concerns

### 6.1 Error Handling

`@RestControllerAdvice` in `com.utms.common.exception.GlobalExceptionHandler`:

| Exception | HTTP Status | Traces to |
|-----------|-------------|-----------|
| `MethodArgumentNotValidException` | 400 | Validation Rules |
| `ValidationException` (custom) | 400 | FR-2.5 (overlap), FR-3.3, FR-4.2 |
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
  "path": "/api/v1/faculty/5/availability",
  "details": [
    { "field": "startTime,endTime", "message": "Availability window overlaps with existing window (09:00 - 11:00) on day 1", "rejectedValue": "08:30-10:00" }
  ]
}
```

### 6.2 Security

- **Authentication:** JWT extracted via Spring Security filter; user ID and roles in claims.
- **Authorization:** `@PreAuthorize` on controller methods:
  - Faculty profile write: `hasAnyRole('ADMIN', 'HOD')`
  - Availability write: `hasAnyRole('ADMIN', 'HOD') or (#id == authentication.principal.facultyId)`
  - Competency/association write: `hasAnyRole('ADMIN', 'HOD')`
  - Read operations: `isAuthenticated()`
- **Data Segregation (RLS):** Repository queries include department scope derived from the authenticated user's JWT claims. HODs see only faculty in their department.
- **Input Sanitization:** All string inputs trimmed; employee_id forced to uppercase; no HTML allowed.

### 6.3 Audit Trail

Every mutation (create, update, soft-delete) triggers an audit event stored in the `audit_events` table within the same transaction:

```java
auditEventPublisher.record("FACULTY", faculty.getId(), "UPDATE",
    mapper.toDto(previousState), mapper.toDto(updatedState), currentUserId);
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
    workload:
      defaults:
        PROFESSOR: { min: 8, max: 14 }
        ASSOCIATE_PROFESSOR: { min: 10, max: 16 }
        ASSISTANT_PROFESSOR: { min: 12, max: 18 }
        LECTURER: { min: 14, max: 20 }
        VISITING: { min: 4, max: 10 }
  ```

### 6.5 Logging

- SLF4J + Logback with structured JSON in deployed environments
- Log: entity type, entity ID, operation, user ID, request ID
- Never log: passwords, tokens, full request bodies with PII (email, phone)
- Security events (access denials, validation failures) logged at WARN level

---

## 7. Non-Functional Design

| NFR | How It's Met |
|-----|-------------|
| Performance (< 200ms single CRUD) | Direct JPA queries with indexes on FKs, employee_id, email, cadre; no N+1 |
| Performance (< 300ms full profile) | Single query with JOIN FETCH for availability, competencies, associations |
| Security (RBAC) | `@PreAuthorize` annotations on every endpoint; role hierarchy configured in Spring Security |
| Security (RLS / Data Segregation) | Repository methods include department filter from SecurityContext |
| Input Validation (allowlist) | Jakarta Validation + `@Pattern` for employee_id format; reject unknown fields via Jackson `FAIL_ON_UNKNOWN_PROPERTIES` |
| Parameterized Queries | Spring Data JPA method queries and `@Query` with named parameters — no string concatenation |
| Audit (same transaction) | Audit events persisted within the `@Transactional` boundary of the mutation |

---

## 8. Testing Strategy

### 8.1 Unit Tests (JUnit 5 + Mockito)

| Layer | What to Test |
|-------|-------------|
| Service | Uniqueness validation (employee_id, email), soft-delete with session check, department FK validation |
| AvailabilityService | Overlap detection (partial overlap, full overlap, adjacent non-overlap, same day different constraint types) |
| CompetencyService | Course existence validation, duplicate competency rejection |
| CampusAssociationService | Campus existence validation, duplicate association rejection |
| Mapper | DTO ↔ Entity mapping correctness, full profile assembly |

Naming: `methodName_scenario_expectedResult()`

Examples:
```java
@Test
void create_facultyWithDuplicateEmployeeId_throwsConflictException() { ... }

@Test
void createAvailability_overlappingWindowOnSameDay_throwsValidationException() { ... }

@Test
void createAvailability_adjacentWindowsNoOverlap_succeeds() { ... }

@Test
void delete_facultyWithPublishedSessions_throwsConflictException() { ... }

@Test
void addCompetency_courseNotFound_throwsValidationException() { ... }

@Test
void getFullProfile_assembliesAllSubResources_returnsCompleteProfile() { ... }
```

### 8.2 Integration Tests (Testcontainers + Spring Boot Test)

| Scenario | Covers |
|----------|--------|
| Faculty CRUD happy path | FR-1.1, AC-1 |
| Create with invalid department FK | FR-1.2, AC-2 |
| Duplicate employee_id/email rejection | FR-1.3, AC-3 |
| HARD_UNAVAILABLE scheduling respect | FR-2.3, AC-4 |
| Overlapping availability rejection | FR-2.5, AC-5 |
| Cross-campus travel time storage | FR-4.2, AC-6 |
| Max weekly hours violation flag | FR-5.3, AC-7 |
| Full profile endpoint correctness | FR-6.3 |
| Department-scoped listing (RLS) | NFR Data Segregation |
| Pagination, filtering, sorting | FR-6.1, FR-6.2 |
| Unauthenticated access → 401 | NFR Security |
| Faculty updating own availability | NFR Security (self-service) |

### 8.3 Coverage Target

- 80%+ line coverage on new code (enforced by JaCoCo)

---

## 9. Requirement Traceability

| Requirement | Design Element(s) |
|-------------|-------------------|
| FR-1.1 | FacultyController CRUD endpoints, FacultyService |
| FR-1.2 | Faculty entity fields, `CreateFacultyRequest` DTO, `Cadre` enum |
| FR-1.3 | `uq_faculty_employee_id`, `uq_faculty_email` constraints, uniqueness check in service |
| FR-1.4 | Soft-delete via `deleted_at`, `FacultyService.delete()` |
| FR-1.5 | Published session check in `FacultyService.delete()` |
| FR-2.1 | `faculty_availability_windows` table, AvailabilityController endpoints |
| FR-2.2 | `day_of_week`, `start_time`, `end_time`, `constraint_type` columns |
| FR-2.3 | HARD_UNAVAILABLE enum value used by scheduling engine as absolute constraint |
| FR-2.4 | SOFT_PREFERRED/SOFT_AVOID enum values used as optimization hints |
| FR-2.5 | Overlap detection in `AvailabilityService.validateNoOverlap()` |
| FR-3.1 | `faculty_competencies` junction table, CompetencyController endpoints |
| FR-3.2 | `uq_faculty_competency` unique constraint |
| FR-3.3 | Course existence validation in `CompetencyService.addCompetency()` |
| FR-3.4 | Competency data read by scheduling engine for faculty-course assignment validation |
| FR-4.1 | `faculty_campus_associations` table, CampusAssociationController endpoints |
| FR-4.2 | `travel_time_minutes` column, used by scheduling engine for gap enforcement |
| FR-4.3 | Travel time stored per faculty-campus pair, scheduling engine reads it |
| FR-5.1 | `workload_configs` table with min/max per cadre |
| FR-5.2 | Configurable via admin API, seeded with defaults |
| FR-5.3 | Scheduling engine reads max_weekly_hours as hard constraint |
| FR-5.4 | Scheduling engine flags below min_weekly_hours as warning |
| FR-6.1 | Paginated list endpoint with `PagedResponse` wrapper |
| FR-6.2 | Query parameter filters: `departmentId`, `cadre`, `campusId`, `isActive`, `search` |
| FR-6.3 | `/faculty/{id}/full` endpoint assembling complete profile |
| NFR Performance | Indexed queries, JOIN FETCH, pagination, < 200ms / < 300ms targets |
| NFR Security | JWT auth, `@PreAuthorize`, RLS scoping, self-service for availability |
| NFR Input Validation | Jakarta Validation, allowlist patterns, sanitized strings |
| NFR Parameterized Queries | Spring Data JPA (no raw concatenation) |
| NFR Audit | `AuditEventPublisher` in same transaction |
| NFR Data Segregation | Repository-level department filtering from SecurityContext |

---

## 10. Open Questions

| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | Should faculty be able to update their own availability without HOD approval? Current design: yes (self-service). | Product Owner | Open |
| 2 | Maximum number of campuses a faculty can be associated with? Current design: no limit. | Product Owner | Open |
| 3 | How to handle visiting faculty who may have shorter contract periods? Need an `active_from`/`active_until` date range? | Product Owner | Open |
| 4 | Should workload configuration support semester-specific overrides (e.g., reduced load during exam period)? | Tech Lead | Open |
| 5 | Should availability windows support date-range scoping (valid from/until) for temporary changes? | Tech Lead | Open |
