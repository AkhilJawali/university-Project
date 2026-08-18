package com.utms.masterdata.academiccalendar;

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
@RequestMapping("/api/v1/academic-calendars")
@RequiredArgsConstructor
@Tag(name = "Academic Calendars", description = "Academic calendar management")
public class AcademicCalendarController {

    private final AcademicCalendarService calendarService;

    @GetMapping
    @Operation(summary = "List academic calendars with pagination and filtering")
    public ResponseEntity<PagedResponse<AcademicCalendarDto>> list(
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) SemesterType semesterType,
            Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(
                calendarService.findAll(campusId, academicYear, semesterType, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single academic calendar by ID")
    public ResponseEntity<AcademicCalendarDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(calendarService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Create a new academic calendar")
    public ResponseEntity<CreateResponse<AcademicCalendarDto>> create(
            @Valid @RequestBody CreateAcademicCalendarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(calendarService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Update an existing academic calendar")
    public ResponseEntity<AcademicCalendarDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateAcademicCalendarRequest request) {
        return ResponseEntity.ok(calendarService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete an academic calendar")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        calendarService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
