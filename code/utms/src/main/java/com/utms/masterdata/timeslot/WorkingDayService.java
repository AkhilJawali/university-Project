package com.utms.masterdata.timeslot;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkingDayService {

    private final WorkingDayRepository workingDayRepository;
    private final TimeSlotGridRepository gridRepository;

    @Transactional(readOnly = true)
    public List<WorkingDayDto> findByGridId(Long gridId) {
        gridRepository.findByIdAndDeletedAtIsNull(gridId)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid", gridId));
        return workingDayRepository.findByGridIdOrderByDayOfWeekAsc(gridId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public List<WorkingDayDto> updateWorkingDays(Long gridId, UpdateWorkingDaysRequest request) {
        gridRepository.findByIdAndDeletedAtIsNull(gridId)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid", gridId));

        // Validate at least one working day
        long workingDayCount = request.getDays().stream()
                .filter(UpdateWorkingDaysRequest.WorkingDayEntry::getIsWorkingDay)
                .count();
        if (workingDayCount == 0) {
            throw new ValidationException("days",
                    "At least one day must be configured as a working day", 0);
        }

        for (UpdateWorkingDaysRequest.WorkingDayEntry entry : request.getDays()) {
            WorkingDay workingDay = workingDayRepository.findByGridIdAndDayOfWeek(gridId, entry.getDayOfWeek())
                    .orElseThrow(() -> new EntityNotFoundException("WorkingDay",
                            "day_of_week=" + entry.getDayOfWeek()));
            workingDay.setIsWorkingDay(entry.getIsWorkingDay());
            workingDayRepository.save(workingDay);
        }

        log.info("Working days updated: gridId={}, workingDays={}", gridId, workingDayCount);
        return findByGridId(gridId);
    }

    private WorkingDayDto toDto(WorkingDay workingDay) {
        return WorkingDayDto.builder()
                .dayOfWeek(workingDay.getDayOfWeek())
                .dayName(DayOfWeek.of(workingDay.getDayOfWeek()).name())
                .isWorkingDay(workingDay.getIsWorkingDay())
                .build();
    }
}
