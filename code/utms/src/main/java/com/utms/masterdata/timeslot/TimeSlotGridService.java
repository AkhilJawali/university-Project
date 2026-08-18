package com.utms.masterdata.timeslot;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeSlotGridService {

    private final TimeSlotGridRepository gridRepository;
    private final SlotDefinitionRepository slotDefinitionRepository;
    private final WorkingDayRepository workingDayRepository;
    private final CampusRepository campusRepository;
    private final TimeSlotGridMapper gridMapper;

    @Transactional(readOnly = true)
    public Page<TimeSlotGridDto> findAll(Long campusId, Boolean isActive, Pageable pageable) {
        return gridRepository.findWithFilters(campusId, isActive, pageable)
                .map(gridMapper::toDto);
    }

    @Transactional(readOnly = true)
    public TimeSlotGridDto findById(Long id) {
        TimeSlotGrid grid = gridRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid", id));
        return gridMapper.toDto(grid);
    }

    @Transactional(readOnly = true)
    public TimeSlotGridDto findActiveByCampusId(Long campusId) {
        campusRepository.findByIdAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new EntityNotFoundException("Campus", campusId));
        TimeSlotGrid grid = gridRepository.findByCampusIdAndIsActiveTrueAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid",
                        "No active grid found for campus " + campusId));
        return gridMapper.toDto(grid);
    }

    @Transactional
    public CreateResponse<TimeSlotGridDto> create(CreateTimeSlotGridRequest request) {
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(request.getCampusId())
                .orElseThrow(() -> new ValidationException("campusId",
                        "Campus not found or has been deleted", request.getCampusId()));

        TimeSlotGrid grid = gridMapper.toEntity(request);
        grid.setCampus(campus);
        grid.setIsActive(false);

        TimeSlotGrid saved = gridRepository.save(grid);

        // Auto-seed default working days (Mon-Sat working, Sun off)
        for (int day = 1; day <= 7; day++) {
            WorkingDay wd = new WorkingDay();
            wd.setGrid(saved);
            wd.setDayOfWeek(day);
            wd.setIsWorkingDay(day <= 6);
            wd.setIsActive(true);
            workingDayRepository.save(wd);
        }

        log.info("Time-slot grid created: id={}, campus={}", saved.getId(), campus.getName());
        return new CreateResponse<>(gridMapper.toDto(saved));
    }

    @Transactional
    public TimeSlotGridDto update(Long id, CreateTimeSlotGridRequest request) {
        TimeSlotGrid grid = gridRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid", id));

        grid.setName(request.getName().trim());
        grid.setEffectiveFrom(request.getEffectiveFrom());

        TimeSlotGrid saved = gridRepository.save(grid);
        log.info("Time-slot grid updated: id={}", saved.getId());
        return gridMapper.toDto(saved);
    }

    @Transactional
    public TimeSlotGridDto activate(Long id) {
        TimeSlotGrid grid = gridRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid", id));

        if (Boolean.TRUE.equals(grid.getIsActive())) {
            return gridMapper.toDto(grid);
        }

        long slotCount = slotDefinitionRepository.countByGridIdAndDeletedAtIsNull(id);
        if (slotCount == 0) {
            throw new ValidationException("gridId",
                    "Cannot activate a grid with no slot definitions", id);
        }

        long workingDayCount = workingDayRepository.countByGridIdAndIsWorkingDayTrue(id);
        if (workingDayCount == 0) {
            throw new ValidationException("gridId",
                    "Cannot activate a grid with no working days configured", id);
        }

        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        gridRepository.deactivateAllForCampus(grid.getCampus().getId(), currentUser);

        grid.setIsActive(true);
        TimeSlotGrid saved = gridRepository.save(grid);

        log.info("Time-slot grid activated: id={}, campus={}", saved.getId(), grid.getCampus().getId());
        return gridMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        TimeSlotGrid grid = gridRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid", id));

        grid.softDelete();
        gridRepository.save(grid);
        log.info("Time-slot grid soft-deleted: id={}", id);
    }
}
