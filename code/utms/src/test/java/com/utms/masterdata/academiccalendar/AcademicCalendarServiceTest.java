package com.utms.masterdata.academiccalendar;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicCalendarServiceTest {

    @Mock
    private AcademicCalendarRepository calendarRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private AcademicCalendarMapper calendarMapper;

    @InjectMocks
    private AcademicCalendarService calendarService;

    private Campus campus;
    private AcademicCalendar calendar;
    private AcademicCalendarDto calendarDto;
    private CreateAcademicCalendarRequest createRequest;

    @BeforeEach
    void setUp() {
        campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");
        campus.setIsActive(true);

        calendar = new AcademicCalendar();
        calendar.setId(1L);
        calendar.setName("Odd Semester 2026");
        calendar.setCampus(campus);
        calendar.setAcademicYear("2026-2027");
        calendar.setSemesterType(SemesterType.ODD);
        calendar.setStartDate(LocalDate.of(2026, 8, 1));
        calendar.setEndDate(LocalDate.of(2026, 12, 15));
        calendar.setIsActive(true);
        calendar.setCreatedAt(LocalDateTime.now());
        calendar.setUpdatedAt(LocalDateTime.now());

        calendarDto = AcademicCalendarDto.builder()
                .id(1L)
                .name("Odd Semester 2026")
                .campusId(1L)
                .campusName("Main Campus")
                .academicYear("2026-2027")
                .semesterType(SemesterType.ODD)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 12, 15))
                .isActive(true)
                .holidayCount(0)
                .examWindowCount(0)
                .specialPeriodCount(0)
                .build();

        createRequest = CreateAcademicCalendarRequest.builder()
                .name("Odd Semester 2026")
                .campusId(1L)
                .academicYear("2026-2027")
                .semesterType(SemesterType.ODD)
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 12, 15))
                .build();
    }

    @Test
    void create_validRequest_returnsCalendarDto() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(calendarRepository.existsOverlapping(1L, SemesterType.ODD,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15))).thenReturn(false);
        when(calendarMapper.toEntity(createRequest)).thenReturn(calendar);
        when(calendarRepository.save(any(AcademicCalendar.class))).thenReturn(calendar);
        when(calendarMapper.toDto(calendar)).thenReturn(calendarDto);

        CreateResponse<AcademicCalendarDto> result = calendarService.create(createRequest);

        assertThat(result.getData().getName()).isEqualTo("Odd Semester 2026");
        assertThat(result.getData().getSemesterType()).isEqualTo(SemesterType.ODD);
        verify(calendarRepository).save(any(AcademicCalendar.class));
    }

    @Test
    void create_overlappingDates_throwsConflictException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(calendarRepository.existsOverlapping(1L, SemesterType.ODD,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15))).thenReturn(true);

        assertThatThrownBy(() -> calendarService.create(createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlapping dates");

        verify(calendarRepository, never()).save(any());
    }

    @Test
    void create_invalidCampusId_throwsValidationException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Campus not found");

        verify(calendarRepository, never()).save(any());
    }

    @Test
    void create_startDateAfterEndDate_throwsValidationException() {
        CreateAcademicCalendarRequest invalidRequest = CreateAcademicCalendarRequest.builder()
                .name("Invalid Calendar")
                .campusId(1L)
                .academicYear("2026-2027")
                .semesterType(SemesterType.ODD)
                .startDate(LocalDate.of(2026, 12, 15))
                .endDate(LocalDate.of(2026, 8, 1))
                .build();

        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));

        assertThatThrownBy(() -> calendarService.create(invalidRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Start date must be before end date");

        verify(calendarRepository, never()).save(any());
    }

    @Test
    void findById_existingCalendar_returnsDto() {
        when(calendarRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(calendar));
        when(calendarMapper.toDto(calendar)).thenReturn(calendarDto);

        AcademicCalendarDto result = calendarService.findById(1L);

        assertThat(result.getName()).isEqualTo("Odd Semester 2026");
        assertThat(result.getAcademicYear()).isEqualTo("2026-2027");
    }

    @Test
    void findById_nonExistent_throwsEntityNotFoundException() {
        when(calendarRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("AcademicCalendar");
    }

    @Test
    void delete_existingCalendar_softDeletes() {
        when(calendarRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(calendar));

        calendarService.delete(1L);

        assertThat(calendar.getDeletedAt()).isNotNull();
        assertThat(calendar.getIsActive()).isFalse();
        verify(calendarRepository).save(calendar);
    }

    @Test
    void delete_nonExistent_throwsEntityNotFoundException() {
        when(calendarRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.delete(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
