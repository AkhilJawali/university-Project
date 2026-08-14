package com.utms.masterdata.program;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.batch.BatchRepository;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
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
class ProgramServiceTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private ProgramMapper programMapper;

    @InjectMocks
    private ProgramService programService;

    private Department department;
    private Program program;
    private ProgramDto programDto;
    private CreateProgramRequest createRequest;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(10L);
        department.setName("Computer Science");
        department.setCode("CS");
        department.setIsActive(true);

        program = new Program();
        program.setId(50L);
        program.setName("B.Tech Computer Science");
        program.setCode("BTCS");
        program.setDepartment(department);
        program.setDurationYears(4);
        program.setTotalSemesters(8);
        program.setDegreeType(DegreeType.UG);
        program.setIsActive(true);

        programDto = ProgramDto.builder()
                .id(50L)
                .name("B.Tech Computer Science")
                .code("BTCS")
                .departmentId(10L)
                .departmentName("Computer Science")
                .durationYears(4)
                .totalSemesters(8)
                .degreeType(DegreeType.UG)
                .isActive(true)
                .build();

        createRequest = CreateProgramRequest.builder()
                .name("B.Tech Computer Science")
                .code("BTCS")
                .departmentId(10L)
                .durationYears(4)
                .totalSemesters(8)
                .degreeType(DegreeType.UG)
                .build();
    }

    @Test
    void findById_existingProgram_returnsProgramDto() {
        when(programRepository.findByIdAndDeletedAtIsNull(50L)).thenReturn(Optional.of(program));
        when(programMapper.toDto(program)).thenReturn(programDto);

        ProgramDto result = programService.findById(50L);

        assertThat(result.getCode()).isEqualTo("BTCS");
        assertThat(result.getDegreeType()).isEqualTo(DegreeType.UG);
    }

    @Test
    void create_validRequest_returnsProgramDto() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(programRepository.existsByCodeAndDepartmentIdAndDeletedAtIsNull("BTCS", 10L)).thenReturn(false);
        when(programMapper.toEntity(createRequest)).thenReturn(program);
        when(programRepository.save(any(Program.class))).thenReturn(program);
        when(programMapper.toDto(program)).thenReturn(programDto);

        CreateResponse<ProgramDto> result = programService.create(createRequest);

        assertThat(result.getData().getCode()).isEqualTo("BTCS");
        verify(programRepository).save(any(Program.class));
    }

    @Test
    void create_invalidDepartmentId_throwsValidationException() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());
        createRequest.setDepartmentId(999L);

        assertThatThrownBy(() -> programService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Department not found");

        verify(programRepository, never()).save(any());
    }

    @Test
    void create_duplicateCodeWithinDepartment_throwsConflictException() {
        when(departmentRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(department));
        when(programRepository.existsByCodeAndDepartmentIdAndDeletedAtIsNull("BTCS", 10L)).thenReturn(true);

        assertThatThrownBy(() -> programService.create(createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BTCS");

        verify(programRepository, never()).save(any());
    }

    @Test
    void delete_programWithNoChildren_softDeletes() {
        when(programRepository.findByIdAndDeletedAtIsNull(50L)).thenReturn(Optional.of(program));
        when(batchRepository.countByProgramIdAndDeletedAtIsNull(50L)).thenReturn(0L);

        programService.delete(50L);

        assertThat(program.getDeletedAt()).isNotNull();
        verify(programRepository).save(program);
    }

    @Test
    void delete_programWithActiveBatches_throwsConflictException() {
        when(programRepository.findByIdAndDeletedAtIsNull(50L)).thenReturn(Optional.of(program));
        when(batchRepository.countByProgramIdAndDeletedAtIsNull(50L)).thenReturn(4L);

        assertThatThrownBy(() -> programService.delete(50L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("4")
                .hasMessageContaining("active batch");

        verify(programRepository, never()).save(any());
    }
}
