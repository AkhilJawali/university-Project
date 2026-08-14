package com.utms.masterdata.section;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.batch.Batch;
import com.utms.masterdata.batch.BatchRepository;
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
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private SectionMapper sectionMapper;

    @InjectMocks
    private SectionService sectionService;

    private Batch batch;
    private Section section;
    private SectionDto sectionDto;
    private CreateSectionRequest createRequest;

    @BeforeEach
    void setUp() {
        batch = new Batch();
        batch.setId(100L);
        batch.setName("2024-28");
        batch.setStrength(120);
        batch.setIsActive(true);

        section = new Section();
        section.setId(500L);
        section.setName("A");
        section.setBatch(batch);
        section.setStrength(60);
        section.setIsActive(true);

        sectionDto = SectionDto.builder()
                .id(500L)
                .name("A")
                .batchId(100L)
                .batchName("2024-28")
                .strength(60)
                .isActive(true)
                .build();

        createRequest = CreateSectionRequest.builder()
                .name("A")
                .batchId(100L)
                .strength(60)
                .build();
    }

    @Test
    void findById_existingSection_returnsSectionDto() {
        when(sectionRepository.findByIdAndDeletedAtIsNull(500L)).thenReturn(Optional.of(section));
        when(sectionMapper.toDto(section)).thenReturn(sectionDto);

        SectionDto result = sectionService.findById(500L);

        assertThat(result.getName()).isEqualTo("A");
        assertThat(result.getBatchId()).isEqualTo(100L);
    }

    @Test
    void findById_nonExistent_throwsEntityNotFoundException() {
        when(sectionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_validRequest_returnsSectionDtoWithNoWarnings() {
        when(batchRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(batch));
        when(sectionRepository.existsByNameAndBatchIdAndDeletedAtIsNull("A", 100L)).thenReturn(false);
        when(sectionMapper.toEntity(createRequest)).thenReturn(section);
        when(sectionRepository.save(any(Section.class))).thenReturn(section);
        when(sectionMapper.toDto(section)).thenReturn(sectionDto);
        when(sectionRepository.sumStrengthByBatchId(100L)).thenReturn(60);

        CreateResponse<SectionDto> result = sectionService.create(createRequest);

        assertThat(result.getData().getName()).isEqualTo("A");
        assertThat(result.getWarnings()).isEmpty();
        verify(sectionRepository).save(any(Section.class));
    }

    @Test
    void create_exceedsBatchStrength_returnsSectionDtoWithWarning() {
        when(batchRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(batch));
        when(sectionRepository.existsByNameAndBatchIdAndDeletedAtIsNull("A", 100L)).thenReturn(false);
        when(sectionMapper.toEntity(createRequest)).thenReturn(section);
        when(sectionRepository.save(any(Section.class))).thenReturn(section);
        when(sectionMapper.toDto(section)).thenReturn(sectionDto);
        when(sectionRepository.sumStrengthByBatchId(100L)).thenReturn(150); // exceeds batch.strength=120

        CreateResponse<SectionDto> result = sectionService.create(createRequest);

        assertThat(result.getData()).isNotNull();
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).contains("exceeds batch strength");
    }

    @Test
    void create_invalidBatchId_throwsValidationException() {
        when(batchRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());
        createRequest.setBatchId(999L);

        assertThatThrownBy(() -> sectionService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Batch not found");

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void create_duplicateNameInBatch_throwsConflictException() {
        when(batchRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(batch));
        when(sectionRepository.existsByNameAndBatchIdAndDeletedAtIsNull("A", 100L)).thenReturn(true);

        assertThatThrownBy(() -> sectionService.create(createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("A")
                .hasMessageContaining("already exists");

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void delete_existingSection_softDeletes() {
        when(sectionRepository.findByIdAndDeletedAtIsNull(500L)).thenReturn(Optional.of(section));

        sectionService.delete(500L);

        assertThat(section.getDeletedAt()).isNotNull();
        assertThat(section.getIsActive()).isFalse();
        verify(sectionRepository).save(section);
    }

    @Test
    void delete_nonExistentSection_throwsEntityNotFoundException() {
        when(sectionRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.delete(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
