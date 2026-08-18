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
@RequestMapping("/api/v1/academic-calendars/{calendarId}/holidays")
@RequiredArgsConstructor
@Tag(name = "Holidays", description = "Holiday management (sub-resource of Academic Calendar)")
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    @Operation(summary = "List all holidays for an academic calendar")
    public ResponseEntity<List<HolidayDto>> list(@PathVariable Long calendarId) {
        return ResponseEntity.ok(holidayService.findByCalendarId(calendarId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single holiday by ID")
    public ResponseEntity<HolidayDto> getById(@PathVariable Long calendarId, @PathVariable Long id) {
        return ResponseEntity.ok(holidayService.findById(calendarId, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Add a holiday to an academic calendar")
    public ResponseEntity<HolidayDto> create(
            @PathVariable Long calendarId,
            @Valid @RequestBody CreateHolidayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayService.create(calendarId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Update an existing holiday")
    public ResponseEntity<HolidayDto> update(
            @PathVariable Long calendarId,
            @PathVariable Long id,
            @Valid @RequestBody CreateHolidayRequest request) {
        return ResponseEntity.ok(holidayService.update(calendarId, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Remove a holiday from an academic calendar")
    public ResponseEntity<Void> delete(@PathVariable Long calendarId, @PathVariable Long id) {
        holidayService.delete(calendarId, id);
        return ResponseEntity.noContent().build();
    }
}
