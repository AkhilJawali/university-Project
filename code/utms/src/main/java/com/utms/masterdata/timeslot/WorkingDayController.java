package com.utms.masterdata.timeslot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/time-slot-grids/{gridId}/working-days")
@RequiredArgsConstructor
@Tag(name = "Working Days", description = "Working day configuration (sub-resource of Time-Slot Grid)")
public class WorkingDayController {

    private final WorkingDayService workingDayService;

    @GetMapping
    @Operation(summary = "Get working day configuration for a time-slot grid")
    public ResponseEntity<List<WorkingDayDto>> list(@PathVariable Long gridId) {
        return ResponseEntity.ok(workingDayService.findByGridId(gridId));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Update working day configuration for a time-slot grid")
    public ResponseEntity<List<WorkingDayDto>> update(
            @PathVariable Long gridId,
            @Valid @RequestBody UpdateWorkingDaysRequest request) {
        return ResponseEntity.ok(workingDayService.updateWorkingDays(gridId, request));
    }
}
