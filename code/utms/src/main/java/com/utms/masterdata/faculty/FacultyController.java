package com.utms.masterdata.faculty;

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

import java.util.List;

@RestController
@RequestMapping("/api/v1/faculty")
@RequiredArgsConstructor
@Tag(name = "Faculty", description = "Faculty master data management")
public class FacultyController {

    private final FacultyService facultyService;

    // ==================== Faculty CRUD ====================

    @GetMapping
    @Operation(summary = "List faculty with pagination and filtering")
    public ResponseEntity<PagedResponse<FacultyDto>> list(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Cadre cadre,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(PagedResponse.from(facultyService.search(search, pageable)));
        }
        if (departmentId != null && cadre != null) {
            return ResponseEntity.ok(PagedResponse.from(
                    facultyService.findByDepartmentIdAndCadre(departmentId, cadre, pageable)));
        }
        if (departmentId != null) {
            return ResponseEntity.ok(PagedResponse.from(facultyService.findByDepartmentId(departmentId, pageable)));
        }
        if (cadre != null) {
            return ResponseEntity.ok(PagedResponse.from(facultyService.findByCadre(cadre, pageable)));
        }
        return ResponseEntity.ok(PagedResponse.from(facultyService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single faculty member by ID")
    public ResponseEntity<FacultyDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(facultyService.findById(id));
    }

    @GetMapping("/{id}/full")
    @Operation(summary = "Get full faculty profile including availability, competencies, and campus associations")
    public ResponseEntity<FacultyFullProfileDto> getFullProfile(@PathVariable Long id) {
        return ResponseEntity.ok(facultyService.getFullProfile(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Create a new faculty member")
    public ResponseEntity<CreateResponse<FacultyDto>> create(@Valid @RequestBody CreateFacultyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facultyService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Update an existing faculty member")
    public ResponseEntity<CreateResponse<FacultyDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateFacultyRequest request) {
        return ResponseEntity.ok(facultyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a faculty member")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        facultyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Availability ====================

    @GetMapping("/{id}/availability")
    @Operation(summary = "Get all availability windows for a faculty member")
    public ResponseEntity<List<FacultyAvailabilityDto>> getAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(facultyService.getAvailabilities(id));
    }

    @PostMapping("/{id}/availability")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'COORDINATOR', 'FACULTY')")
    @Operation(summary = "Add an availability window for a faculty member")
    public ResponseEntity<FacultyAvailabilityDto> addAvailability(
            @PathVariable Long id,
            @Valid @RequestBody CreateAvailabilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facultyService.addAvailability(id, request));
    }

    @DeleteMapping("/{id}/availability/{availId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'COORDINATOR', 'FACULTY')")
    @Operation(summary = "Remove an availability window")
    public ResponseEntity<Void> removeAvailability(
            @PathVariable Long id,
            @PathVariable Long availId) {
        facultyService.removeAvailability(id, availId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Competencies ====================

    @PostMapping("/{id}/competencies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Add a course competency to a faculty member")
    public ResponseEntity<FacultyFullProfileDto.CompetencyDto> addCompetency(
            @PathVariable Long id,
            @Valid @RequestBody CreateCompetencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facultyService.addCompetency(id, request.getCourseId()));
    }

    @DeleteMapping("/{id}/competencies/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Remove a course competency from a faculty member")
    public ResponseEntity<Void> removeCompetency(
            @PathVariable Long id,
            @PathVariable Long compId) {
        facultyService.removeCompetency(id, compId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Campus Associations ====================

    @PostMapping("/{id}/campus-associations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Add a campus association to a faculty member")
    public ResponseEntity<FacultyFullProfileDto.CampusAssociationDto> addCampusAssociation(
            @PathVariable Long id,
            @Valid @RequestBody CreateCampusAssociationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(facultyService.addCampusAssociation(id, request.getCampusId(), request.getTravelTimeMinutes()));
    }

    @DeleteMapping("/{id}/campus-associations/{assocId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Remove a campus association from a faculty member")
    public ResponseEntity<Void> removeCampusAssociation(
            @PathVariable Long id,
            @PathVariable Long assocId) {
        facultyService.removeCampusAssociation(id, assocId);
        return ResponseEntity.noContent().build();
    }
}
