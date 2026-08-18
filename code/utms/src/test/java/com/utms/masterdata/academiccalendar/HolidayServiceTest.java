package com.utms.masterdata.academiccalendar;

import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private AcademicCalendarRepository calendarRepository;

    @Mock
    private HolidayMapper holidayMapper;

    @InjectMocks
    private HolidayService holidayService;

    private AcademicCalendar calendar;
    private Holiday holiday;
    private HolidayDto holidayDto;
    private CreateHolidayRequest createRequest;

    @BeforeEach
    void setUp() {
        calendar = new AcademicCalendar();
        calendar.setId(1L);
        calendar.setName("Odd Semester 2026");
        calendar.setStartDate(LocalDate.of(2026, 8, 1));
        calendar.setEndDate(LocalDate.of(2026, 12, 15));
        calendar.setIsActive(true);

        holiday = new Holiday();
        holiday.setId(1L);
        holiday.setCalendar(calendar);
        holiday.setName("Diwali");
        holiday.setDate(LocalDate.of(2026, 10, 20));
        holiday.setDayType(DayType.FULL_DAY);
        holiday.setIsRecurring(false);
        holiday.setIsActive(true);
        holiday.setCreatedAt(LocalDateTime.now());
        holiday.setUpdatedAt(LocalDateTime.now());

        holidayDto = HolidayDto.builder()
                .id(1L)
                .calendarId(1L)
                .name("Diwali")
                .date(LocalDate.of(2026, 10, 20))
                .dayType(DayType.FULL_DAY)
                .isRecurring(false)
                .build();

        createRequest = CreateHolidayRequest.builder()
                .name("Diwali")
                .date(LocalDate.of(2026, 10, 20))
                .dayType(DayType.FULL_DAY)
                .isRecurring(false)
                .build();
    }

    @Test
    void create_validRequest_returnsHolidayDto() {
        when(calendarRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(calendar));
        when(holidayRepository.existsByCalendarIdAndDate(1L, LocalDate.of(2026, 10, 20))).thenReturn(false);
        when(holidayMapper.toEntity(createRequest)).thenReturn(holiday);
        when(holidayRepository.save(any(Holiday.class))).thenReturn(holiday);
        when(holidayMapper.toDto(holiday)).thenReturn(holidayDto);

        HolidayDto result = holidayService.create(1L, createRequest);

        assertThat(result.getName()).isEqualTo("Diwali");
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2026, 10, 20));
        verify(holidayRepository).save(any(Holiday.class));
    }

    @Test
    void create_dateOutsideCalendarRange_throwsValidationException() {
        CreateHolidayRequest outsideRequest = CreateHolidayRequest.builder()
                .name("New Year")
                .date(LocalDate.of(2027, 1, 1))
                .dayType(DayType.FULL_DAY)
                .isRecurring(false)
                .build();

        when(calendarRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(calendar));

        assertThatThrownBy(() -> holidayService.create(1L, outsideRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("within calendar range");

        verify(holidayRepository, never()).save(any());
    }

    @Test
    void create_duplicateDate_throwsConflictException() {
        when(calendarRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(calendar));
        when(holidayRepository.existsByCalendarIdAndDate(1L, LocalDate.of(2026, 10, 20))).thenReturn(true);

        assertThatThrownBy(() -> holidayService.create(1L, createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists on");

        verify(holidayRepository, never()).save(any());
    }

    @Test
    void create_calendarNotFound_throwsEntityNotFoundException() {
        when(calendarRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> holidayService.create(999L, createRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("AcademicCalendar");

        verify(holidayRepository, never()).save(any());
    }

    @Test
    void findById_existingHoliday_returnsDto() {
        when(holidayRepository.findByIdAndCalendarIdAndDeletedAtIsNull(1L, 1L))
                .thenReturn(Optional.of(holiday));
        when(holidayMapper.toDto(holiday)).thenReturn(holidayDto);

        HolidayDto result = holidayService.findById(1L, 1L);

        assertThat(result.getName()).isEqualTo("Diwali");
    }

    @Test
    void findById_nonExistent_throwsEntityNotFoundException() {
        when(holidayRepository.findByIdAndCalendarIdAndDeletedAtIsNull(999L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> holidayService.findById(1L, 999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void delete_existingHoliday_softDeletes() {
        when(holidayRepository.findByIdAndCalendarIdAndDeletedAtIsNull(1L, 1L))
                .thenReturn(Optional.of(holiday));

        holidayService.delete(1L, 1L);

        assertThat(holiday.getDeletedAt()).isNotNull();
        assertThat(holiday.getIsActive()).isFalse();
        verify(holidayRepository).save(holiday);
    }
}
