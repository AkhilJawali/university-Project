package com.utms.masterdata.academiccalendar;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicCalendarService {

    private final AcademicCalendarRepository calendarRepository;
    private final HolidayRepository holidayRepository;
    private final ExamWindowRepository examWindowRepository;
    private final SpecialPeriodRepository specialPeriodRepository;
    private final CampusRepository campusRepository;
    private final AcademicCalendarMapper calendarMapper;

    @Transactional(readOnly = true)
    public Page<AcademicCalendarDto> findAll(Long campusId, String academicYear,
                                              SemesterType semesterType, Pageable pageable) {
        return calendarRepository.findWithFilters(campusId, academicYear, semesterType, pageable)
                .map(this::toDtoWithCounts);
    }

    @Transactional(readOnly = true)
    public AcademicCalendarDto findById(Long id) {
        AcademicCalendar calendar = calendarRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("AcademicCalendar", id));
        return toDtoWithCounts(calendar);
    }

    @Transactional
    public CreateResponse<AcademicCalendarDto> create(CreateAcademicCalendarRequest request) {
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(request.getCampusId())
                .orElseThrow(() -> new ValidationException("campusId",
                        "Campus not found or has been deleted", request.getCampusId()));

        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new ValidationException("startDate",
                    "Start date must be before end date", request.getStartDate());
        }

        boolean overlaps = calendarRepository.existsOverlapping(
                request.getCampusId(),
                request.getSemesterType(),
                request.getStartDate(),
                request.getEndDate());
        if (overlaps) {
            throw new ConflictException(
                    "An academic calendar for this campus and semester type already exists with overlapping dates.");
        }

        AcademicCalendar calendar = calendarMapper.toEntity(request);
        calendar.setCampus(campus);
        calendar.setIsActive(true);

        AcademicCalendar saved = calendarRepository.save(calendar);
        log.info("Academic calendar created: id={}, campus={}, semester={}",
                saved.getId(), campus.getName(), saved.getSemesterType());

        return new CreateResponse<>(toDtoWithCounts(saved));
    }

    @Transactional
    public AcademicCalendarDto update(Long id, CreateAcademicCalendarRequest request) {
        AcademicCalendar calendar = calendarRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("AcademicCalendar", id));

        if (!request.getStartDate().isBefore(request.getEndDate())) {
            throw new ValidationException("startDate",
                    "Start date must be before end date", request.getStartDate());
        }

        boolean overlaps = calendarRepository.existsOverlappingExcluding(
                calendar.getCampus().getId(),
                request.getSemesterType(),
                request.getStartDate(),
                request.getEndDate(),
                id);
        if (overlaps) {
            throw new ConflictException(
                    "An academic calendar for this campus and semester type already exists with overlapping dates.");
        }

        calendar.setName(request.getName().trim());
        calendar.setAcademicYear(request.getAcademicYear());
        calendar.setSemesterType(request.getSemesterType());
        calendar.setStartDate(request.getStartDate());
        calendar.setEndDate(request.getEndDate());

        AcademicCalendar saved = calendarRepository.save(calendar);
        log.info("Academic calendar updated: id={}", saved.getId());
        return toDtoWithCounts(saved);
    }

    @Transactional
    public void delete(Long id) {
        AcademicCalendar calendar = calendarRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("AcademicCalendar", id));

        calendar.softDelete();
        calendarRepository.save(calendar);
        log.info("Academic calendar soft-deleted: id={}", id);
    }

    private AcademicCalendarDto toDtoWithCounts(AcademicCalendar calendar) {
        AcademicCalendarDto dto = calendarMapper.toDto(calendar);
        dto.setHolidayCount((int) holidayRepository.countByCalendarIdAndDeletedAtIsNull(calendar.getId()));
        dto.setExamWindowCount((int) examWindowRepository.countByCalendarIdAndDeletedAtIsNull(calendar.getId()));
        dto.setSpecialPeriodCount((int) specialPeriodRepository.countByCalendarIdAndDeletedAtIsNull(calendar.getId()));
        return dto;
    }
}
