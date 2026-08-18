package com.utms.masterdata.timeslot;

import com.utms.common.dto.CreateResponse;
import com.utms.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping("/api/v1/time-slot-grids")
@RequiredArgsConstructor
@Tag(name = "Time-Slot Grids", description = "Time-slot grid management")
public class TimeSlotGridController {

    private final TimeSlotGridService gridService;

    @GetMapping
    @Operation(summary = "List time-slot grids with pagination and filtering")
    public ResponseEntity<PagedResponse<TimeSlotGridDto>> list(
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) Boolean isActive,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(gridService.findAll(campusId, isActive, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single time-slot grid by ID with slots and working days")
    public ResponseEntity<TimeSlotGridDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(gridService.findById(id));
    }

    @GetMapping("/campus/{campusId}/active")
    @Operation(summary = "Get the active time-slot grid for a campus")
    public ResponseEntity<TimeSlotGridDto> getActiveByCampus(@PathVariable Long campusId) {
        return ResponseEntity.ok(gridService.findActiveByCampusId(campusId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Create a new time-slot grid")
    public ResponseEntity<CreateResponse<TimeSlotGridDto>> create(
            @Valid @RequestBody CreateTimeSlotGridRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gridService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Update an existing time-slot grid")
    public ResponseEntity<TimeSlotGridDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateTimeSlotGridRequest request) {
        return ResponseEntity.ok(gridService.update(id, request));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Activate a time-slot grid (deactivates others for same campus)")
    public ResponseEntity<TimeSlotGridDto> activate(@PathVariable Long id) {
        return ResponseEntity.ok(gridService.activate(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a time-slot grid")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        gridService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
