package com.utms.masterdata.academiccalendar;

import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final AcademicCalendarRepository calendarRepository;
    private final HolidayMapper holidayMapper;

    @Transactional(readOnly = true)
    public List<HolidayDto> findByCalendarId(Long calendarId) {
        calendarRepository.findByIdAndDeletedAtIsNull(calendarId)
                .orElseThrow(() -> new EntityNotFoundException("AcademicCalendar", calendarId));
        return holidayRepository.findByCalendarIdAndDeletedAtIsNull(calendarId).stream()
                .map(holidayMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public HolidayDto findById(Long calendarId, Long id) {
        Holiday holiday = holidayRepository.findByIdAndCalendarIdAndDeletedAtIsNull(id, calendarId)
                .orElseThrow(() -> new EntityNotFoundException("Holiday", id));
        return holidayMapper.toDto(holiday);
    }

    @Transactional
    public HolidayDto create(Long calendarId, CreateHolidayRequest request) {
        AcademicCalendar calendar = calendarRepository.findByIdAndDeletedAtIsNull(calendarId)
                .orElseThrow(() -> new EntityNotFoundException("AcademicCalendar", calendarId));

        if (request.getDate().isBefore(calendar.getStartDate())
                || request.getDate().isAfter(calendar.getEndDate())) {
            throw new ValidationException("date",
                    String.format("Holiday date must be within calendar range (%s to %s)",
                            calendar.getStartDate(), calendar.getEndDate()),
                    request.getDate());
        }

        if (holidayRepository.existsByCalendarIdAndDateAndNotDeleted(calendarId, request.getDate())) {
            throw new ConflictException(
                    "A holiday already exists on " + request.getDate() + " in this calendar.");
        }

        Holiday holiday = holidayMapper.toEntity(request);
        holiday.setCalendar(calendar);
        holiday.setIsActive(true);

        Holiday saved = holidayRepository.save(holiday);
        log.info("Holiday created: id={}, calendarId={}, date={}", saved.getId(), calendarId, saved.getDate());
        return holidayMapper.toDto(saved);
    }

    @Transactional
    public HolidayDto update(Long calendarId, Long id, CreateHolidayRequest request) {
        AcademicCalendar calendar = calendarRepository.findByIdAndDeletedAtIsNull(calendarId)
                .orElseThrow(() -> new EntityNotFoundException("AcademicCalendar", calendarId));

        Holiday holiday = holidayRepository.findByIdAndCalendarIdAndDeletedAtIsNull(id, calendarId)
                .orElseThrow(() -> new EntityNotFoundException("Holiday", id));

        if (request.getDate().isBefore(calendar.getStartDate())
                || request.getDate().isAfter(calendar.getEndDate())) {
            throw new ValidationException("date",
                    String.format("Holiday date must be within calendar range (%s to %s)",
                            calendar.getStartDate(), calendar.getEndDate()),
                    request.getDate());
        }

        holiday.setName(request.getName().trim());
        holiday.setDate(request.getDate());
        holiday.setDayType(request.getDayType());
        holiday.setIsRecurring(request.getIsRecurring());

        Holiday saved = holidayRepository.save(holiday);
        log.info("Holiday updated: id={}", saved.getId());
        return holidayMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long calendarId, Long id) {
        Holiday holiday = holidayRepository.findByIdAndCalendarIdAndDeletedAtIsNull(id, calendarId)
                .orElseThrow(() -> new EntityNotFoundException("Holiday", id));

        holiday.softDelete();
        holidayRepository.save(holiday);
        log.info("Holiday soft-deleted: id={}", id);
    }
}
