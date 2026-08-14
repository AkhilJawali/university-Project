package com.utms.masterdata.faculty;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.masterdata.course.Course;
import com.utms.masterdata.course.CourseRepository;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final FacultyAvailabilityRepository availabilityRepository;
    private final FacultyCompetencyRepository competencyRepository;
    private final FacultyCampusAssociationRepository campusAssociationRepository;
    private final WorkloadConfigRepository workloadConfigRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final CampusRepository campusRepository;
    private final FacultyMapper facultyMapper;

    // ==================== Faculty CRUD ====================

    @Transactional(readOnly = true)
    public Page<FacultyDto> findAll(Pageable pageable) {
        return facultyRepository.findAllByDeletedAtIsNull(pageable)
                .map(facultyMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<FacultyDto> findByDepartmentId(Long departmentId, Pageable pageable) {
        return facultyRepository.findAllByDepartmentIdAndDeletedAtIsNull(departmentId, pageable)
                .map(facultyMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<FacultyDto> findByCadre(Cadre cadre, Pageable pageable) {
        return facultyRepository.findAllByCadreAndDeletedAtIsNull(cadre, pageable)
                .map(facultyMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<FacultyDto> findByDepartmentIdAndCadre(Long departmentId, Cadre cadre, Pageable pageable) {
        return facultyRepository.findAllByDepartmentIdAndCadreAndDeletedAtIsNull(departmentId, cadre, pageable)
                .map(facultyMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<FacultyDto> search(String search, Pageable pageable) {
        return facultyRepository.searchByNameOrEmailOrEmployeeId(search, pageable)
                .map(facultyMapper::toDto);
    }

    @Transactional(readOnly = true)
    public FacultyDto findById(Long id) {
        Faculty faculty = facultyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", id));
        return facultyMapper.toDto(faculty);
    }

    @Transactional
    public CreateResponse<FacultyDto> create(CreateFacultyRequest request) {
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(request.getDepartmentId())
                .orElseThrow(() -> new ValidationException("departmentId",
                        "Department not found or has been deleted", request.getDepartmentId()));

        if (facultyRepository.existsByEmployeeIdAndDeletedAtIsNull(request.getEmployeeId())) {
            throw new ConflictException("Faculty with employee ID '" + request.getEmployeeId() + "' already exists");
        }

        if (facultyRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new ConflictException("Faculty with email '" + request.getEmail() + "' already exists");
        }

        Faculty faculty = facultyMapper.toEntity(request);
        faculty.setDepartment(department);
        faculty.setIsActive(true);
        Faculty saved = facultyRepository.save(faculty);

        log.info("Faculty created: id={}, employeeId={}, departmentId={}", saved.getId(), saved.getEmployeeId(), department.getId());
        return new CreateResponse<>(facultyMapper.toDto(saved));
    }

    @Transactional
    public CreateResponse<FacultyDto> update(Long id, CreateFacultyRequest request) {
        Faculty faculty = facultyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", id));

        Department department = departmentRepository.findByIdAndDeletedAtIsNull(request.getDepartmentId())
                .orElseThrow(() -> new ValidationException("departmentId",
                        "Department not found or has been deleted", request.getDepartmentId()));

        if (facultyRepository.existsByEmployeeIdAndIdNotAndDeletedAtIsNull(request.getEmployeeId(), id)) {
            throw new ConflictException("Faculty with employee ID '" + request.getEmployeeId() + "' already exists");
        }

        if (facultyRepository.existsByEmailAndIdNotAndDeletedAtIsNull(request.getEmail(), id)) {
            throw new ConflictException("Faculty with email '" + request.getEmail() + "' already exists");
        }

        facultyMapper.updateEntity(request, faculty);
        faculty.setDepartment(department);
        Faculty saved = facultyRepository.save(faculty);

        log.info("Faculty updated: id={}, employeeId={}", saved.getId(), saved.getEmployeeId());
        return new CreateResponse<>(facultyMapper.toDto(saved));
    }

    @Transactional
    public void delete(Long id) {
        Faculty faculty = facultyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", id));

        faculty.softDelete();
        facultyRepository.save(faculty);
        log.info("Faculty soft-deleted: id={}, employeeId={}", id, faculty.getEmployeeId());
    }

    // ==================== Availability ====================

    @Transactional(readOnly = true)
    public List<FacultyAvailabilityDto> getAvailabilities(Long facultyId) {
        facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        return availabilityRepository.findAllByFacultyIdAndDeletedAtIsNull(facultyId)
                .stream()
                .map(facultyMapper::toAvailabilityDto)
                .toList();
    }

    @Transactional
    public FacultyAvailabilityDto addAvailability(Long facultyId, CreateAvailabilityRequest request) {
        Faculty faculty = facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new ValidationException("endTime", "End time must be after start time", request.getEndTime());
        }

        List<FacultyAvailability> overlapping = availabilityRepository.findOverlapping(
                facultyId, request.getDayOfWeek(), request.getStartTime(), request.getEndTime());

        if (!overlapping.isEmpty()) {
            throw new ConflictException("Availability window overlaps with an existing window on day " + request.getDayOfWeek());
        }

        FacultyAvailability availability = new FacultyAvailability();
        availability.setFaculty(faculty);
        availability.setDayOfWeek(request.getDayOfWeek());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        availability.setConstraintType(request.getConstraintType());
        availability.setIsActive(true);

        FacultyAvailability saved = availabilityRepository.save(availability);
        log.info("Availability added: facultyId={}, day={}, {}–{}", facultyId,
                request.getDayOfWeek(), request.getStartTime(), request.getEndTime());
        return facultyMapper.toAvailabilityDto(saved);
    }

    @Transactional
    public void removeAvailability(Long facultyId, Long availabilityId) {
        facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        FacultyAvailability availability = availabilityRepository.findByIdAndDeletedAtIsNull(availabilityId)
                .orElseThrow(() -> new EntityNotFoundException("FacultyAvailability", availabilityId));

        if (!availability.getFaculty().getId().equals(facultyId)) {
            throw new ValidationException("availabilityId",
                    "Availability does not belong to the specified faculty", availabilityId);
        }

        availability.softDelete();
        availabilityRepository.save(availability);
        log.info("Availability removed: facultyId={}, availabilityId={}", facultyId, availabilityId);
    }

    // ==================== Competencies ====================

    @Transactional
    public FacultyFullProfileDto.CompetencyDto addCompetency(Long facultyId, Long courseId) {
        Faculty faculty = facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new ValidationException("courseId",
                        "Course not found or has been deleted", courseId));

        if (competencyRepository.existsByFacultyIdAndCourseIdAndDeletedAtIsNull(facultyId, courseId)) {
            throw new ConflictException("Faculty already has competency for course: " + course.getCode());
        }

        FacultyCompetency competency = new FacultyCompetency();
        competency.setFaculty(faculty);
        competency.setCourse(course);
        competency.setIsActive(true);

        FacultyCompetency saved = competencyRepository.save(competency);
        log.info("Competency added: facultyId={}, courseId={}", facultyId, courseId);

        return FacultyFullProfileDto.CompetencyDto.builder()
                .id(saved.getId())
                .courseId(course.getId())
                .courseCode(course.getCode())
                .courseName(course.getName())
                .build();
    }

    @Transactional
    public void removeCompetency(Long facultyId, Long competencyId) {
        facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        FacultyCompetency competency = competencyRepository.findByIdAndDeletedAtIsNull(competencyId)
                .orElseThrow(() -> new EntityNotFoundException("FacultyCompetency", competencyId));

        if (!competency.getFaculty().getId().equals(facultyId)) {
            throw new ValidationException("competencyId",
                    "Competency does not belong to the specified faculty", competencyId);
        }

        competency.softDelete();
        competencyRepository.save(competency);
        log.info("Competency removed: facultyId={}, competencyId={}", facultyId, competencyId);
    }

    // ==================== Campus Associations ====================

    @Transactional
    public FacultyFullProfileDto.CampusAssociationDto addCampusAssociation(Long facultyId, Long campusId, Integer travelTimeMinutes) {
        Faculty faculty = facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new ValidationException("campusId",
                        "Campus not found or has been deleted", campusId));

        if (campusAssociationRepository.existsByFacultyIdAndCampusIdAndDeletedAtIsNull(facultyId, campusId)) {
            throw new ConflictException("Faculty is already associated with campus: " + campus.getName());
        }

        if (travelTimeMinutes == null || travelTimeMinutes <= 0) {
            throw new ValidationException("travelTimeMinutes", "Travel time must be greater than 0", travelTimeMinutes);
        }

        FacultyCampusAssociation association = new FacultyCampusAssociation();
        association.setFaculty(faculty);
        association.setCampus(campus);
        association.setTravelTimeMinutes(travelTimeMinutes);
        association.setIsActive(true);

        FacultyCampusAssociation saved = campusAssociationRepository.save(association);
        log.info("Campus association added: facultyId={}, campusId={}", facultyId, campusId);

        return FacultyFullProfileDto.CampusAssociationDto.builder()
                .id(saved.getId())
                .campusId(campus.getId())
                .campusName(campus.getName())
                .travelTimeMinutes(saved.getTravelTimeMinutes())
                .build();
    }

    @Transactional
    public void removeCampusAssociation(Long facultyId, Long associationId) {
        facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        FacultyCampusAssociation association = campusAssociationRepository.findByIdAndDeletedAtIsNull(associationId)
                .orElseThrow(() -> new EntityNotFoundException("FacultyCampusAssociation", associationId));

        if (!association.getFaculty().getId().equals(facultyId)) {
            throw new ValidationException("associationId",
                    "Campus association does not belong to the specified faculty", associationId);
        }

        association.softDelete();
        campusAssociationRepository.save(association);
        log.info("Campus association removed: facultyId={}, associationId={}", facultyId, associationId);
    }

    // ==================== Full Profile ====================

    @Transactional(readOnly = true)
    public FacultyFullProfileDto getFullProfile(Long facultyId) {
        Faculty faculty = facultyRepository.findByIdAndDeletedAtIsNull(facultyId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty", facultyId));

        List<FacultyAvailabilityDto> availabilities = availabilityRepository
                .findAllByFacultyIdAndDeletedAtIsNull(facultyId)
                .stream()
                .map(facultyMapper::toAvailabilityDto)
                .toList();

        List<FacultyFullProfileDto.CompetencyDto> competencies = competencyRepository
                .findAllByFacultyIdAndDeletedAtIsNull(facultyId)
                .stream()
                .map(c -> FacultyFullProfileDto.CompetencyDto.builder()
                        .id(c.getId())
                        .courseId(c.getCourse().getId())
                        .courseCode(c.getCourse().getCode())
                        .courseName(c.getCourse().getName())
                        .build())
                .toList();

        List<FacultyFullProfileDto.CampusAssociationDto> campusAssociations = campusAssociationRepository
                .findAllByFacultyIdAndDeletedAtIsNull(facultyId)
                .stream()
                .map(a -> FacultyFullProfileDto.CampusAssociationDto.builder()
                        .id(a.getId())
                        .campusId(a.getCampus().getId())
                        .campusName(a.getCampus().getName())
                        .travelTimeMinutes(a.getTravelTimeMinutes())
                        .build())
                .toList();

        return FacultyFullProfileDto.builder()
                .faculty(facultyMapper.toDto(faculty))
                .availabilities(availabilities)
                .competencies(competencies)
                .campusAssociations(campusAssociations)
                .build();
    }
}
