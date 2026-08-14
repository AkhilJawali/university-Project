package com.utms.masterdata.batch;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.program.Program;
import com.utms.masterdata.program.ProgramRepository;
import com.utms.masterdata.section.SectionRepository;
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
class BatchServiceTest {

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private BatchMapper batchMapper;

    @InjectMocks
    private BatchService batchService;

    private Program program;
    private Batch batch;
    private BatchDto batchDto;
    private CreateBatchRequest createRequest;

    @BeforeEach
    void setUp() {
        program = new Program();
        program.setId(50L);
        program.setName("B.Tech CS");
        program.setIsActive(true);

        batch = new Batch();
        batch.setId(100L);
        batch.setName("2024-28");
        batch.setProgram(program);
        batch.setAcademicYear("2024-2028");
        batch.setSemesterNumber(1);
        batch.setStrength(120);
        batch.setIsActive(true);

        batchDto = BatchDto.builder()
                .id(100L)
                .name("2024-28")
                .programId(50L)
                .programName("B.Tech CS")
                .academicYear("2024-2028")
                .semesterNumber(1)
                .strength(120)
                .isActive(true)
                .build();

        createRequest = CreateBatchRequest.builder()
                .name("2024-28")
                .programId(50L)
                .academicYear("2024-2028")
                .semesterNumber(1)
                .strength(120)
                .build();
    }

    @Test
    void findById_existingBatch_returnsBatchDto() {
        when(batchRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(batch));
        when(batchMapper.toDto(batch)).thenReturn(batchDto);

        BatchDto result = batchService.findById(100L);

        assertThat(result.getName()).isEqualTo("2024-28");
        assertThat(result.getStrength()).isEqualTo(120);
    }

    @Test
    void findById_nonExistent_throwsEntityNotFoundException() {
        when(batchRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_validRequest_returnsBatchDto() {
        when(programRepository.findByIdAndDeletedAtIsNull(50L)).thenReturn(Optional.of(program));
        when(batchMapper.toEntity(createRequest)).thenReturn(batch);
        when(batchRepository.save(any(Batch.class))).thenReturn(batch);
        when(batchMapper.toDto(batch)).thenReturn(batchDto);

        CreateResponse<BatchDto> result = batchService.create(createRequest);

        assertThat(result.getData().getName()).isEqualTo("2024-28");
        verify(batchRepository).save(any(Batch.class));
    }

    @Test
    void create_invalidProgramId_throwsValidationException() {
        when(programRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());
        createRequest.setProgramId(999L);

        assertThatThrownBy(() -> batchService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Program not found");

        verify(batchRepository, never()).save(any());
    }

    @Test
    void delete_batchWithNoChildren_softDeletes() {
        when(batchRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(batch));
        when(sectionRepository.countByBatchIdAndDeletedAtIsNull(100L)).thenReturn(0L);

        batchService.delete(100L);

        assertThat(batch.getDeletedAt()).isNotNull();
        verify(batchRepository).save(batch);
    }

    @Test
    void delete_batchWithActiveSections_throwsConflictException() {
        when(batchRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(batch));
        when(sectionRepository.countByBatchIdAndDeletedAtIsNull(100L)).thenReturn(2L);

        assertThatThrownBy(() -> batchService.delete(100L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("2")
                .hasMessageContaining("active section");

        verify(batchRepository, never()).save(any());
    }
}
