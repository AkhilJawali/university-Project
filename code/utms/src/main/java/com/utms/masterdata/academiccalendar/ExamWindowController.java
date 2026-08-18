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
@RequestMapping("/api/v1/academic-calendars/{calendarId}/exam-windows")
@RequiredArgsConstructor
@Tag(name = "Exam Windows", description = "Exam window management (sub-resource of Academic Calendar)")
public class ExamWindowController {

    private final ExamWindowService examWindowService;

    @GetMapping
    @Operation(summary = "List all exam windows for an academic calendar")
    public ResponseEntity<List<ExamWindowDto>> list(@PathVariable Long calendarId) {
        return ResponseEntity.ok(examWindowService.findByCalendarId(calendarId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single exam window by ID")
    public ResponseEntity<ExamWindowDto> getById(@PathVariable Long calendarId, @PathVariable Long id) {
        return ResponseEntity.ok(examWindowService.findById(calendarId, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Add an exam window to an academic calendar")
    public ResponseEntity<ExamWindowDto> create(
            @PathVariable Long calendarId,
            @Valid @RequestBody CreateExamWindowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examWindowService.create(calendarId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Update an existing exam window")
    public ResponseEntity<ExamWindowDto> update(
            @PathVariable Long calendarId,
            @PathVariable Long id,
            @Valid @RequestBody CreateExamWindowRequest request) {
        return ResponseEntity.ok(examWindowService.update(calendarId, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Remove an exam window from an academic calendar")
    public ResponseEntity<Void> delete(@PathVariable Long calendarId, @PathVariable Long id) {
        examWindowService.delete(calendarId, id);
        return ResponseEntity.noContent().build();
    }
}
