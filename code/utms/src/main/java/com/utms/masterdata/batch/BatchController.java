package com.utms.masterdata.batch;

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
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
@Tag(name = "Batches", description = "Batch master data management")
public class BatchController {

    private final BatchService batchService;

    @GetMapping
    @Operation(summary = "List batches with pagination and filtering")
    public ResponseEntity<PagedResponse<BatchDto>> list(
            @RequestParam(required = false) Long programId,
            Pageable pageable) {
        if (programId != null) {
            return ResponseEntity.ok(PagedResponse.from(batchService.findByProgramId(programId, pageable)));
        }
        return ResponseEntity.ok(PagedResponse.from(batchService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single batch by ID")
    public ResponseEntity<BatchDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(batchService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'COORDINATOR')")
    @Operation(summary = "Create a new batch")
    public ResponseEntity<CreateResponse<BatchDto>> create(@Valid @RequestBody CreateBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'COORDINATOR')")
    @Operation(summary = "Update an existing batch")
    public ResponseEntity<BatchDto> update(@PathVariable Long id, @Valid @RequestBody CreateBatchRequest request) {
        return ResponseEntity.ok(batchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a batch")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        batchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
