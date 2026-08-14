package com.utms.masterdata.program;

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
@RequestMapping("/api/v1/programs")
@RequiredArgsConstructor
@Tag(name = "Programs", description = "Program master data management")
public class ProgramController {

    private final ProgramService programService;

    @GetMapping
    @Operation(summary = "List programs with pagination and filtering")
    public ResponseEntity<PagedResponse<ProgramDto>> list(
            @RequestParam(required = false) Long departmentId,
            Pageable pageable) {
        if (departmentId != null) {
            return ResponseEntity.ok(PagedResponse.from(programService.findByDepartmentId(departmentId, pageable)));
        }
        return ResponseEntity.ok(PagedResponse.from(programService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single program by ID")
    public ResponseEntity<ProgramDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(programService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Create a new program")
    public ResponseEntity<CreateResponse<ProgramDto>> create(@Valid @RequestBody CreateProgramRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Update an existing program")
    public ResponseEntity<ProgramDto> update(@PathVariable Long id, @Valid @RequestBody CreateProgramRequest request) {
        return ResponseEntity.ok(programService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a program")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        programService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
