package com.utms.masterdata.course;

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
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course/Subject master data management")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "List courses with pagination and filtering")
    public ResponseEntity<PagedResponse<CourseDto>> list(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) CourseType courseType,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(PagedResponse.from(courseService.search(search, pageable)));
        }
        if (departmentId != null) {
            return ResponseEntity.ok(PagedResponse.from(courseService.findByDepartmentId(departmentId, pageable)));
        }
        if (courseType != null) {
            return ResponseEntity.ok(PagedResponse.from(courseService.findByCourseType(courseType, pageable)));
        }
        return ResponseEntity.ok(PagedResponse.from(courseService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single course by ID")
    public ResponseEntity<CourseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Create a new course")
    public ResponseEntity<CreateResponse<CourseDto>> create(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Update an existing course")
    public ResponseEntity<CreateResponse<CourseDto>> update(@PathVariable Long id, @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.ok(courseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a course")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
