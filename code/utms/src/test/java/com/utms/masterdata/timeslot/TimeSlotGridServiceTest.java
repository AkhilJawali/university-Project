package com.utms.masterdata.timeslot;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeSlotGridServiceTest {

    @Mock
    private TimeSlotGridRepository gridRepository;

    @Mock
    private SlotDefinitionRepository slotDefinitionRepository;

    @Mock
    private WorkingDayRepository workingDayRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private TimeSlotGridMapper gridMapper;

    @InjectMocks
    private TimeSlotGridService gridService;

    private Campus campus;
    private TimeSlotGrid grid;
    private TimeSlotGridDto gridDto;
    private CreateTimeSlotGridRequest createRequest;

    @BeforeEach
    void setUp() {
        campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");
        campus.setIsActive(true);

        grid = new TimeSlotGrid();
        grid.setId(1L);
        grid.setName("Standard Grid");
        grid.setCampus(campus);
        grid.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        grid.setIsActive(false);
        grid.setCreatedAt(LocalDateTime.now());
        grid.setUpdatedAt(LocalDateTime.now());

        gridDto = TimeSlotGridDto.builder()
                .id(1L)
                .name("Standard Grid")
                .campusId(1L)
                .campusName("Main Campus")
                .effectiveFrom(LocalDate.of(2026, 8, 1))
                .isActive(false)
                .build();

        createRequest = CreateTimeSlotGridRequest.builder()
                .name("Standard Grid")
                .campusId(1L)
                .effectiveFrom(LocalDate.of(2026, 8, 1))
                .build();
    }

    @Test
    void create_validRequest_returnsGridDtoWithWorkingDaysSeeded() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(gridMapper.toEntity(createRequest)).thenReturn(grid);
        when(gridRepository.save(any(TimeSlotGrid.class))).thenReturn(grid);
        when(workingDayRepository.save(any(WorkingDay.class))).thenAnswer(i -> i.getArgument(0));
        when(gridMapper.toDto(grid)).thenReturn(gridDto);

        CreateResponse<TimeSlotGridDto> result = gridService.create(createRequest);

        assertThat(result.getData().getName()).isEqualTo("Standard Grid");
        verify(gridRepository).save(any(TimeSlotGrid.class));
        // 7 working days should be auto-seeded
        verify(workingDayRepository, times(7)).save(any(WorkingDay.class));
    }

    @Test
    void activate_gridWithSlots_deactivatesPreviousAndActivates() {
        when(gridRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(grid));
        when(slotDefinitionRepository.countByGridIdAndDeletedAtIsNull(1L)).thenReturn(5L);
        when(workingDayRepository.countByGridIdAndIsWorkingDayTrue(1L)).thenReturn(6L);
        when(gridRepository.save(any(TimeSlotGrid.class))).thenReturn(grid);
        when(gridMapper.toDto(grid)).thenReturn(gridDto);

        TimeSlotGridDto result = gridService.activate(1L);

        verify(gridRepository).deactivateAllForCampus(1L);
        assertThat(grid.getIsActive()).isTrue();
        verify(gridRepository).save(grid);
    }

    @Test
    void activate_gridWithNoSlots_throwsValidationException() {
        when(gridRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(grid));
        when(slotDefinitionRepository.countByGridIdAndDeletedAtIsNull(1L)).thenReturn(0L);

        assertThatThrownBy(() -> gridService.activate(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("no slot definitions");
    }

    @Test
    void activate_gridWithNoWorkingDays_throwsValidationException() {
        when(gridRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(grid));
        when(slotDefinitionRepository.countByGridIdAndDeletedAtIsNull(1L)).thenReturn(5L);
        when(workingDayRepository.countByGridIdAndIsWorkingDayTrue(1L)).thenReturn(0L);

        assertThatThrownBy(() -> gridService.activate(1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("no working days");
    }

    @Test
    void activate_alreadyActiveGrid_returnsWithoutChange() {
        grid.setIsActive(true);
        TimeSlotGridDto activeDto = TimeSlotGridDto.builder()
                .id(1L)
                .name("Standard Grid")
                .isActive(true)
                .build();

        when(gridRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(grid));
        when(gridMapper.toDto(grid)).thenReturn(activeDto);

        TimeSlotGridDto result = gridService.activate(1L);

        assertThat(result.getIsActive()).isTrue();
        // Should not call deactivate since it's already active
        verify(gridRepository, times(0)).deactivateAllForCampus(any());
    }

    @Test
    void findById_nonExistent_throwsEntityNotFoundException() {
        when(gridRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gridService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_existingGrid_softDeletes() {
        when(gridRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(grid));

        gridService.delete(1L);

        assertThat(grid.getDeletedAt()).isNotNull();
        assertThat(grid.getIsActive()).isFalse();
        verify(gridRepository).save(grid);
    }
}
