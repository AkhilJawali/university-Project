package com.utms.masterdata.campus;

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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/campuses")
@RequiredArgsConstructor
@Tag(name = "Campuses", description = "Campus master data management")
public class CampusController {

    private final CampusService campusService;
    private final HierarchyService hierarchyService;

    @GetMapping
    @Operation(summary = "List campuses with pagination and optional search")
    public ResponseEntity<PagedResponse<CampusDto>> list(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(PagedResponse.from(campusService.search(search, pageable)));
        }
        return ResponseEntity.ok(PagedResponse.from(campusService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single campus by ID")
    public ResponseEntity<CampusDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(campusService.findById(id));
    }

    @GetMapping("/{id}/hierarchy")
    @Operation(summary = "Get full nested hierarchy tree for a campus")
    public ResponseEntity<Map<String, Object>> getHierarchy(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("data", hierarchyService.getHierarchyTree(id)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Create a new campus")
    public ResponseEntity<CreateResponse<CampusDto>> create(@Valid @RequestBody CreateCampusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campusService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    @Operation(summary = "Update an existing campus")
    public ResponseEntity<CampusDto> update(@PathVariable Long id, @Valid @RequestBody UpdateCampusRequest request) {
        return ResponseEntity.ok(campusService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a campus")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        campusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
