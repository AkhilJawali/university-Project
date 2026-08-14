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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacultyServiceTest {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private FacultyAvailabilityRepository availabilityRepository;

    @Mock
    private FacultyCompetencyRepository competencyRepository;

    @Mock
    private FacultyCampusAssociationRepository campusAssociationRepository;

    @Mock
    private WorkloadConfigRepository workloadConfigRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private FacultyMapper facultyMapper;

    @InjectMocks
    private FacultyService facultyService;

    private Department department;
    private Faculty faculty;
    private FacultyDto facultyDto;
    private CreateFacultyRequest createRequest;
    private Campus campus;
    private Course course;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(10L);
        department.setName("Computer Science");
        department.setIsActive(true);

        faculty = new Faculty();
        faculty.setId(1L);
        faculty.setEmployeeId("EMP001");
        faculty.setFirstName("John");
        faculty.setLastName("Doe");
        faculty.setEmail("john.doe@university.edu");
        faculty.setPhone("9876543210");
        faculty.setDepartment(department);
        faculty.setCadre(Cadre.PROFESSOR);
        faculty.setQualification("Ph.D. Computer Science");
        faculty.setIsActive(true);

        facultyDto = FacultyDto.builder()
                .id(1L)
                .employeeId("EMP001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@university.edu")
                .phone("9876543210")
                .departmentId(10L)
                .departmentName("Computer Science")
                .cadre(Cadre.PROFESSOR)
                .qualification("Ph.D. Computer Science")
                .isActive(true)
                .build();

        createRequest = CreateFacultyRequest.builder()
                .employeeId("EMP001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@university.edu")
                .phone("9876543210")
                .departmentId(10L)
                .cadre(Cadre.PROFESSOR)
                .qualification("Ph.D. Computer Science")
                .build();

        campus = new Campus();
        campus.setId(5L);
        campus.setName("Main Campus");
        campus.setIsActive(true);

        course = new Course();
        course.setId(20L);
        course.setCode("CS201");
        course.setName("Algorithms");
        course.setIsActive(true);
    }

    // ==================== findById ====================

    @Test
    void findById_existingFaculty_returnsFacultyDto() {
        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(facultyMapper.toDto(faculty)).thenReturn(facultyDto);

        FacultyDto result = facultyService.findById(1L);

        assertThat(result.getEmployeeId()).isEqualTo("EMP001");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getDepartmentId()).isEqualTo(10L);
        assertThat(result.getCadre()).isEqualTo(Cadre.PROFESSOR);
    }

    @Test
    void findById_nonExistent_throwsEntityNotFoundException() {
        when(facultyRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultyService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== create ====================

    @Test
    void create_validRequest_returnsFacultyDto() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(facultyRepository.existsByEmployeeIdAndDeletedAtIsNull("EMP001")).thenReturn(false);
        when(facultyRepository.existsByEmailAndDeletedAtIsNull("john.doe@university.edu")).thenReturn(false);
        when(facultyMapper.toEntity(createRequest)).thenReturn(faculty);
        when(facultyRepository.save(any(Faculty.class))).thenReturn(faculty);
        when(facultyMapper.toDto(faculty)).thenReturn(facultyDto);

        CreateResponse<FacultyDto> result = facultyService.create(createRequest);

        assertThat(result.getData().getEmployeeId()).isEqualTo("EMP001");
        assertThat(result.getData().getEmail()).isEqualTo("john.doe@university.edu");
        verify(facultyRepository).save(any(Faculty.class));
    }

    @Test
    void create_duplicateEmployeeId_throwsConflictException() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(facultyRepository.existsByEmployeeIdAndDeletedAtIsNull("EMP001")).thenReturn(true);

        assertThatThrownBy(() -> facultyService.create(createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("EMP001");

        verify(facultyRepository, never()).save(any());
    }

    @Test
    void create_duplicateEmail_throwsConflictException() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(facultyRepository.existsByEmployeeIdAndDeletedAtIsNull("EMP001")).thenReturn(false);
        when(facultyRepository.existsByEmailAndDeletedAtIsNull("john.doe@university.edu")).thenReturn(true);

        assertThatThrownBy(() -> facultyService.create(createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("john.doe@university.edu");

        verify(facultyRepository, never()).save(any());
    }

    @Test
    void create_invalidDepartmentId_throwsValidationException() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());
        createRequest.setDepartmentId(999L);

        assertThatThrownBy(() -> facultyService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Department not found");

        verify(facultyRepository, never()).save(any());
    }

    // ==================== addAvailability ====================

    @Test
    void addAvailability_validRequest_returnsAvailabilityDto() {
        CreateAvailabilityRequest availRequest = CreateAvailabilityRequest.builder()
                .dayOfWeek(1)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .constraintType(ConstraintType.HARD_UNAVAILABLE)
                .build();

        FacultyAvailability savedAvailability = new FacultyAvailability();
        savedAvailability.setId(50L);
        savedAvailability.setFaculty(faculty);
        savedAvailability.setDayOfWeek(1);
        savedAvailability.setStartTime(LocalTime.of(9, 0));
        savedAvailability.setEndTime(LocalTime.of(11, 0));
        savedAvailability.setConstraintType(ConstraintType.HARD_UNAVAILABLE);

        FacultyAvailabilityDto availDto = FacultyAvailabilityDto.builder()
                .id(50L)
                .facultyId(1L)
                .dayOfWeek(1)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .constraintType(ConstraintType.HARD_UNAVAILABLE)
                .build();

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(availabilityRepository.findOverlapping(eq(1L), eq(1), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of());
        when(availabilityRepository.save(any(FacultyAvailability.class))).thenReturn(savedAvailability);
        when(facultyMapper.toAvailabilityDto(savedAvailability)).thenReturn(availDto);

        FacultyAvailabilityDto result = facultyService.addAvailability(1L, availRequest);

        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getDayOfWeek()).isEqualTo(1);
        assertThat(result.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.getConstraintType()).isEqualTo(ConstraintType.HARD_UNAVAILABLE);
        verify(availabilityRepository).save(any(FacultyAvailability.class));
    }

    @Test
    void addAvailability_overlappingTime_throwsConflictException() {
        CreateAvailabilityRequest availRequest = CreateAvailabilityRequest.builder()
                .dayOfWeek(1)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .constraintType(ConstraintType.HARD_UNAVAILABLE)
                .build();

        FacultyAvailability existingAvailability = new FacultyAvailability();
        existingAvailability.setId(40L);
        existingAvailability.setFaculty(faculty);
        existingAvailability.setDayOfWeek(1);
        existingAvailability.setStartTime(LocalTime.of(10, 0));
        existingAvailability.setEndTime(LocalTime.of(12, 0));

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(availabilityRepository.findOverlapping(eq(1L), eq(1), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of(existingAvailability));

        assertThatThrownBy(() -> facultyService.addAvailability(1L, availRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlaps");

        verify(availabilityRepository, never()).save(any());
    }

    // ==================== addCompetency ====================

    @Test
    void addCompetency_validCourse_succeeds() {
        FacultyCompetency savedCompetency = new FacultyCompetency();
        savedCompetency.setId(30L);
        savedCompetency.setFaculty(faculty);
        savedCompetency.setCourse(course);

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(courseRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(course));
        when(competencyRepository.existsByFacultyIdAndCourseIdAndDeletedAtIsNull(1L, 20L)).thenReturn(false);
        when(competencyRepository.save(any(FacultyCompetency.class))).thenReturn(savedCompetency);

        FacultyFullProfileDto.CompetencyDto result = facultyService.addCompetency(1L, 20L);

        assertThat(result.getCourseId()).isEqualTo(20L);
        assertThat(result.getCourseCode()).isEqualTo("CS201");
        assertThat(result.getCourseName()).isEqualTo("Algorithms");
        verify(competencyRepository).save(any(FacultyCompetency.class));
    }

    @Test
    void addCompetency_nonExistentCourse_throwsValidationException() {
        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(courseRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultyService.addCompetency(1L, 999L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Course not found");

        verify(competencyRepository, never()).save(any());
    }

    // ==================== addCampusAssociation ====================

    @Test
    void addCampusAssociation_validCampus_succeeds() {
        FacultyCampusAssociation savedAssociation = new FacultyCampusAssociation();
        savedAssociation.setId(60L);
        savedAssociation.setFaculty(faculty);
        savedAssociation.setCampus(campus);
        savedAssociation.setTravelTimeMinutes(30);

        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(campusRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(campus));
        when(campusAssociationRepository.existsByFacultyIdAndCampusIdAndDeletedAtIsNull(1L, 5L)).thenReturn(false);
        when(campusAssociationRepository.save(any(FacultyCampusAssociation.class))).thenReturn(savedAssociation);

        FacultyFullProfileDto.CampusAssociationDto result = facultyService.addCampusAssociation(1L, 5L, 30);

        assertThat(result.getCampusId()).isEqualTo(5L);
        assertThat(result.getCampusName()).isEqualTo("Main Campus");
        assertThat(result.getTravelTimeMinutes()).isEqualTo(30);
        verify(campusAssociationRepository).save(any(FacultyCampusAssociation.class));
    }

    @Test
    void addCampusAssociation_nonExistentCampus_throwsValidationException() {
        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));
        when(campusRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultyService.addCampusAssociation(1L, 999L, 30))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Campus not found");

        verify(campusAssociationRepository, never()).save(any());
    }

    // ==================== delete ====================

    @Test
    void delete_existingFaculty_softDeletes() {
        when(facultyRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(faculty));

        facultyService.delete(1L);

        assertThat(faculty.getDeletedAt()).isNotNull();
        assertThat(faculty.getIsActive()).isFalse();
        verify(facultyRepository).save(faculty);
    }

    @Test
    void delete_nonExistent_throwsEntityNotFoundException() {
        when(facultyRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facultyService.delete(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
