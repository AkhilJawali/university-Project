package com.utms.masterdata.timeslot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/time-slot-grids/{gridId}/slots")
@RequiredArgsConstructor
@Tag(name = "Slot Definitions", description = "Slot definition management (sub-resource of Time-Slot Grid)")
public class SlotDefinitionController {

    private final SlotDefinitionService slotDefinitionService;

    @GetMapping
    @Operation(summary = "List all slot definitions for a time-slot grid")
    public ResponseEntity<List<SlotDefinitionDto>> list(@PathVariable Long gridId) {
        return ResponseEntity.ok(slotDefinitionService.findByGridId(gridId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Add a slot definition to a time-slot grid")
    public ResponseEntity<SlotDefinitionDto> create(
            @PathVariable Long gridId,
            @Valid @RequestBody CreateSlotDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slotDefinitionService.create(gridId, request));
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Bulk create slot definitions for a time-slot grid")
    public ResponseEntity<List<SlotDefinitionDto>> bulkCreate(
            @PathVariable Long gridId,
            @Valid @RequestBody List<CreateSlotDefinitionRequest> slots) {
        return ResponseEntity.status(HttpStatus.CREATED).body(slotDefinitionService.bulkCreate(gridId, slots));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Update an existing slot definition")
    public ResponseEntity<SlotDefinitionDto> update(
            @PathVariable Long gridId,
            @PathVariable Long id,
            @Valid @RequestBody CreateSlotDefinitionRequest request) {
        return ResponseEntity.ok(slotDefinitionService.update(gridId, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Remove a slot definition from a time-slot grid")
    public ResponseEntity<Void> delete(@PathVariable Long gridId, @PathVariable Long id) {
        slotDefinitionService.delete(gridId, id);
        return ResponseEntity.noContent().build();
    }
}
