package com.utms.masterdata.timeslot;

import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotDefinitionService {

    private final SlotDefinitionRepository slotDefinitionRepository;
    private final TimeSlotGridRepository gridRepository;
    private final SlotDefinitionMapper slotDefinitionMapper;

    @Transactional(readOnly = true)
    public List<SlotDefinitionDto> findByGridId(Long gridId) {
        gridRepository.findByIdAndDeletedAtIsNull(gridId)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid", gridId));
        return slotDefinitionRepository.findByGridIdAndDeletedAtIsNullOrderBySlotNumberAsc(gridId).stream()
                .map(slotDefinitionMapper::toDto)
                .toList();
    }

    @Transactional
    public SlotDefinitionDto create(Long gridId, CreateSlotDefinitionRequest request) {
        TimeSlotGrid grid = gridRepository.findByIdAndDeletedAtIsNull(gridId)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid", gridId));

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new ValidationException("startTime",
                    "Start time must be before end time", request.getStartTime());
        }

        boolean overlaps = slotDefinitionRepository.existsOverlapping(
                gridId, request.getStartTime(), request.getEndTime());
        if (overlaps) {
            throw new ConflictException("Slot times overlap with an existing slot in this grid.");
        }

        if (slotDefinitionRepository.existsByGridIdAndSlotNumberAndNotDeleted(gridId, request.getSlotNumber())) {
            throw new ConflictException(
                    "Slot number " + request.getSlotNumber() + " already exists in this grid.");
        }

        int durationMinutes = (int) Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();

        SlotDefinition slot = slotDefinitionMapper.toEntity(request);
        slot.setGrid(grid);
        slot.setDurationMinutes(durationMinutes);
        slot.setIsActive(true);

        SlotDefinition saved = slotDefinitionRepository.save(slot);
        log.info("Slot definition created: id={}, gridId={}, slot#={}", saved.getId(), gridId, saved.getSlotNumber());
        return slotDefinitionMapper.toDto(saved);
    }

    @Transactional
    public SlotDefinitionDto update(Long gridId, Long id, CreateSlotDefinitionRequest request) {
        gridRepository.findByIdAndDeletedAtIsNull(gridId)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid", gridId));

        SlotDefinition slot = slotDefinitionRepository.findByIdAndGridIdAndDeletedAtIsNull(id, gridId)
                .orElseThrow(() -> new EntityNotFoundException("SlotDefinition", id));

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new ValidationException("startTime",
                    "Start time must be before end time", request.getStartTime());
        }

        boolean overlaps = slotDefinitionRepository.existsOverlappingExcluding(
                gridId, request.getStartTime(), request.getEndTime(), id);
        if (overlaps) {
            throw new ConflictException("Slot times overlap with an existing slot in this grid.");
        }

        int durationMinutes = (int) Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();

        slot.setSlotNumber(request.getSlotNumber());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setSlotType(request.getSlotType());
        slot.setDurationMinutes(durationMinutes);

        SlotDefinition saved = slotDefinitionRepository.save(slot);
        log.info("Slot definition updated: id={}", saved.getId());
        return slotDefinitionMapper.toDto(saved);
    }

    @Transactional
    public List<SlotDefinitionDto> bulkCreate(Long gridId, List<CreateSlotDefinitionRequest> slots) {
        TimeSlotGrid grid = gridRepository.findByIdAndDeletedAtIsNull(gridId)
                .orElseThrow(() -> new EntityNotFoundException("TimeSlotGrid", gridId));

        // Validate no duplicate slot numbers within the request
        Set<Integer> slotNumbers = new HashSet<>();
        for (CreateSlotDefinitionRequest slot : slots) {
            if (!slotNumbers.add(slot.getSlotNumber())) {
                throw new ConflictException(
                        "Duplicate slot number in request: " + slot.getSlotNumber());
            }
        }

        // Validate no overlaps within the request itself
        List<CreateSlotDefinitionRequest> sorted = slots.stream()
                .sorted(Comparator.comparing(CreateSlotDefinitionRequest::getStartTime))
                .toList();
        for (int i = 0; i < sorted.size() - 1; i++) {
            if (sorted.get(i).getEndTime().isAfter(sorted.get(i + 1).getStartTime())) {
                throw new ConflictException(String.format(
                        "Slots %d and %d overlap in time.",
                        sorted.get(i).getSlotNumber(), sorted.get(i + 1).getSlotNumber()));
            }
        }

        // Validate no conflicts with existing slots in DB
        for (CreateSlotDefinitionRequest slot : slots) {
            if (slotDefinitionRepository.existsOverlapping(gridId, slot.getStartTime(), slot.getEndTime())) {
                throw new ConflictException(
                        "Slot " + slot.getSlotNumber() + " overlaps with existing slot in grid.");
            }
            if (slotDefinitionRepository.existsByGridIdAndSlotNumberAndNotDeleted(gridId, slot.getSlotNumber())) {
                throw new ConflictException(
                        "Slot number " + slot.getSlotNumber() + " already exists in grid.");
            }
        }

        // Persist all
        List<SlotDefinition> entities = slots.stream().map(slot -> {
            SlotDefinition sd = new SlotDefinition();
            sd.setGrid(grid);
            sd.setSlotNumber(slot.getSlotNumber());
            sd.setStartTime(slot.getStartTime());
            sd.setEndTime(slot.getEndTime());
            sd.setSlotType(slot.getSlotType());
            sd.setDurationMinutes((int) Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes());
            sd.setIsActive(true);
            return sd;
        }).toList();

        List<SlotDefinition> saved = slotDefinitionRepository.saveAll(entities);
        log.info("Bulk slot definitions created: gridId={}, count={}", gridId, saved.size());
        return saved.stream().map(slotDefinitionMapper::toDto).toList();
    }

    @Transactional
    public void delete(Long gridId, Long id) {
        SlotDefinition slot = slotDefinitionRepository.findByIdAndGridIdAndDeletedAtIsNull(id, gridId)
                .orElseThrow(() -> new EntityNotFoundException("SlotDefinition", id));

        slot.softDelete();
        slotDefinitionRepository.save(slot);
        log.info("Slot definition soft-deleted: id={}", id);
    }
}
