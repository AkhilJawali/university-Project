package com.utms.masterdata.timeslot;

import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.campus.Campus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
class SlotDefinitionServiceTest {

    @Mock
    private SlotDefinitionRepository slotDefinitionRepository;

    @Mock
    private TimeSlotGridRepository gridRepository;

    @Mock
    private SlotDefinitionMapper slotDefinitionMapper;

    @InjectMocks
    private SlotDefinitionService slotDefinitionService;

    private TimeSlotGrid grid;
    private SlotDefinition slot;
    private SlotDefinitionDto slotDto;
    private CreateSlotDefinitionRequest createRequest;

    @BeforeEach
    void setUp() {
        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");

        grid = new TimeSlotGrid();
        grid.setId(1L);
        grid.setName("Standard Grid");
        grid.setCampus(campus);
        grid.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        grid.setIsActive(false);

        slot = new SlotDefinition();
        slot.setId(1L);
        slot.setGrid(grid);
        slot.setSlotNumber(1);
        slot.setStartTime(LocalTime.of(8, 0));
        slot.setEndTime(LocalTime.of(9, 0));
        slot.setSlotType(SlotType.LECTURE);
        slot.setDurationMinutes(60);
        slot.setIsActive(true);

        slotDto = SlotDefinitionDto.builder()
                .id(1L)
                .gridId(1L)
                .slotNumber(1)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .slotType(SlotType.LECTURE)
                .durationMinutes(60)
                .build();

        createRequest = CreateSlotDefinitionRequest.builder()
                .slotNumber(1)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .slotType(SlotType.LECTURE)
                .build();
    }

    @Test
    void create_validRequest_returnsSlotDtoWithAutoCalculatedDuration() {
        when(gridRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(grid));
        when(slotDefinitionRepository.existsOverlapping(1L, LocalTime.of(8, 0), LocalTime.of(9, 0)))
                .thenReturn(false);
        when(slotDefinitionRepository.existsByGridIdAndSlotNumber(1L, 1)).thenReturn(false);
        when(slotDefinitionMapper.toEntity(createRequest)).thenReturn(slot);
        when(slotDefinitionRepository.save(any(SlotDefinition.class))).thenReturn(slot);
        when(slotDefinitionMapper.toDto(slot)).thenReturn(slotDto);

        SlotDefinitionDto result = slotDefinitionService.create(1L, createRequest);

        assertThat(result.getDurationMinutes()).isEqualTo(60);
        assertThat(result.getSlotType()).isEqualTo(SlotType.LECTURE);
        verify(slotDefinitionRepository).save(any(SlotDefinition.class));
    }

    @Test
    void create_overlappingTimes_throwsConflictException() {
        when(gridRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(grid));
        when(slotDefinitionRepository.existsOverlapping(1L, LocalTime.of(8, 0), LocalTime.of(9, 0)))
                .thenReturn(true);

        assertThatThrownBy(() -> slotDefinitionService.create(1L, createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlap");

        verify(slotDefinitionRepository, never()).save(any());
    }

    @Test
    void create_startTimeAfterEndTime_throwsValidationException() {
        CreateSlotDefinitionRequest invalidRequest = CreateSlotDefinitionRequest.builder()
                .slotNumber(1)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(9, 0))
                .slotType(SlotType.LECTURE)
                .build();

        when(gridRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(grid));

        assertThatThrownBy(() -> slotDefinitionService.create(1L, invalidRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Start time must be before end time");

        verify(slotDefinitionRepository, never()).save(any());
    }

    @Test
    void create_duplicateSlotNumber_throwsConflictException() {
        when(gridRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(grid));
        when(slotDefinitionRepository.existsOverlapping(1L, LocalTime.of(8, 0), LocalTime.of(9, 0)))
                .thenReturn(false);
        when(slotDefinitionRepository.existsByGridIdAndSlotNumber(1L, 1)).thenReturn(true);

        assertThatThrownBy(() -> slotDefinitionService.create(1L, createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Slot number 1 already exists");

        verify(slotDefinitionRepository, never()).save(any());
    }

    @Test
    void bulkCreate_validSlots_returnsAllCreated() {
        CreateSlotDefinitionRequest slot1 = CreateSlotDefinitionRequest.builder()
                .slotNumber(1).startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0))
                .slotType(SlotType.LECTURE).build();
        CreateSlotDefinitionRequest slot2 = CreateSlotDefinitionRequest.builder()
                .slotNumber(2).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .slotType(SlotType.LECTURE).build();

        when(gridRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(grid));
        when(slotDefinitionRepository.existsOverlapping(eq(1L), any(), any())).thenReturn(false);
        when(slotDefinitionRepository.existsByGridIdAndSlotNumber(eq(1L), any())).thenReturn(false);

        SlotDefinition saved1 = new SlotDefinition();
        saved1.setId(1L);
        saved1.setGrid(grid);
        saved1.setSlotNumber(1);
        saved1.setStartTime(LocalTime.of(8, 0));
        saved1.setEndTime(LocalTime.of(9, 0));
        saved1.setSlotType(SlotType.LECTURE);
        saved1.setDurationMinutes(60);

        SlotDefinition saved2 = new SlotDefinition();
        saved2.setId(2L);
        saved2.setGrid(grid);
        saved2.setSlotNumber(2);
        saved2.setStartTime(LocalTime.of(9, 0));
        saved2.setEndTime(LocalTime.of(10, 0));
        saved2.setSlotType(SlotType.LECTURE);
        saved2.setDurationMinutes(60);

        when(slotDefinitionRepository.saveAll(any())).thenReturn(List.of(saved1, saved2));
        when(slotDefinitionMapper.toDto(saved1)).thenReturn(SlotDefinitionDto.builder()
                .id(1L).gridId(1L).slotNumber(1).durationMinutes(60).build());
        when(slotDefinitionMapper.toDto(saved2)).thenReturn(SlotDefinitionDto.builder()
                .id(2L).gridId(1L).slotNumber(2).durationMinutes(60).build());

        List<SlotDefinitionDto> result = slotDefinitionService.bulkCreate(1L, List.of(slot1, slot2));

        assertThat(result).hasSize(2);
        verify(slotDefinitionRepository).saveAll(any());
    }

    @Test
    void bulkCreate_internalOverlap_throwsConflictException() {
        CreateSlotDefinitionRequest slot1 = CreateSlotDefinitionRequest.builder()
                .slotNumber(1).startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 30))
                .slotType(SlotType.LECTURE).build();
        CreateSlotDefinitionRequest slot2 = CreateSlotDefinitionRequest.builder()
                .slotNumber(2).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .slotType(SlotType.LECTURE).build();

        when(gridRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(grid));

        assertThatThrownBy(() -> slotDefinitionService.bulkCreate(1L, List.of(slot1, slot2)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlap in time");

        verify(slotDefinitionRepository, never()).saveAll(any());
    }

    @Test
    void bulkCreate_duplicateSlotNumbers_throwsConflictException() {
        CreateSlotDefinitionRequest slot1 = CreateSlotDefinitionRequest.builder()
                .slotNumber(1).startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0))
                .slotType(SlotType.LECTURE).build();
        CreateSlotDefinitionRequest slot2 = CreateSlotDefinitionRequest.builder()
                .slotNumber(1).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .slotType(SlotType.LECTURE).build();

        when(gridRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(grid));

        assertThatThrownBy(() -> slotDefinitionService.bulkCreate(1L, List.of(slot1, slot2)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Duplicate slot number");

        verify(slotDefinitionRepository, never()).saveAll(any());
    }

    @Test
    void create_gridNotFound_throwsEntityNotFoundException() {
        when(gridRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slotDefinitionService.create(999L, createRequest))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_existingSlot_softDeletes() {
        when(slotDefinitionRepository.findByIdAndGridIdAndDeletedAtIsNull(1L, 1L))
                .thenReturn(Optional.of(slot));

        slotDefinitionService.delete(1L, 1L);

        assertThat(slot.getDeletedAt()).isNotNull();
        assertThat(slot.getIsActive()).isFalse();
        verify(slotDefinitionRepository).save(slot);
    }
}
