package com.utms.masterdata.academiccalendar;

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
@RequestMapping("/api/v1/academic-calendars/{calendarId}/special-periods")
@RequiredArgsConstructor
@Tag(name = "Special Periods", description = "Special period management (sub-resource of Academic Calendar)")
public class SpecialPeriodController {

    private final SpecialPeriodService specialPeriodService;

    @GetMapping
    @Operation(summary = "List all special periods for an academic calendar")
    public ResponseEntity<List<SpecialPeriodDto>> list(@PathVariable Long calendarId) {
        return ResponseEntity.ok(specialPeriodService.findByCalendarId(calendarId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single special period by ID")
    public ResponseEntity<SpecialPeriodDto> getById(@PathVariable Long calendarId, @PathVariable Long id) {
        return ResponseEntity.ok(specialPeriodService.findById(calendarId, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Add a special period to an academic calendar")
    public ResponseEntity<SpecialPeriodDto> create(
            @PathVariable Long calendarId,
            @Valid @RequestBody CreateSpecialPeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(specialPeriodService.create(calendarId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Update an existing special period")
    public ResponseEntity<SpecialPeriodDto> update(
            @PathVariable Long calendarId,
            @PathVariable Long id,
            @Valid @RequestBody CreateSpecialPeriodRequest request) {
        return ResponseEntity.ok(specialPeriodService.update(calendarId, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Remove a special period from an academic calendar")
    public ResponseEntity<Void> delete(@PathVariable Long calendarId, @PathVariable Long id) {
        specialPeriodService.delete(calendarId, id);
        return ResponseEntity.noContent().build();
    }
}
