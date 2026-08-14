package com.utms.masterdata.section;

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
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
@Tag(name = "Sections", description = "Section master data management")
public class SectionController {

    private final SectionService sectionService;

    @GetMapping
    @Operation(summary = "List sections with pagination and filtering")
    public ResponseEntity<PagedResponse<SectionDto>> list(
            @RequestParam(required = false) Long batchId,
            Pageable pageable) {
        if (batchId != null) {
            return ResponseEntity.ok(PagedResponse.from(sectionService.findByBatchId(batchId, pageable)));
        }
        return ResponseEntity.ok(PagedResponse.from(sectionService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single section by ID")
    public ResponseEntity<SectionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sectionService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'COORDINATOR')")
    @Operation(summary = "Create a new section")
    public ResponseEntity<CreateResponse<SectionDto>> create(@Valid @RequestBody CreateSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'COORDINATOR')")
    @Operation(summary = "Update an existing section")
    public ResponseEntity<CreateResponse<SectionDto>> update(@PathVariable Long id, @Valid @RequestBody CreateSectionRequest request) {
        return ResponseEntity.ok(sectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a section")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
