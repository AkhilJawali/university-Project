package com.utms.masterdata.room;

import com.utms.common.dto.CreateResponse;
import com.utms.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room and resource management")
public class RoomController {

    private final RoomService roomService;
    private final ResourceBlockService resourceBlockService;

    @GetMapping
    @Operation(summary = "List rooms with pagination and optional filtering")
    public ResponseEntity<PagedResponse<RoomDto>> list(
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(PagedResponse.from(roomService.search(search, pageable)));
        }
        if (campusId != null || building != null || roomType != null || minCapacity != null) {
            return ResponseEntity.ok(PagedResponse.from(
                    roomService.findAllWithFilters(campusId, building, roomType, minCapacity, pageable)));
        }
        return ResponseEntity.ok(PagedResponse.from(roomService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single room by ID")
    public ResponseEntity<RoomDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Create a new room")
    public ResponseEntity<CreateResponse<RoomDto>> create(@Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Update an existing room")
    public ResponseEntity<RoomDto> update(@PathVariable Long id, @Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(roomService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a room")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Get room availability for a date range")
    public ResponseEntity<List<RoomAvailabilityDto>> getAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(resourceBlockService.getAvailability(id, startDate, endDate));
    }
}
