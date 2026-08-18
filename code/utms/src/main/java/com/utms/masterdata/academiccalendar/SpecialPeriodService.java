package com.utms.masterdata.academiccalendar;

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
public class SpecialPeriodService {

    private final SpecialPeriodRepository specialPeriodRepository;
    private final AcademicCalendarRepository calendarRepository;
    private final SpecialPeriodMapper specialPeriodMapper;

    @Transactional(readOnly = true)
    public List<SpecialPeriodDto> findByCalendarId(Long calendarId) {
        calendarRepository.findByIdAndDeletedAtIsNull(calendarId)
                .orElseThrow(() -> new EntityNotFoundException("AcademicCalendar", calendarId));
        return specialPeriodRepository.findByCalendarIdAndDeletedAtIsNull(calendarId).stream()
                .map(specialPeriodMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpecialPeriodDto findById(Long calendarId, Long id) {
        SpecialPeriod period = specialPeriodRepository.findByIdAndCalendarIdAndDeletedAtIsNull(id, calendarId)
                .orElseThrow(() -> new EntityNotFoundException("SpecialPeriod", id));
        return specialPeriodMapper.toDto(period);
    }

    @Transactional
    public SpecialPeriodDto create(Long calendarId, CreateSpecialPeriodRequest request) {
        AcademicCalendar calendar = calendarRepository.findByIdAndDeletedAtIsNull(calendarId)
                .orElseThrow(() -> new EntityNotFoundException("AcademicCalendar", calendarId));

        // Same-day windows are allowed (e.g., single-day exam)
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new ValidationException("startDate",
                    "Start date must be on or before end date", request.getStartDate());
        }

        if (request.getStartDate().isBefore(calendar.getStartDate())
                || request.getEndDate().isAfter(calendar.getEndDate())) {
            throw new ValidationException("startDate",
                    String.format("Special period dates must be within calendar range (%s to %s)",
                            calendar.getStartDate(), calendar.getEndDate()),
                    request.getStartDate());
        }

        SpecialPeriod period = specialPeriodMapper.toEntity(request);
        period.setCalendar(calendar);
        period.setIsActive(true);

        SpecialPeriod saved = specialPeriodRepository.save(period);
        log.info("Special period created: id={}, calendarId={}", saved.getId(), calendarId);
        return specialPeriodMapper.toDto(saved);
    }

    @Transactional
    public SpecialPeriodDto update(Long calendarId, Long id, CreateSpecialPeriodRequest request) {
        AcademicCalendar calendar = calendarRepository.findByIdAndDeletedAtIsNull(calendarId)
                .orElseThrow(() -> new EntityNotFoundException("AcademicCalendar", calendarId));

        SpecialPeriod period = specialPeriodRepository.findByIdAndCalendarIdAndDeletedAtIsNull(id, calendarId)
                .orElseThrow(() -> new EntityNotFoundException("SpecialPeriod", id));

        if (request.getStartDate().isBefore(calendar.getStartDate())
                || request.getEndDate().isAfter(calendar.getEndDate())) {
            throw new ValidationException("startDate",
                    String.format("Special period dates must be within calendar range (%s to %s)",
                            calendar.getStartDate(), calendar.getEndDate()),
                    request.getStartDate());
        }

        period.setName(request.getName().trim());
        period.setPeriodType(request.getPeriodType());
        period.setStartDate(request.getStartDate());
        period.setEndDate(request.getEndDate());

        SpecialPeriod saved = specialPeriodRepository.save(period);
        log.info("Special period updated: id={}", saved.getId());
        return specialPeriodMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long calendarId, Long id) {
        SpecialPeriod period = specialPeriodRepository.findByIdAndCalendarIdAndDeletedAtIsNull(id, calendarId)
                .orElseThrow(() -> new EntityNotFoundException("SpecialPeriod", id));

        period.softDelete();
        specialPeriodRepository.save(period);
        log.info("Special period soft-deleted: id={}", id);
    }
}
