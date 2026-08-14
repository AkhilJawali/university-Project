# Design: Master Data — Campus & Department Hierarchy

**Jira Reference:** AID-179
**Source Requirements:** docs/requirements/AID-179-campus-department-hierarchy-requirements.md
**Application:** Existing (Spring Boot modular monolith already scaffolded)
**Stack:** Java 17 · Spring Boot 3.x · Maven · PostgreSQL 15+ · Flyway
**Generated:** 13 August 2026

---

## 1. Overview

This design covers the foundational master data module for the University Timetable Management System (UTMS). It defines the technical architecture for managing the five-level organizational hierarchy:

**Campus → Department → Program → Batch → Section**

Every downstream module (scheduling engine, conflict detection, approval workflow, reporting) depends on this hierarchy being accurate and consistently enforced. The design provides full CRUD with referential integrity, soft-delete semantics, paginated querying, a nested hierarchy tree endpoint, and row-level security for data segregation.

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
                         │  (CampusController,      │
                         │   DepartmentController,  │
                         │   ProgramController,     │
                         │   BatchController,       │
                         │   SectionController)     │
                         └────────────┬────────────┘
                                      │
                         ┌────────────▼────────────┐
                         │   Service Layer          │
                         │  (CampusService,         │
                         │   DepartmentService,     │
                         │   ProgramService,        │
                         │   BatchService,          │
                         │   SectionService,        │
                         │   HierarchyService)      │
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
         │  campuses, departments,         │
         │  programs, batches, sections,   │
         │  audit_events                   │
         └────────────────────────────────┘
```

### Key Design Decisions

- **Synchronous REST** — all operations are standard request/response; no async needed for CRUD.
- **Single transaction** — each mutation (create/update/delete) writes both the entity and the audit event within one DB transaction.
- **Soft-delete** — setting `deleted_at` timestamp; active-only queries use a `@Where` clause or explicit filter.
- **RLS via Spring Security context** — campus/department scoping injected from JWT claims, enforced in repository queries.

---

## 3. API Design

### 3.1 Campus Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/campuses` | List campuses (paginated, filtered) | Authenticated | FR-7.1, FR-7.2, FR-7.4 |
| GET | `/api/v1/campuses/{id}` | Get single campus | Authenticated | FR-1.1 |
| GET | `/api/v1/campuses/{id}/hierarchy` | Full nested tree | Authenticated | FR-7.3 |
| POST | `/api/v1/campuses` | Create campus | ADMIN, REGISTRAR | FR-1.1, FR-1.2 |
| PUT | `/api/v1/campuses/{id}` | Update campus | ADMIN, REGISTRAR | FR-1.1 |
| DELETE | `/api/v1/campuses/{id}` | Soft-delete campus | ADMIN | FR-1.4, FR-1.5 |

#### Create Campus Request DTO
```java
public record CreateCampusRequest(
    @NotBlank @Size(min = 1, max = 200) String name,
    @NotBlank @Size(min = 2, max = 20) @Pattern(regexp = "^[A-Z0-9\\-]+$") String code,
    @Size(max = 500) String address,
    @Size(max = 100) String city,
    @Size(max = 100) String state,
    @NotBlank String timezone
) {}
```

#### Campus Response DTO
```java
public record CampusDto(
    Long id,
    String name,
    String code,
    String address,
    String city,
    String state,
    String timezone,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

#### Error Responses
- `400` — Validation failure (invalid code format, missing name, etc.)
- `409` — Duplicate campus code (FR-1.3)
- `409` — Cannot delete campus with active departments (FR-1.5)
- `401` — No/invalid JWT
- `403` — Insufficient role

---

### 3.2 Department Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/departments` | List departments (paginated, filtered) | Authenticated | FR-7.1, FR-7.2 |
| GET | `/api/v1/departments/{id}` | Get single department | Authenticated | FR-2.1 |
| POST | `/api/v1/departments` | Create department | ADMIN, REGISTRAR | FR-2.1, FR-2.2 |
| PUT | `/api/v1/departments/{id}` | Update department | ADMIN, REGISTRAR | FR-2.1 |
| DELETE | `/api/v1/departments/{id}` | Soft-delete department | ADMIN | FR-2.5 |

#### Create Department Request DTO
```java
public record CreateDepartmentRequest(
    @NotBlank @Size(min = 1, max = 200) String name,
    @NotBlank @Size(min = 2, max = 20) @Pattern(regexp = "^[A-Z0-9\\-]+$") String code,
    @NotNull Long campusId,
    Long hodFacultyId  // nullable
) {}
```

#### Department Response DTO
```java
public record DepartmentDto(
    Long id,
    String name,
    String code,
    Long campusId,
    String campusName,
    Long hodFacultyId,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

#### Referential Integrity Errors
- `400` — `campusId` references non-existent or deleted campus (FR-2.3, FR-6.2)
- `409` — Duplicate code within campus (FR-2.4)
- `409` — Cannot delete department with active programs (FR-2.5, FR-6.3)

---

### 3.3 Program Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/programs` | List programs (paginated, filtered) | Authenticated | FR-7.1 |
| GET | `/api/v1/programs/{id}` | Get single program | Authenticated | FR-3.1 |
| POST | `/api/v1/programs` | Create program | ADMIN, HOD | FR-3.1, FR-3.2 |
| PUT | `/api/v1/programs/{id}` | Update program | ADMIN, HOD | FR-3.1 |
| DELETE | `/api/v1/programs/{id}` | Soft-delete program | ADMIN | FR-3.5 |

#### Create Program Request DTO
```java
public record CreateProgramRequest(
    @NotBlank @Size(min = 1, max = 200) String name,
    @NotBlank @Size(min = 2, max = 20) @Pattern(regexp = "^[A-Z0-9\\-]+$") String code,
    @NotNull Long departmentId,
    @NotNull @Min(1) @Max(8) Integer durationYears,
    @NotNull @Min(1) @Max(16) Integer totalSemesters,
    @NotNull DegreeType degreeType  // UG, PG, PhD, Diploma
) {}
```

---

### 3.4 Batch Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/batches` | List batches (paginated, filtered) | Authenticated | FR-7.1 |
| GET | `/api/v1/batches/{id}` | Get single batch | Authenticated | FR-4.1 |
| POST | `/api/v1/batches` | Create batch | ADMIN, HOD, COORDINATOR | FR-4.1, FR-4.2 |
| PUT | `/api/v1/batches/{id}` | Update batch | ADMIN, HOD, COORDINATOR | FR-4.1 |
| DELETE | `/api/v1/batches/{id}` | Soft-delete batch | ADMIN | FR-4.5 |

#### Create Batch Request DTO
```java
public record CreateBatchRequest(
    @NotBlank @Size(min = 1, max = 200) String name,
    @NotNull Long programId,
    @NotBlank String academicYear,
    @NotNull @Min(1) @Max(16) Integer semesterNumber,
    @NotNull @Min(1) @Max(10000) Integer strength
) {}
```

---

### 3.5 Section Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/sections` | List sections (paginated, filtered) | Authenticated | FR-7.1 |
| GET | `/api/v1/sections/{id}` | Get single section | Authenticated | FR-5.1 |
| POST | `/api/v1/sections` | Create section | ADMIN, HOD, COORDINATOR | FR-5.1, FR-5.2 |
| PUT | `/api/v1/sections/{id}` | Update section | ADMIN, HOD, COORDINATOR | FR-5.1 |
| DELETE | `/api/v1/sections/{id}` | Soft-delete section | ADMIN | FR-5.1 |

#### Create Section Request DTO
```java
public record CreateSectionRequest(
    @NotBlank @Size(min = 1, max = 200) String name,
    @NotNull Long batchId,
    @NotNull @Min(1) @Max(10000) Integer strength
) {}
```

#### Section-Specific Response Behavior
- On create/update: if `sum(section strengths) > batch.strength`, include a `warnings` array in the response (FR-5.4):
```json
{
  "data": { ... },
  "warnings": [
    "Total section strength (180) exceeds batch strength (150). This is allowed but may indicate a configuration issue."
  ]
}
```

---

### 3.6 Hierarchy Tree Endpoint

**GET** `/api/v1/campuses/{id}/hierarchy`

Returns the full nested tree for a campus:
```json
{
  "data": {
    "id": 1,
    "name": "Main Campus",
    "code": "MAIN",
    "departments": [
      {
        "id": 10,
        "name": "Computer Science",
        "code": "CS",
        "programs": [
          {
            "id": 100,
            "name": "B.Tech Computer Science",
            "code": "BTCS",
            "batches": [
              {
                "id": 1000,
                "name": "2024-28",
                "sections": [
                  { "id": 5000, "name": "A", "strength": 60 },
                  { "id": 5001, "name": "B", "strength": 60 }
                ]
              }
            ]
          }
        ]
      }
    ]
  }
}
```

- Traces to: FR-7.3
- Performance target: < 500ms (use `@EntityGraph` or custom query with joins)
- Filters: `?includeDeleted=true` to include soft-deleted entities

---

### 3.7 Common Query Parameters (All List Endpoints)

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | 0 | Page number (0-indexed) |
| `size` | int | 20 | Page size (max 100) |
| `sort` | string | `name,asc` | Sort field and direction |
| `isActive` | boolean | true | Filter by active status |
| `includeDeleted` | boolean | false | Include soft-deleted records |
| `search` | string | — | Full-text search on name/code |
| Parent FK filters | long | — | e.g., `campusId`, `departmentId`, `programId`, `batchId` |

---

## 4. Data Model

### 4.1 Entity-Relationship Diagram

```
┌──────────┐       ┌──────────────┐       ┌──────────┐       ┌─────────┐       ┌──────────┐
│ campuses │1─────N│ departments  │1─────N│ programs │1─────N│ batches │1─────N│ sections │
└──────────┘       └──────────────┘       └──────────┘       └─────────┘       └──────────┘
```

### 4.2 Table Definitions

#### `utms.campuses`
```sql
CREATE TABLE utms.campuses (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20) NOT NULL,
    address         VARCHAR(500),
    city            VARCHAR(100),
    state           VARCHAR(100),
    timezone        VARCHAR(50) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT uq_campuses_code UNIQUE (code)
);

CREATE INDEX idx_campuses_is_active ON utms.campuses(is_active) WHERE deleted_at IS NULL;
```

#### `utms.departments`
```sql
CREATE TABLE utms.departments (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20) NOT NULL,
    campus_id       BIGINT NOT NULL,
    hod_faculty_id  BIGINT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_departments_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id),
    CONSTRAINT uq_departments_code_campus UNIQUE (campus_id, code)
);

CREATE INDEX idx_departments_campus_id ON utms.departments(campus_id);
CREATE INDEX idx_departments_is_active ON utms.departments(is_active) WHERE deleted_at IS NULL;
```

#### `utms.programs`
```sql
CREATE TABLE utms.programs (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20) NOT NULL,
    department_id   BIGINT NOT NULL,
    duration_years  INTEGER NOT NULL CHECK (duration_years BETWEEN 1 AND 8),
    total_semesters INTEGER NOT NULL CHECK (total_semesters BETWEEN 1 AND 16),
    degree_type     VARCHAR(20) NOT NULL CHECK (degree_type IN ('UG', 'PG', 'PHD', 'DIPLOMA')),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_programs_departments FOREIGN KEY (department_id) REFERENCES utms.departments(id),
    CONSTRAINT uq_programs_code_department UNIQUE (department_id, code)
);

CREATE INDEX idx_programs_department_id ON utms.programs(department_id);
CREATE INDEX idx_programs_is_active ON utms.programs(is_active) WHERE deleted_at IS NULL;
```

#### `utms.batches`
```sql
CREATE TABLE utms.batches (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    program_id      BIGINT NOT NULL,
    academic_year   VARCHAR(20) NOT NULL,
    semester_number INTEGER NOT NULL CHECK (semester_number BETWEEN 1 AND 16),
    strength        INTEGER NOT NULL CHECK (strength > 0 AND strength <= 10000),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_batches_programs FOREIGN KEY (program_id) REFERENCES utms.programs(id)
);

CREATE INDEX idx_batches_program_id ON utms.batches(program_id);
CREATE INDEX idx_batches_is_active ON utms.batches(is_active) WHERE deleted_at IS NULL;
```

#### `utms.sections`
```sql
CREATE TABLE utms.sections (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    batch_id        BIGINT NOT NULL,
    strength        INTEGER NOT NULL CHECK (strength > 0 AND strength <= 10000),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_sections_batches FOREIGN KEY (batch_id) REFERENCES utms.batches(id),
    CONSTRAINT uq_sections_name_batch UNIQUE (batch_id, name)
);

CREATE INDEX idx_sections_batch_id ON utms.sections(batch_id);
CREATE INDEX idx_sections_is_active ON utms.sections(is_active) WHERE deleted_at IS NULL;
```

### 4.3 Migration Strategy

- **Tool:** Flyway
- **Naming:** `V{N}__{description}.sql`
- **Planned migrations:**
  - `V1__create_campuses_table.sql`
  - `V2__create_departments_table.sql`
  - `V3__create_programs_table.sql`
  - `V4__create_batches_table.sql`
  - `V5__create_sections_table.sql`
- All migrations are reversible (DROP TABLE IF EXISTS in undo scripts)
- Schema: `utms` (set via Flyway `defaultSchema` config)

---

## 5. Service / Business Logic

### 5.1 Service Classes

| Service | Responsibility | Traces to |
|---------|---------------|-----------|
| `CampusService` | Campus CRUD, uniqueness validation, soft-delete with child check | FR-1.x |
| `DepartmentService` | Department CRUD, FK validation, scoped uniqueness | FR-2.x |
| `ProgramService` | Program CRUD, FK validation, scoped uniqueness | FR-3.x |
| `BatchService` | Batch CRUD, FK validation, strength validation | FR-4.x |
| `SectionService` | Section CRUD, FK validation, strength warning logic | FR-5.x |
| `HierarchyService` | Hierarchy tree assembly, recursive loading | FR-7.3 |

### 5.2 Key Business Rules

#### Referential Integrity (FR-6.x)
```java
// In DepartmentService.create():
Campus campus = campusRepository.findByIdAndDeletedAtIsNull(request.campusId())
    .orElseThrow(() -> new ValidationException("campusId",
        "Campus not found or has been deleted", request.campusId()));
```

#### Soft-Delete with Child Protection (FR-1.5, FR-2.5, FR-3.5, FR-4.5)
```java
// In CampusService.delete():
long activeChildren = departmentRepository.countByCampusIdAndDeletedAtIsNull(campusId);
if (activeChildren > 0) {
    throw new ConflictException("Cannot delete campus: " + activeChildren
        + " active department(s) still reference it.");
}
campus.setDeletedAt(LocalDateTime.now());
```

#### Section Strength Warning (FR-5.4)
```java
// In SectionService.create():
int totalSectionStrength = sectionRepository.sumStrengthByBatchId(request.batchId()) + request.strength();
List<String> warnings = new ArrayList<>();
if (totalSectionStrength > batch.getStrength()) {
    warnings.add(String.format(
        "Total section strength (%d) exceeds batch strength (%d).",
        totalSectionStrength, batch.getStrength()));
}
return new CreateResponse<>(mapper.toDto(saved), warnings);
```

### 5.3 Validation Rules (Jakarta Validation)

All validation is applied at the controller layer via `@Valid` on request DTOs. Custom validators:

| Validator | Purpose |
|-----------|---------|
| `@UniqueCode` | Checks code uniqueness within scope (custom annotation + ConstraintValidator) |
| `@ActiveParent` | Validates that referenced parent exists and is not soft-deleted |

### 5.4 Transaction Boundaries

- Every `create`, `update`, `delete` method is annotated `@Transactional`
- Audit event is written in the same transaction (via JPA `@EntityListeners` or explicit call)
- Read operations use `@Transactional(readOnly = true)`

---

## 6. Cross-cutting Concerns

### 6.1 Error Handling

`@RestControllerAdvice` in `com.utms.common.exception.GlobalExceptionHandler`:

| Exception | HTTP Status | Traces to |
|-----------|-------------|-----------|
| `MethodArgumentNotValidException` | 400 | FR-6.2, Validation Rules |
| `ValidationException` (custom) | 400 | FR-6.2 |
| `EntityNotFoundException` | 404 | — |
| `ConflictException` (custom) | 409 | FR-6.3, FR-1.3, FR-2.4, FR-3.4, FR-5.5 |
| `AccessDeniedException` | 403 | NFR Security |
| `Exception` (catch-all) | 500 | NFR Security (no internals exposed) |

Response format per API standards:
```json
{
  "timestamp": "2026-08-13T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/departments",
  "details": [
    { "field": "campusId", "message": "Campus not found or has been deleted", "rejectedValue": 999 }
  ]
}
```

### 6.2 Security

- **Authentication:** JWT extracted via Spring Security filter; user ID and roles in claims.
- **Authorization:** `@PreAuthorize` on controller methods:
  - Write operations: `hasAnyRole('ADMIN', 'REGISTRAR')` (campus/dept), `hasAnyRole('ADMIN', 'HOD', 'COORDINATOR')` (batch/section)
  - Read operations: `isAuthenticated()`
- **Data Segregation (RLS):** Repository queries include campus/department scope derived from the authenticated user's JWT claims. HODs and Coordinators see only their own campus/department data.
- **Input Sanitization:** All string inputs trimmed; code field forced to uppercase; no HTML allowed in any field.

### 6.3 Audit Trail

Every mutation (create, update, soft-delete) triggers an audit event stored in the `audit_events` table within the same transaction:

```java
@Component
@RequiredArgsConstructor
public class AuditEventPublisher {
    private final AuditEventRepository auditEventRepository;

    public void record(String entityType, Long entityId, String action,
                       Object previousValue, Object newValue, String userId) {
        // Persists audit record in same transaction
    }
}
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
| Performance (< 200ms single CRUD) | Direct JPA queries with indexes on FKs and active-status columns; no N+1 |
| Performance (< 500ms hierarchy tree) | Custom JPQL with JOIN FETCH or `@EntityGraph` loading full tree in one query |
| Security (RBAC) | `@PreAuthorize` annotations on every endpoint; role hierarchy configured in Spring Security |
| Security (RLS / Data Segregation) | Repository methods include campus/dept filter from SecurityContext |
| Input Validation (allowlist) | Jakarta Validation + `@Pattern` for code format; reject unknown fields via Jackson `FAIL_ON_UNKNOWN_PROPERTIES` |
| Parameterized Queries | Spring Data JPA method queries and `@Query` with named parameters — no string concatenation |
| Audit (same transaction) | Audit events persisted within the `@Transactional` boundary of the mutation |

---

## 8. Testing Strategy

### 8.1 Unit Tests (JUnit 5 + Mockito)

| Layer | What to Test |
|-------|-------------|
| Service | Referential integrity checks, soft-delete child protection, strength warning logic, uniqueness validation |
| Mapper | DTO ↔ Entity mapping correctness (MapStruct generated, but verify edge cases) |
| Validators | Custom annotation validators (`@UniqueCode`, `@ActiveParent`) |

Naming: `methodName_scenario_expectedResult()`

Example:
```java
@Test
void delete_campusWithActiveDepartments_throwsConflictException() { ... }

@Test
void create_sectionExceedsBatchStrength_returnsWarning() { ... }
```

### 8.2 Integration Tests (Testcontainers + Spring Boot Test)

| Scenario | Covers |
|----------|--------|
| CRUD happy path for each entity | FR-1.1 through FR-5.1 |
| Create with invalid parent FK | FR-6.2, AC-2 |
| Delete parent with active children | FR-6.3, AC-3 |
| Duplicate code rejection | FR-1.3, AC-4 |
| Section strength warning | FR-5.4, AC-5 |
| Unauthenticated access → 401 | AC-6 |
| Coordinator RLS scoping | AC-7 |
| Hierarchy tree endpoint correctness and performance | FR-7.3 |
| Pagination, filtering, sorting | FR-7.1, FR-7.2 |

### 8.3 Coverage Target

- 80%+ line coverage on new code (enforced by JaCoCo)

---

## 9. Requirement Traceability

| Requirement | Design Element(s) |
|-------------|-------------------|
| FR-1.1 | CampusController CRUD endpoints, CampusService |
| FR-1.2 | Campus entity fields, `CreateCampusRequest` DTO |
| FR-1.3 | `uq_campuses_code` constraint, uniqueness check in service |
| FR-1.4 | Soft-delete via `deleted_at`, `CampusService.delete()` |
| FR-1.5 | Child protection check in `CampusService.delete()` |
| FR-2.1 | DepartmentController CRUD endpoints, DepartmentService |
| FR-2.2 | Department entity fields, `CreateDepartmentRequest` DTO |
| FR-2.3 | FK validation in `DepartmentService.create()` |
| FR-2.4 | `uq_departments_code_campus` composite unique constraint |
| FR-2.5 | Child protection check in `DepartmentService.delete()` |
| FR-3.1 | ProgramController CRUD endpoints, ProgramService |
| FR-3.2 | Program entity fields, `CreateProgramRequest` DTO, `DegreeType` enum |
| FR-3.3 | FK validation in `ProgramService.create()` |
| FR-3.4 | `uq_programs_code_department` composite unique constraint |
| FR-3.5 | Child protection check in `ProgramService.delete()` |
| FR-4.1 | BatchController CRUD endpoints, BatchService |
| FR-4.2 | Batch entity fields, `CreateBatchRequest` DTO |
| FR-4.3 | FK validation in `BatchService.create()` |
| FR-4.4 | `CHECK (strength > 0 AND strength <= 10000)` + Jakarta `@Min(1) @Max(10000)` |
| FR-4.5 | Child protection check in `BatchService.delete()` |
| FR-5.1 | SectionController CRUD endpoints, SectionService |
| FR-5.2 | Section entity fields, `CreateSectionRequest` DTO |
| FR-5.3 | FK validation in `SectionService.create()` |
| FR-5.4 | Strength sum warning in `SectionService.create()/update()` |
| FR-5.5 | `uq_sections_name_batch` composite unique constraint |
| FR-6.1 | All FK validations in service layer before persist |
| FR-6.2 | `ValidationException` → HTTP 400 with field-level error |
| FR-6.3 | `ConflictException` → HTTP 409 with dependent entity details |
| FR-6.4 | Soft-delete only; no cascading hard deletes |
| FR-7.1 | Paginated list endpoints on all controllers |
| FR-7.2 | Query parameter filtering (`isActive`, parent FK, `search`) |
| FR-7.3 | `HierarchyService` + `/campuses/{id}/hierarchy` endpoint |
| FR-7.4 | Default filter `deleted_at IS NULL`; override with `includeDeleted=true` |
| NFR Performance | Indexed queries, `@EntityGraph`, pagination, < 200ms / < 500ms targets |
| NFR Security | JWT auth, `@PreAuthorize`, RLS scoping |
| NFR Input Validation | Jakarta Validation, allowlist patterns, sanitized strings |
| NFR Parameterized Queries | Spring Data JPA (no raw concatenation) |
| NFR Audit | `AuditEventPublisher` in same transaction |
| NFR Data Segregation | Repository-level campus/dept filtering from SecurityContext |

---

## 10. Open Questions

| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | Maximum number of campuses expected (affects index strategy)? | Product Owner | Open |
| 2 | Does campus `timezone` affect slot-grid interpretation or is it informational? | Product Owner | Open |
| 3 | Should `hod_faculty_id` FK on departments reference the `faculty` table (circular dependency with that module)? | Tech Lead | Open |
| 4 | Is there a need for bulk import (CSV/Excel) of hierarchy data for initial data loading? | Product Owner | Open |
| 5 | Should the hierarchy tree endpoint support partial depth (e.g., campus + departments only)? | Tech Lead | Open |
