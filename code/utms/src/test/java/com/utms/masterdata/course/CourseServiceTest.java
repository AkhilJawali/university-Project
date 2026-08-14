package com.utms.masterdata.course;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseService courseService;

    private Department department;
    private Course course;
    private CourseDto courseDto;
    private CreateCourseRequest createRequest;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(10L);
        department.setName("Computer Science");
        department.setCode("CS");
        department.setIsActive(true);

        course = new Course();
        course.setId(100L);
        course.setCode("CS101");
        course.setName("Data Structures");
        course.setDepartment(department);
        course.setLectureHours(3);
        course.setTutorialHours(1);
        course.setPracticalHours(2);
        course.setCreditHours(6);
        course.setCourseType(CourseType.CORE);
        course.setIsCrossListed(false);
        course.setPrerequisites(new ArrayList<>());
        course.setEquipmentTags(List.of("projector"));
        course.setIsActive(true);

        courseDto = CourseDto.builder()
                .id(100L)
                .code("CS101")
                .name("Data Structures")
                .departmentId(10L)
                .departmentName("Computer Science")
                .lectureHours(3)
                .tutorialHours(1)
                .practicalHours(2)
                .creditHours(6)
                .courseType(CourseType.CORE)
                .isCrossListed(false)
                .prerequisites(List.of())
                .equipmentTags(List.of("projector"))
                .isActive(true)
                .build();

        createRequest = CreateCourseRequest.builder()
                .code("CS101")
                .name("Data Structures")
                .departmentId(10L)
                .lectureHours(3)
                .tutorialHours(1)
                .practicalHours(2)
                .creditHours(6)
                .courseType(CourseType.CORE)
                .isCrossListed(false)
                .prerequisites(List.of())
                .equipmentTags(List.of("projector"))
                .build();
    }

    @Test
    void findById_existingCourse_returnsCourseDto() {
        when(courseRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(course));
        when(courseMapper.toDto(course)).thenReturn(courseDto);

        CourseDto result = courseService.findById(100L);

        assertThat(result.getCode()).isEqualTo("CS101");
        assertThat(result.getLectureHours()).isEqualTo(3);
    }

    @Test
    void findById_nonExistent_throwsEntityNotFoundException() {
        when(courseRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_validRequest_returnsCourseDto() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(courseRepository.existsByCodeAndDeletedAtIsNull("CS101")).thenReturn(false);
        when(courseMapper.toEntity(createRequest)).thenReturn(course);
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toDto(course)).thenReturn(courseDto);

        CreateResponse<CourseDto> result = courseService.create(createRequest);

        assertThat(result.getData().getCode()).isEqualTo("CS101");
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void create_duplicateCode_throwsConflictException() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(courseRepository.existsByCodeAndDeletedAtIsNull("CS101")).thenReturn(true);

        assertThatThrownBy(() -> courseService.create(createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CS101");

        verify(courseRepository, never()).save(any());
    }

    @Test
    void create_invalidDepartmentId_throwsValidationException() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());
        createRequest.setDepartmentId(999L);

        assertThatThrownBy(() -> courseService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Department not found");

        verify(courseRepository, never()).save(any());
    }

    @Test
    void create_ltpAllZero_throwsValidationException() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(courseRepository.existsByCodeAndDeletedAtIsNull("CS101")).thenReturn(false);
        createRequest.setLectureHours(0);
        createRequest.setTutorialHours(0);
        createRequest.setPracticalHours(0);

        assertThatThrownBy(() -> courseService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("At least one of L, T, P must be greater than 0");

        verify(courseRepository, never()).save(any());
    }

    @Test
    void create_creditMismatch_returnsWarning() {
        createRequest.setCreditHours(4); // L+T+P = 3+1+2 = 6, but creditHours=4
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(courseRepository.existsByCodeAndDeletedAtIsNull("CS101")).thenReturn(false);
        when(courseMapper.toEntity(createRequest)).thenReturn(course);
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toDto(course)).thenReturn(courseDto);

        CreateResponse<CourseDto> result = courseService.create(createRequest);

        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).contains("Credit hours");
    }

    @Test
    void create_selfPrerequisite_throwsValidationException() {
        createRequest.setPrerequisites(List.of(100L));
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(courseRepository.existsByCodeAndDeletedAtIsNull("CS101")).thenReturn(false);

        // For update scenario (currentCourseId not null) — testing via update
        Course existingCourse = new Course();
        existingCourse.setId(100L);
        existingCourse.setCode("CS101");
        existingCourse.setDepartment(department);
        existingCourse.setPrerequisites(new ArrayList<>());
        when(courseRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(existingCourse));
        when(courseRepository.existsByCodeAndIdNotAndDeletedAtIsNull("CS101", 100L)).thenReturn(false);

        assertThatThrownBy(() -> courseService.update(100L, createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be a prerequisite of itself");
    }

    @Test
    void create_nonExistentPrerequisite_throwsValidationException() {
        createRequest.setPrerequisites(List.of(999L));
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(courseRepository.existsByCodeAndDeletedAtIsNull("CS101")).thenReturn(false);
        when(courseRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Prerequisite course not found");
    }

    @Test
    void create_equipmentTagsExceedLimit_throwsValidationException() {
        List<String> tags = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k");
        createRequest.setEquipmentTags(tags);
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(courseRepository.existsByCodeAndDeletedAtIsNull("CS101")).thenReturn(false);

        assertThatThrownBy(() -> courseService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Maximum 10 equipment tags");
    }

    @Test
    void create_equipmentTagUppercase_throwsValidationException() {
        createRequest.setEquipmentTags(List.of("Projector"));
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(courseRepository.existsByCodeAndDeletedAtIsNull("CS101")).thenReturn(false);

        assertThatThrownBy(() -> courseService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lowercase");
    }

    @Test
    void delete_existingCourse_softDeletes() {
        when(courseRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(course));

        courseService.delete(100L);

        assertThat(course.getDeletedAt()).isNotNull();
        assertThat(course.getIsActive()).isFalse();
        verify(courseRepository).save(course);
    }

    @Test
    void delete_nonExistent_throwsEntityNotFoundException() {
        when(courseRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.delete(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
