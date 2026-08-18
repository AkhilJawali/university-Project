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
public class ExamWindowService {

    private final ExamWindowRepository examWindowRepository;
    private final AcademicCalendarRepository calendarRepository;
    private final ExamWindowMapper examWindowMapper;

    @Transactional(readOnly = true)
    public List<ExamWindowDto> findByCalendarId(Long calendarId) {
        calendarRepository.findByIdAndDeletedAtIsNull(calendarId)
                .orElseThrow(() -> new EntityNotFoundException("AcademicCalendar", calendarId));
        return examWindowRepository.findByCalendarIdAndDeletedAtIsNull(calendarId).stream()
                .map(examWindowMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExamWindowDto findById(Long calendarId, Long id) {
        ExamWindow examWindow = examWindowRepository.findByIdAndCalendarIdAndDeletedAtIsNull(id, calendarId)
                .orElseThrow(() -> new EntityNotFoundException("ExamWindow", id));
        return examWindowMapper.toDto(examWindow);
    }

    @Transactional
    public ExamWindowDto create(Long calendarId, CreateExamWindowRequest request) {
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
                    String.format("Exam window dates must be within calendar range (%s to %s)",
                            calendar.getStartDate(), calendar.getEndDate()),
                    request.getStartDate());
        }

        ExamWindow examWindow = examWindowMapper.toEntity(request);
        examWindow.setCalendar(calendar);
        examWindow.setIsActive(true);

        ExamWindow saved = examWindowRepository.save(examWindow);
        log.info("Exam window created: id={}, calendarId={}", saved.getId(), calendarId);
        return examWindowMapper.toDto(saved);
    }

    @Transactional
    public ExamWindowDto update(Long calendarId, Long id, CreateExamWindowRequest request) {
        AcademicCalendar calendar = calendarRepository.findByIdAndDeletedAtIsNull(calendarId)
                .orElseThrow(() -> new EntityNotFoundException("AcademicCalendar", calendarId));

        ExamWindow examWindow = examWindowRepository.findByIdAndCalendarIdAndDeletedAtIsNull(id, calendarId)
                .orElseThrow(() -> new EntityNotFoundException("ExamWindow", id));

        if (request.getStartDate().isBefore(calendar.getStartDate())
                || request.getEndDate().isAfter(calendar.getEndDate())) {
            throw new ValidationException("startDate",
                    String.format("Exam window dates must be within calendar range (%s to %s)",
                            calendar.getStartDate(), calendar.getEndDate()),
                    request.getStartDate());
        }

        examWindow.setName(request.getName().trim());
        examWindow.setExamType(request.getExamType());
        examWindow.setStartDate(request.getStartDate());
        examWindow.setEndDate(request.getEndDate());

        ExamWindow saved = examWindowRepository.save(examWindow);
        log.info("Exam window updated: id={}", saved.getId());
        return examWindowMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long calendarId, Long id) {
        ExamWindow examWindow = examWindowRepository.findByIdAndCalendarIdAndDeletedAtIsNull(id, calendarId)
                .orElseThrow(() -> new EntityNotFoundException("ExamWindow", id));

        examWindow.softDelete();
        examWindowRepository.save(examWindow);
        log.info("Exam window soft-deleted: id={}", id);
    }
}
