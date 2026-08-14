# Design: Master Data — Room & Resource Management

**Jira Reference:** AID-182
**Source Requirements:** docs/requirements/AID-182-room-resource-management-requirements.md
**Application:** Existing (Spring Boot modular monolith already scaffolded)
**Stack:** Java 17 · Spring Boot 3.x · Maven · PostgreSQL 15+ · Flyway
**Generated:** 13 August 2026

---

## 1. Overview

This design covers the room and resource management module for the University Timetable Management System (UTMS). Rooms are the physical spaces where sessions are scheduled — each has a capacity, type, equipment tags, and may be subject to temporary resource blocks (maintenance, events, reservations).

The module provides:
- Full CRUD for rooms with capacity, type, equipment tag management
- Resource block lifecycle management (REQUESTED → APPROVED → ACTIVE → RELEASED)
- Room availability queries (excluding blocks and scheduled sessions)
- Block impact detection on published sessions
- Equipment tag matching for scheduling engine room allocation

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
                         │  (RoomController,        │
                         │   ResourceBlockController)│
                         └────────────┬────────────┘
                                      │
                         ┌────────────▼────────────┐
                         │   Service Layer          │
                         │  (RoomService,           │
                         │   ResourceBlockService,  │
                         │   RoomAvailabilityService)│
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
         │  rooms, resource_blocks,        │
         │  audit_events                   │
         └────────────────────────────────┘
```

### Key Design Decisions

- **Synchronous REST** — all operations are standard request/response; no async needed for CRUD.
- **Single transaction** — each mutation writes both the entity and the audit event within one DB transaction.
- **Soft-delete** — setting `deleted_at` timestamp; active-only queries use explicit filter.
- **RLS via Spring Security context** — campus scoping injected from JWT claims, enforced in repository queries.
- **JSONB for equipment_tags** — leverages PostgreSQL's native JSON support with GIN index for efficient tag-based querying by the scheduling engine.
- **State machine for resource blocks** — explicit status transitions with approval gating when published sessions are impacted.
- **Availability computed dynamically** — room availability is calculated by subtracting blocks and scheduled sessions from the room's total time grid, not stored as a separate materialized view.

---

## 3. API Design

### 3.1 Room Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/rooms` | List rooms (paginated, filtered) | Authenticated | FR-5.1, FR-5.2 |
| GET | `/api/v1/rooms/{id}` | Get single room | Authenticated | FR-1.1 |
| GET | `/api/v1/rooms/{id}/availability` | Get available time slots for date range | Authenticated | FR-5.3 |
| POST | `/api/v1/rooms` | Create room | ADMIN | FR-1.1, FR-1.2 |
| PUT | `/api/v1/rooms/{id}` | Update room | ADMIN | FR-1.1 |
| DELETE | `/api/v1/rooms/{id}` | Soft-delete room | ADMIN | FR-1.4, FR-1.5 |

#### Create Room Request DTO
```java
public record CreateRoomRequest(
    @NotBlank @Size(min = 2, max = 20) @Pattern(regexp = "^[A-Z0-9\\-]+$") String code,
    @NotBlank @Size(min = 1, max = 200) String name,
    @NotNull Long campusId,
    @NotBlank @Size(min = 1, max = 100) String building,
    @Size(max = 20) String floor,
    @NotNull @Min(1) @Max(5000) Integer capacity,
    @NotNull RoomType roomType,
    @Size(max = 20) List<@NotBlank @Size(max = 50) @Pattern(regexp = "^[a-z][a-z0-9\\-]*$") String> equipmentTags
) {}
```

#### Update Room Request DTO
```java
public record UpdateRoomRequest(
    @NotBlank @Size(min = 1, max = 200) String name,
    @NotBlank @Size(min = 1, max = 100) String building,
    @Size(max = 20) String floor,
    @NotNull @Min(1) @Max(5000) Integer capacity,
    @NotNull RoomType roomType,
    @Size(max = 20) List<@NotBlank @Size(max = 50) @Pattern(regexp = "^[a-z][a-z0-9\\-]*$") String> equipmentTags
) {}
```

#### Room Response DTO
```java
public record RoomDto(
    Long id,
    String code,
    String name,
    Long campusId,
    String campusName,
    String building,
    String floor,
    Integer capacity,
    RoomType roomType,
    List<String> equipmentTags,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

#### RoomType Enum
```java
public enum RoomType {
    LECTURE_HALL, LAB, SEMINAR, AUDITORIUM, TUTORIAL
}
```

#### Availability Response DTO
```java
public record RoomAvailabilityResponse(
    Long roomId,
    String roomCode,
    LocalDate startDate,
    LocalDate endDate,
    List<DayAvailability> days
) {}

public record DayAvailability(
    LocalDate date,
    List<TimeSlot> availableSlots,
    List<TimeSlot> blockedSlots,
    List<TimeSlot> scheduledSlots
) {}

public record TimeSlot(
    LocalTime startTime,
    LocalTime endTime,
    String reason  // null for available, block reason or session info for occupied
) {}
```

#### Error Responses
- `400` — Validation failure (invalid code format, capacity out of range, invalid equipment tags)
- `409` — Duplicate room code within campus (FR-1.3)
- `409` — Cannot delete room with published sessions (FR-1.5)
- `401` — No/invalid JWT
- `403` — Insufficient role

---

### 3.2 Resource Block Endpoints

| Method | Path | Description | Auth | Traces to |
|--------|------|-------------|------|-----------|
| GET | `/api/v1/rooms/{roomId}/blocks` | List resource blocks for a room (filtered) | Authenticated | FR-5.4 |
| GET | `/api/v1/rooms/{roomId}/blocks/{blockId}` | Get single resource block | Authenticated | FR-4.1 |
| POST | `/api/v1/rooms/{roomId}/blocks` | Create (request) a resource block | ADMIN, COORDINATOR, FACILITIES | FR-4.1, FR-4.2 |
| PUT | `/api/v1/rooms/{roomId}/blocks/{blockId}/approve` | Approve a resource block | ADMIN, REGISTRAR | FR-4.5 |
| PUT | `/api/v1/rooms/{roomId}/blocks/{blockId}/activate` | Activate an approved block | ADMIN | FR-4.3 |
| PUT | `/api/v1/rooms/{roomId}/blocks/{blockId}/release` | Release (end early) a block | ADMIN, FACILITIES | FR-4.6 |

#### Create Resource Block Request DTO
```java
public record CreateResourceBlockRequest(
    @NotBlank @Size(min = 1, max = 500) String reason,
    @NotNull @FutureOrPresent LocalDate startDate,
    @NotNull LocalDate endDate,
    LocalTime startTime,  // nullable for full-day block
    LocalTime endTime,    // nullable for full-day block
    @NotNull ResourceBlockType blockType
) {}

public enum ResourceBlockType {
    MAINTENANCE, EVENT, RESERVATION, EMERGENCY
}
```

#### Resource Block Response DTO
```java
public record ResourceBlockDto(
    Long id,
    Long roomId,
    String roomCode,
    String reason,
    LocalDate startDate,
    LocalDate endDate,
    LocalTime startTime,
    LocalTime endTime,
    ResourceBlockStatus status,
    ResourceBlockType blockType,
    String requestedBy,
    String approvedBy,
    List<ImpactedSessionDto> impactedSessions,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

public record ImpactedSessionDto(
    Long sessionId,
    String courseName,
    String facultyName,
    LocalDate sessionDate,
    LocalTime sessionStartTime,
    LocalTime sessionEndTime
) {}
```

#### ResourceBlockStatus Enum
```java
public enum ResourceBlockStatus {
    REQUESTED, APPROVED, ACTIVE, RELEASED
}
```

#### Status Transition Rules
```
REQUESTED → APPROVED  (by ADMIN/REGISTRAR; required if impactedSessions.size() > 0)
REQUESTED → ACTIVE    (by ADMIN; only if impactedSessions.size() == 0, skipping approval)
APPROVED  → ACTIVE    (by ADMIN)
ACTIVE    → RELEASED  (by ADMIN/FACILITIES; early release)
```

#### Error Responses
- `400` — Validation failure (end_date before start_date, past start_date, missing reason)
- `400` — Invalid status transition (e.g., REQUESTED → RELEASED)
- `409` — Cannot activate block without approval when published sessions are impacted
- `401` — No/invalid JWT
- `403` — Insufficient role

---

### 3.3 Common Query Parameters

#### Room List Endpoint

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | 0 | Page number (0-indexed) |
| `size` | int | 20 | Page size (max 100) |
| `sort` | string | `name,asc` | Sort field and direction |
| `isActive` | boolean | true | Filter by active status |
| `includeDeleted` | boolean | false | Include soft-deleted records |
| `search` | string | — | Full-text search on name/code |
| `campusId` | long | — | Filter by campus |
| `building` | string | — | Filter by building |
| `roomType` | string | — | Filter by room type |
| `minCapacity` | int | — | Minimum capacity filter |
| `equipmentTags` | string | — | Comma-separated tags (rooms must have ALL specified) |

#### Block List Endpoint

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `status` | string | — | Filter by block status |
| `startDate` | date | — | Filter blocks starting on or after this date |
| `endDate` | date | — | Filter blocks ending on or before this date |

#### Availability Endpoint

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `startDate` | date | Yes | Start of date range |
| `endDate` | date | Yes | End of date range |

---

## 4. Data Model

### 4.1 Entity-Relationship Diagram

```
Campus (1) ──→ (N) Room
Room (1) ──→ (N) ResourceBlock
```

### 4.2 Table Definitions

#### `utms.rooms`
```sql
CREATE TABLE utms.rooms (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    campus_id       BIGINT NOT NULL,
    building        VARCHAR(100) NOT NULL,
    floor           VARCHAR(20),
    capacity        INTEGER NOT NULL CHECK (capacity >= 1 AND capacity <= 5000),
    room_type       VARCHAR(20) NOT NULL CHECK (room_type IN ('LECTURE_HALL', 'LAB', 'SEMINAR', 'AUDITORIUM', 'TUTORIAL')),
    equipment_tags  JSONB DEFAULT '[]'::jsonb,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    deleted_at      TIMESTAMP NULL,

    CONSTRAINT fk_rooms_campuses FOREIGN KEY (campus_id) REFERENCES utms.campuses(id),
    CONSTRAINT uq_rooms_code_campus UNIQUE (campus_id, code)
);

CREATE INDEX idx_rooms_campus_id ON utms.rooms(campus_id);
CREATE INDEX idx_rooms_room_type ON utms.rooms(room_type);
CREATE INDEX idx_rooms_capacity ON utms.rooms(capacity);
CREATE INDEX idx_rooms_building ON utms.rooms(campus_id, building);
CREATE INDEX idx_rooms_is_active ON utms.rooms(is_active) WHERE deleted_at IS NULL;
CREATE INDEX idx_rooms_equipment_tags ON utms.rooms USING GIN (equipment_tags);
```

#### `utms.resource_blocks`
```sql
CREATE TABLE utms.resource_blocks (
    id              BIGSERIAL PRIMARY KEY,
    room_id         BIGINT NOT NULL,
    reason          VARCHAR(500) NOT NULL,
    block_type      VARCHAR(20) NOT NULL CHECK (block_type IN ('MAINTENANCE', 'EVENT', 'RESERVATION', 'EMERGENCY')),
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    start_time      TIME NULL,  -- null means full-day block
    end_time        TIME NULL,  -- null means full-day block
    status          VARCHAR(20) NOT NULL DEFAULT 'REQUESTED' CHECK (status IN ('REQUESTED', 'APPROVED', 'ACTIVE', 'RELEASED')),
    requested_by    VARCHAR(100) NOT NULL,
    approved_by     VARCHAR(100),
    approved_at     TIMESTAMP NULL,
    released_at     TIMESTAMP NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,

    CONSTRAINT fk_blocks_rooms FOREIGN KEY (room_id) REFERENCES utms.rooms(id),
    CONSTRAINT chk_blocks_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_blocks_times CHECK (
        (start_time IS NULL AND end_time IS NULL) OR
        (start_time IS NOT NULL AND end_time IS NOT NULL AND start_time < end_time)
    )
);

CREATE INDEX idx_blocks_room_id ON utms.resource_blocks(room_id);
CREATE INDEX idx_blocks_status ON utms.resource_blocks(status);
CREATE INDEX idx_blocks_date_range ON utms.resource_blocks(room_id, start_date, end_date);
CREATE INDEX idx_blocks_active ON utms.resource_blocks(room_id, start_date, end_date) WHERE status = 'ACTIVE';
```

### 4.3 Migration Strategy

- **Tool:** Flyway
- **Naming:** `V{N}__{description}.sql`
- **Planned migrations:**
  - `V13__create_rooms_table.sql`
  - `V14__create_resource_blocks_table.sql`
- All migrations are reversible (DROP TABLE IF EXISTS in undo scripts)
- Schema: `utms` (set via Flyway `defaultSchema` config)

---

## 5. Service / Business Logic

### 5.1 Service Classes

| Service | Responsibility | Traces to |
|---------|---------------|-----------|
| `RoomService` | Room CRUD, scoped uniqueness, equipment tag validation, soft-delete with session check | FR-1.x, FR-2.x, FR-3.x |
| `ResourceBlockService` | Block lifecycle (create, approve, activate, release), impact detection, status transitions | FR-4.x |
| `RoomAvailabilityService` | Computes available time slots by subtracting blocks and sessions from room grid | FR-5.3 |

### 5.2 Key Business Rules

#### Scoped Uniqueness (FR-1.3)
```java
// In RoomService.create():
if (roomRepository.existsByCampusIdAndCodeAndDeletedAtIsNull(request.campusId(), request.code())) {
    throw new ConflictException("Room with code '" + request.code()
        + "' already exists in this campus.");
}
```

#### Soft-Delete with Published Session Check (FR-1.5)
```java
// In RoomService.delete():
boolean hasPublishedSessions = sessionRepository
    .existsByRoomIdAndTimetableStatus(roomId, TimetableStatus.PUBLISHED);
if (hasPublishedSessions) {
    throw new ConflictException("Cannot delete room: it has sessions in a published timetable.");
}
room.setDeletedAt(LocalDateTime.now());
```

#### Equipment Tag Validation (FR-3.3)
```java
// In RoomService.validateEquipmentTags():
if (tags != null && tags.size() > 20) {
    throw new ValidationException("equipmentTags", "Maximum 20 equipment tags allowed", tags.size());
}
for (String tag : tags) {
    if (!tag.matches("^[a-z][a-z0-9\\-]*$") || tag.length() > 50) {
        throw new ValidationException("equipmentTags",
            "Tags must be lowercase alphanumeric with hyphens, max 50 characters", tag);
    }
}
```

#### Resource Block Creation with Impact Detection (FR-4.4)
```java
// In ResourceBlockService.create():
public ResourceBlockDto createBlock(Long roomId, CreateResourceBlockRequest request, String userId) {
    Room room = roomRepository.findByIdAndDeletedAtIsNull(roomId)
        .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));

    ResourceBlock block = new ResourceBlock();
    block.setRoom(room);
    block.setReason(request.reason());
    block.setBlockType(request.blockType());
    block.setStartDate(request.startDate());
    block.setEndDate(request.endDate());
    block.setStartTime(request.startTime());
    block.setEndTime(request.endTime());
    block.setStatus(ResourceBlockStatus.REQUESTED);
    block.setRequestedBy(userId);

    ResourceBlock saved = blockRepository.save(block);

    // Detect impacted published sessions
    List<Session> impacted = detectImpactedSessions(roomId, request);
    if (!impacted.isEmpty()) {
        // Flag sessions for displacement; require approval before activation
        flagSessionsForDisplacement(impacted, saved.getId());
    }

    auditEventPublisher.record("RESOURCE_BLOCK", saved.getId(), "CREATE", null, saved, userId);
    return mapper.toDto(saved, impacted);
}
```

#### Block Approval Gating (FR-4.5)
```java
// In ResourceBlockService.activate():
public ResourceBlockDto activateBlock(Long roomId, Long blockId, String userId) {
    ResourceBlock block = findBlockOrThrow(roomId, blockId);

    List<Session> impacted = detectImpactedSessions(block);

    if (!impacted.isEmpty() && block.getStatus() != ResourceBlockStatus.APPROVED) {
        throw new ConflictException(
            "Cannot activate block: it impacts " + impacted.size()
            + " published session(s). Approval required first.");
    }

    if (block.getStatus() == ResourceBlockStatus.REQUESTED && impacted.isEmpty()) {
        // No impact — can skip approval and go directly to ACTIVE
        block.setStatus(ResourceBlockStatus.ACTIVE);
    } else if (block.getStatus() == ResourceBlockStatus.APPROVED) {
        block.setStatus(ResourceBlockStatus.ACTIVE);
    } else {
        throw new ValidationException("status",
            "Invalid transition: " + block.getStatus() + " → ACTIVE", block.getStatus());
    }

    block.setUpdatedBy(userId);
    ResourceBlock saved = blockRepository.save(block);
    auditEventPublisher.record("RESOURCE_BLOCK", saved.getId(), "ACTIVATE", block.getStatus(), saved.getStatus(), userId);
    return mapper.toDto(saved);
}
```

#### Block Approval (FR-4.5)
```java
// In ResourceBlockService.approve():
public ResourceBlockDto approveBlock(Long roomId, Long blockId, String userId) {
    ResourceBlock block = findBlockOrThrow(roomId, blockId);

    if (block.getStatus() != ResourceBlockStatus.REQUESTED) {
        throw new ValidationException("status",
            "Can only approve blocks in REQUESTED status", block.getStatus());
    }

    block.setStatus(ResourceBlockStatus.APPROVED);
    block.setApprovedBy(userId);
    block.setApprovedAt(LocalDateTime.now());
    block.setUpdatedBy(userId);

    ResourceBlock saved = blockRepository.save(block);
    auditEventPublisher.record("RESOURCE_BLOCK", saved.getId(), "APPROVE", "REQUESTED", "APPROVED", userId);
    return mapper.toDto(saved);
}
```

#### Block Release (FR-4.6)
```java
// In ResourceBlockService.release():
public ResourceBlockDto releaseBlock(Long roomId, Long blockId, String userId) {
    ResourceBlock block = findBlockOrThrow(roomId, blockId);

    if (block.getStatus() != ResourceBlockStatus.ACTIVE) {
        throw new ValidationException("status",
            "Can only release ACTIVE blocks", block.getStatus());
    }

    block.setStatus(ResourceBlockStatus.RELEASED);
    block.setReleasedAt(LocalDateTime.now());
    block.setUpdatedBy(userId);

    ResourceBlock saved = blockRepository.save(block);
    auditEventPublisher.record("RESOURCE_BLOCK", saved.getId(), "RELEASE", "ACTIVE", "RELEASED", userId);
    return mapper.toDto(saved);
}
```

#### Room Availability Computation (FR-5.3)
```java
// In RoomAvailabilityService.getAvailability():
public RoomAvailabilityResponse getAvailability(Long roomId, LocalDate startDate, LocalDate endDate) {
    Room room = roomRepository.findByIdAndDeletedAtIsNull(roomId)
        .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));

    // Get active blocks overlapping the date range
    List<ResourceBlock> activeBlocks = blockRepository
        .findByRoomIdAndStatusAndDateRange(roomId, ResourceBlockStatus.ACTIVE, startDate, endDate);

    // Get scheduled sessions in the date range
    List<Session> scheduledSessions = sessionRepository
        .findByRoomIdAndDateRange(roomId, startDate, endDate);

    // Compute availability per day
    List<DayAvailability> days = new ArrayList<>();
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
        days.add(computeDayAvailability(date, activeBlocks, scheduledSessions));
    }

    return new RoomAvailabilityResponse(roomId, room.getCode(), startDate, endDate, days);
}
```

### 5.3 Validation Rules (Jakarta Validation)

All validation is applied at the controller layer via `@Valid` on request DTOs. Custom validators:

| Validator | Purpose |
|-----------|---------|
| `@UniqueRoomCode` | Checks room code uniqueness within campus |
| `@ActiveParent` | Validates that referenced campus exists and is not soft-deleted |
| `@ValidDateRange` | Ensures endDate >= startDate |
| `@ValidTimeRange` | Ensures startTime < endTime (when both provided) |
| `@ValidEquipmentTags` | Tag format, count, and length validation |

### 5.4 Transaction Boundaries

- Every `create`, `update`, `delete`, `approve`, `activate`, `release` method is annotated `@Transactional`
- Audit event is written in the same transaction (via `AuditEventPublisher`)
- Read operations use `@Transactional(readOnly = true)`
- Availability computation uses `@Transactional(readOnly = true)`

---

## 6. Cross-cutting Concerns

### 6.1 Error Handling

`@RestControllerAdvice` in `com.utms.common.exception.GlobalExceptionHandler`:

| Exception | HTTP Status | Traces to |
|-----------|-------------|-----------|
| `MethodArgumentNotValidException` | 400 | Validation Rules |
| `ValidationException` (custom) | 400 | FR-3.3, FR-4.2, status transitions |
| `EntityNotFoundException` | 404 | — |
| `ConflictException` (custom) | 409 | FR-1.3, FR-1.5, FR-4.5 |
| `AccessDeniedException` | 403 | NFR Security |
| `Exception` (catch-all) | 500 | NFR Security (no internals exposed) |

Response format:
```json
{
  "timestamp": "2026-08-13T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Cannot activate block: it impacts 3 published session(s). Approval required first.",
  "path": "/api/v1/rooms/5/blocks/12/activate",
  "details": []
}
```

### 6.2 Security

- **Authentication:** JWT extracted via Spring Security filter; user ID and roles in claims.
- **Authorization:** `@PreAuthorize` on controller methods:
  - Room CRUD: `hasRole('ADMIN')`
  - Block creation: `hasAnyRole('ADMIN', 'COORDINATOR', 'FACILITIES')`
  - Block approval: `hasAnyRole('ADMIN', 'REGISTRAR')`
  - Block activation: `hasRole('ADMIN')`
  - Block release: `hasAnyRole('ADMIN', 'FACILITIES')`
  - Read operations: `isAuthenticated()`
- **Data Segregation (RLS):** Repository queries include campus scope derived from the authenticated user's JWT claims. Coordinators see only rooms in their campus.
- **Input Sanitization:** All string inputs trimmed; code field forced to uppercase; no HTML allowed in any field.

### 6.3 Audit Trail

Every mutation (create, update, soft-delete, status transitions) triggers an audit event stored in the `audit_events` table within the same transaction:

```java
auditEventPublisher.record("ROOM", room.getId(), "CREATE",
    null, mapper.toDto(room), currentUserId);

auditEventPublisher.record("RESOURCE_BLOCK", block.getId(), "APPROVE",
    "REQUESTED", "APPROVED", currentUserId);
```

Fields: `entity_type`, `entity_id`, `action` (CREATE/UPDATE/DELETE/APPROVE/ACTIVATE/RELEASE), `previous_value` (JSON), `new_value` (JSON), `user_id`, `timestamp`.

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
    room:
      max-equipment-tags: 20
      availability-max-range-days: 90
  ```

### 6.5 Logging

- SLF4J + Logback with structured JSON in deployed environments
- Log: entity type, entity ID, operation, user ID, request ID, block status transitions
- Never log: passwords, tokens, full request bodies with PII
- Security events (access denials, validation failures) logged at WARN level
- Block status transitions logged at INFO level for operational visibility

---

## 7. Non-Functional Design

| NFR | How It's Met |
|-----|-------------|
| Performance (< 200ms single CRUD) | Direct JPA queries with indexes on FKs, code, room_type, capacity; GIN index on equipment_tags; no N+1 |
| Performance (< 500ms availability) | Indexed date-range queries on blocks and sessions; computation in application layer |
| Security (RBAC) | `@PreAuthorize` annotations on every endpoint; role hierarchy configured in Spring Security |
| Security (RLS / Data Segregation) | Repository methods include campus filter from SecurityContext |
| Input Validation (allowlist) | Jakarta Validation + `@Pattern` for code format; reject unknown fields via Jackson `FAIL_ON_UNKNOWN_PROPERTIES` |
| Parameterized Queries | Spring Data JPA method queries and `@Query` with named parameters — no string concatenation |
| Audit (same transaction) | Audit events persisted within the `@Transactional` boundary of the mutation |

---

## 8. Testing Strategy

### 8.1 Unit Tests (JUnit 5 + Mockito)

| Layer | What to Test |
|-------|-------------|
| RoomService | Scoped uniqueness (same code different campus → OK), soft-delete with session check, campus FK validation, equipment tag validation |
| ResourceBlockService | Status transitions (all valid paths), approval gating (block with impacts requires approval), release from ACTIVE only, invalid transitions rejected |
| RoomAvailabilityService | Availability computation with blocks and sessions subtracted; full-day blocks; partial-day blocks; overlapping blocks |
| Mapper | DTO ↔ Entity mapping correctness, impacted session inclusion |

Naming: `methodName_scenario_expectedResult()`

Examples:
```java
@Test
void create_roomWithDuplicateCodeInSameCampus_throwsConflictException() { ... }

@Test
void create_roomWithSameCodeInDifferentCampus_succeeds() { ... }

@Test
void delete_roomWithPublishedSessions_throwsConflictException() { ... }

@Test
void activate_blockWithImpactedSessionsWithoutApproval_throwsConflictException() { ... }

@Test
void activate_blockWithNoImpactedSessions_skipsApproval() { ... }

@Test
void release_blockInRequestedStatus_throwsValidationException() { ... }

@Test
void getAvailability_withActiveBlockAndSessions_excludesBoth() { ... }

@Test
void createBlock_impactsPublishedSessions_flagsForDisplacement() { ... }
```

### 8.2 Integration Tests (Testcontainers + Spring Boot Test)

| Scenario | Covers |
|----------|--------|
| Room CRUD happy path | FR-1.1, AC-1 |
| Create with invalid campus FK | FR-1.2, AC-2 |
| Duplicate code within same campus | FR-1.3, AC-3 |
| Block raised on room with published sessions → flags displacement | FR-4.4, AC-4 |
| Engine excludes room with ACTIVE block | FR-4.3, AC-5 |
| Availability query returns free slots | FR-5.3, AC-6 |
| Equipment tag filtering | FR-3.2, AC-7 |
| Block lifecycle: REQUESTED → APPROVED → ACTIVE → RELEASED | FR-4.x |
| Approval gating enforcement | FR-4.5 |
| Campus-scoped listing (RLS) | NFR Data Segregation |
| Pagination, filtering, sorting | FR-5.1, FR-5.2 |
| Unauthenticated access → 401 | NFR Security |
| Unauthorized block approval → 403 | NFR Security |

### 8.3 Coverage Target

- 80%+ line coverage on new code (enforced by JaCoCo)

---

## 9. Requirement Traceability

| Requirement | Design Element(s) |
|-------------|-------------------|
| FR-1.1 | RoomController CRUD endpoints, RoomService |
| FR-1.2 | Room entity fields, `CreateRoomRequest` DTO, `RoomType` enum |
| FR-1.3 | `uq_rooms_code_campus` composite unique constraint, uniqueness check in service |
| FR-1.4 | Soft-delete via `deleted_at`, `RoomService.delete()` |
| FR-1.5 | Published session check in `RoomService.delete()` |
| FR-2.1 | `capacity` column with CHECK constraint |
| FR-2.2 | Scheduling engine reads capacity for section-strength matching |
| FR-2.3 | `room_type` column used by engine for session-type compatibility |
| FR-3.1 | `equipment_tags` JSONB column |
| FR-3.2 | GIN index on equipment_tags; scheduling engine matches course→room tags |
| FR-3.3 | `ValidEquipmentTags` validator (max 20 items, lowercase, max 50 chars) |
| FR-4.1 | `resource_blocks` table, ResourceBlockController endpoints |
| FR-4.2 | Block entity fields, `CreateResourceBlockRequest` DTO, `ResourceBlockStatus` enum |
| FR-4.3 | ACTIVE status prevents scheduling; engine checks block status |
| FR-4.4 | Impact detection in `ResourceBlockService.create()`, session flagging |
| FR-4.5 | Approval gating in `ResourceBlockService.activate()` |
| FR-4.6 | Release endpoint transitions ACTIVE → RELEASED |
| FR-5.1 | Paginated list endpoint with `PagedResponse` wrapper |
| FR-5.2 | Query parameter filters: `campusId`, `building`, `roomType`, `minCapacity`, `equipmentTags`, `isActive` |
| FR-5.3 | `RoomAvailabilityService.getAvailability()` endpoint |
| FR-5.4 | Block list filtering by `status`, `startDate`, `endDate` |
| NFR Performance | Indexed queries (GIN for tags, composite for date ranges), pagination, < 200ms / < 500ms targets |
| NFR Security | JWT auth, `@PreAuthorize`, RLS scoping |
| NFR Input Validation | Jakarta Validation, allowlist patterns, sanitized strings |
| NFR Parameterized Queries | Spring Data JPA (no raw concatenation) |
| NFR Audit | `AuditEventPublisher` in same transaction for all mutations and transitions |
| NFR Data Segregation | Repository-level campus filtering from SecurityContext |

---

## 10. Open Questions

| # | Question | Owner | Status |
|---|----------|-------|--------|
| 1 | Should room equipment tags be a controlled vocabulary (predefined list) or free-form? Current design: free-form with format validation. | Product Owner | Open |
| 2 | How far in advance can resource blocks be raised? Current design: no limit beyond "today or future". | Product Owner | Open |
| 3 | Should room capacity include a "usable capacity" vs "max capacity" distinction (e.g., for social distancing)? | Product Owner | Open |
| 4 | Should block approval auto-trigger session displacement, or should displacement be a separate manual step? Current design: flags for displacement, manual resolution. | Tech Lead | Open |
| 5 | Should the availability endpoint support campus-wide queries (all rooms in a campus for a date range) for bulk scheduling? | Tech Lead | Open |
| 6 | Should EMERGENCY block type bypass approval gating and go directly to ACTIVE? | Product Owner | Open |
