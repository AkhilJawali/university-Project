package com.utms.masterdata.department;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.masterdata.program.ProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentService departmentService;

    private Campus campus;
    private Department department;
    private DepartmentDto departmentDto;
    private CreateDepartmentRequest createRequest;

    @BeforeEach
    void setUp() {
        campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");
        campus.setCode("MAIN");

        department = new Department();
        department.setId(10L);
        department.setName("Computer Science");
        department.setCode("CS");
        department.setCampus(campus);
        department.setIsActive(true);

        departmentDto = DepartmentDto.builder()
                .id(10L)
                .name("Computer Science")
                .code("CS")
                .campusId(1L)
                .campusName("Main Campus")
                .isActive(true)
                .build();

        createRequest = CreateDepartmentRequest.builder()
                .name("Computer Science")
                .code("CS")
                .campusId(1L)
                .build();
    }

    @Test
    void findById_existingDepartment_returnsDepartmentDto() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(departmentMapper.toDto(department)).thenReturn(departmentDto);

        DepartmentDto result = departmentService.findById(10L);

        assertThat(result.getCode()).isEqualTo("CS");
        assertThat(result.getCampusId()).isEqualTo(1L);
    }

    @Test
    void findById_nonExistent_throwsEntityNotFoundException() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_validRequest_returnsDepartmentDto() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(departmentRepository.existsByCodeAndCampusIdAndDeletedAtIsNull("CS", 1L)).thenReturn(false);
        when(departmentMapper.toEntity(createRequest)).thenReturn(department);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        when(departmentMapper.toDto(department)).thenReturn(departmentDto);

        CreateResponse<DepartmentDto> result = departmentService.create(createRequest);

        assertThat(result.getData().getCode()).isEqualTo("CS");
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    void create_invalidCampusId_throwsValidationException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());
        createRequest.setCampusId(999L);

        assertThatThrownBy(() -> departmentService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Campus not found");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void create_duplicateCodeWithinCampus_throwsConflictException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(departmentRepository.existsByCodeAndCampusIdAndDeletedAtIsNull("CS", 1L)).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CS");

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void delete_departmentWithNoChildren_softDeletes() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(programRepository.countByDepartmentIdAndDeletedAtIsNull(10L)).thenReturn(0L);

        departmentService.delete(10L);

        assertThat(department.getDeletedAt()).isNotNull();
        verify(departmentRepository).save(department);
    }

    @Test
    void delete_departmentWithActivePrograms_throwsConflictException() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(programRepository.countByDepartmentIdAndDeletedAtIsNull(10L)).thenReturn(5L);

        assertThatThrownBy(() -> departmentService.delete(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("5")
                .hasMessageContaining("active program");

        verify(departmentRepository, never()).save(any());
    }
}
