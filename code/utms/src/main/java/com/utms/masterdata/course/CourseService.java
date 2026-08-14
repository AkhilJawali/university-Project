package com.utms.masterdata.course;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseMapper courseMapper;

    @Transactional(readOnly = true)
    public Page<CourseDto> findAll(Pageable pageable) {
        return courseRepository.findAllByDeletedAtIsNull(pageable)
                .map(courseMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<CourseDto> findByDepartmentId(Long departmentId, Pageable pageable) {
        return courseRepository.findAllByDepartmentIdAndDeletedAtIsNull(departmentId, pageable)
                .map(courseMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<CourseDto> findByCourseType(CourseType type, Pageable pageable) {
        return courseRepository.findAllByCourseTypeAndDeletedAtIsNull(type, pageable)
                .map(courseMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<CourseDto> search(String search, Pageable pageable) {
        return courseRepository.searchByNameOrCode(search, pageable)
                .map(courseMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CourseDto findById(Long id) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));
        return courseMapper.toDto(course);
    }

    @Transactional
    public CreateResponse<CourseDto> create(CreateCourseRequest request) {
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(request.getDepartmentId())
                .orElseThrow(() -> new ValidationException("departmentId",
                        "Department not found or has been deleted", request.getDepartmentId()));

        if (courseRepository.existsByCodeAndDeletedAtIsNull(request.getCode())) {
            throw new ConflictException("Course with code '" + request.getCode() + "' already exists");
        }

        validateLtp(request);
        validatePrerequisites(request.getPrerequisites(), null);
        validateEquipmentTags(request.getEquipmentTags());

        Course course = courseMapper.toEntity(request);
        course.setDepartment(department);
        course.setIsActive(true);
        if (course.getIsCrossListed() == null) course.setIsCrossListed(false);
        Course saved = courseRepository.save(course);

        List<String> warnings = checkCreditMismatch(request);

        log.info("Course created: id={}, code={}, departmentId={}", saved.getId(), saved.getCode(), department.getId());
        return new CreateResponse<>(courseMapper.toDto(saved), warnings);
    }

    @Transactional
    public CreateResponse<CourseDto> update(Long id, CreateCourseRequest request) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));

        Department department = departmentRepository.findByIdAndDeletedAtIsNull(request.getDepartmentId())
                .orElseThrow(() -> new ValidationException("departmentId",
                        "Department not found or has been deleted", request.getDepartmentId()));

        if (courseRepository.existsByCodeAndIdNotAndDeletedAtIsNull(request.getCode(), id)) {
            throw new ConflictException("Course with code '" + request.getCode() + "' already exists");
        }

        validateLtp(request);
        validatePrerequisites(request.getPrerequisites(), id);
        validateEquipmentTags(request.getEquipmentTags());

        courseMapper.updateEntity(request, course);
        course.setDepartment(department);
        Course saved = courseRepository.save(course);

        List<String> warnings = checkCreditMismatch(request);

        log.info("Course updated: id={}, code={}", saved.getId(), saved.getCode());
        return new CreateResponse<>(courseMapper.toDto(saved), warnings);
    }

    @Transactional
    public void delete(Long id) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Course", id));

        course.softDelete();
        courseRepository.save(course);
        log.info("Course soft-deleted: id={}", id);
    }

    private void validateLtp(CreateCourseRequest request) {
        int total = request.getLectureHours() + request.getTutorialHours() + request.getPracticalHours();
        if (total == 0) {
            throw new ValidationException("lectureHours",
                    "At least one of L, T, P must be greater than 0", 0);
        }
    }

    private void validatePrerequisites(List<Long> prerequisites, Long currentCourseId) {
        if (prerequisites == null || prerequisites.isEmpty()) return;

        for (Long prereqId : prerequisites) {
            if (currentCourseId != null && prereqId.equals(currentCourseId)) {
                throw new ValidationException("prerequisites",
                        "A course cannot be a prerequisite of itself", prereqId);
            }

            Course prereq = courseRepository.findByIdAndDeletedAtIsNull(prereqId)
                    .orElseThrow(() -> new ValidationException("prerequisites",
                            "Prerequisite course not found: " + prereqId, prereqId));

            // Check for circular dependency
            if (currentCourseId != null && hasCircularDependency(prereq, currentCourseId, new HashSet<>())) {
                throw new ValidationException("prerequisites",
                        "Circular prerequisite dependency detected involving course: " + prereqId, prereqId);
            }
        }
    }

    private boolean hasCircularDependency(Course course, Long targetId, Set<Long> visited) {
        if (course.getPrerequisites() == null || course.getPrerequisites().isEmpty()) {
            return false;
        }
        for (Long prereqId : course.getPrerequisites()) {
            if (prereqId.equals(targetId)) return true;
            if (visited.contains(prereqId)) continue;
            visited.add(prereqId);
            Course prereq = courseRepository.findByIdAndDeletedAtIsNull(prereqId).orElse(null);
            if (prereq != null && hasCircularDependency(prereq, targetId, visited)) {
                return true;
            }
        }
        return false;
    }

    private void validateEquipmentTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        if (tags.size() > 10) {
            throw new ValidationException("equipmentTags", "Maximum 10 equipment tags allowed", tags.size());
        }
        for (String tag : tags) {
            if (tag == null || tag.isBlank() || tag.length() > 50) {
                throw new ValidationException("equipmentTags",
                        "Each tag must be non-empty and max 50 characters", tag);
            }
            if (!tag.equals(tag.toLowerCase())) {
                throw new ValidationException("equipmentTags",
                        "Equipment tags must be lowercase", tag);
            }
        }
    }

    private List<String> checkCreditMismatch(CreateCourseRequest request) {
        List<String> warnings = new ArrayList<>();
        int ltpTotal = request.getLectureHours() + request.getTutorialHours() + request.getPracticalHours();
        if (request.getCreditHours() != ltpTotal) {
            warnings.add(String.format(
                    "Credit hours (%d) does not equal L+T+P total (%d). This may be intentional but please verify.",
                    request.getCreditHours(), ltpTotal));
        }
        return warnings;
    }
}
