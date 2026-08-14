package com.utms.masterdata.room;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceBlockService {

    private final ResourceBlockRepository resourceBlockRepository;
    private final RoomRepository roomRepository;
    private final ResourceBlockMapper resourceBlockMapper;

    @Transactional(readOnly = true)
    public Page<ResourceBlockDto> findAllByRoomId(Long roomId, Pageable pageable) {
        validateRoomExists(roomId);
        return resourceBlockRepository.findAllByRoomIdAndDeletedAtIsNull(roomId, pageable)
                .map(resourceBlockMapper::toDto);
    }

    @Transactional
    public ResourceBlockDto create(Long roomId, CreateResourceBlockRequest request, String requestedBy) {
        Room room = roomRepository.findByIdAndDeletedAtIsNull(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room", roomId));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("endDate",
                    "End date must be on or after start date", request.getEndDate());
        }

        ResourceBlock block = resourceBlockMapper.toEntity(request);
        block.setRoom(room);
        block.setStatus(BlockStatus.REQUESTED);
        block.setRequestedBy(requestedBy);
        block.setIsActive(true);
        ResourceBlock saved = resourceBlockRepository.save(block);

        log.info("Resource block created: id={}, roomId={}, status=REQUESTED", saved.getId(), roomId);
        return resourceBlockMapper.toDto(saved);
    }

    @Transactional
    public ResourceBlockDto approve(Long roomId, Long blockId, String approvedBy) {
        ResourceBlock block = findBlockForRoom(roomId, blockId);

        validateTransition(block.getStatus(), BlockStatus.APPROVED);

        block.setStatus(BlockStatus.APPROVED);
        block.setApprovedBy(approvedBy);
        ResourceBlock saved = resourceBlockRepository.save(block);

        log.info("Resource block approved: id={}, roomId={}, approvedBy={}", blockId, roomId, approvedBy);
        return resourceBlockMapper.toDto(saved);
    }

    @Transactional
    public ResourceBlockDto activate(Long roomId, Long blockId) {
        ResourceBlock block = findBlockForRoom(roomId, blockId);

        validateTransition(block.getStatus(), BlockStatus.ACTIVE);

        block.setStatus(BlockStatus.ACTIVE);
        ResourceBlock saved = resourceBlockRepository.save(block);

        log.info("Resource block activated: id={}, roomId={}", blockId, roomId);
        return resourceBlockMapper.toDto(saved);
    }

    @Transactional
    public ResourceBlockDto release(Long roomId, Long blockId) {
        ResourceBlock block = findBlockForRoom(roomId, blockId);

        validateTransition(block.getStatus(), BlockStatus.RELEASED);

        block.setStatus(BlockStatus.RELEASED);
        ResourceBlock saved = resourceBlockRepository.save(block);

        log.info("Resource block released: id={}, roomId={}", blockId, roomId);
        return resourceBlockMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RoomAvailabilityDto> getAvailability(Long roomId, LocalDate startDate, LocalDate endDate) {
        Room room = roomRepository.findByIdAndDeletedAtIsNull(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room", roomId));

        List<ResourceBlock> activeBlocks = resourceBlockRepository.findActiveBlocksInDateRange(
                roomId, startDate, endDate);

        List<RoomAvailabilityDto> availability = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            LocalDate date = current;
            List<RoomAvailabilityDto.TimeSlot> blockedSlots = activeBlocks.stream()
                    .filter(b -> !date.isBefore(b.getStartDate()) && !date.isAfter(b.getEndDate()))
                    .filter(b -> b.getStartTime() != null && b.getEndTime() != null)
                    .map(b -> RoomAvailabilityDto.TimeSlot.builder()
                            .startTime(b.getStartTime())
                            .endTime(b.getEndTime())
                            .reason(b.getReason())
                            .build())
                    .toList();

            availability.add(RoomAvailabilityDto.builder()
                    .roomId(room.getId())
                    .roomName(room.getName())
                    .date(date)
                    .blockedSlots(blockedSlots)
                    .availableSlots(List.of())
                    .build());

            current = current.plusDays(1);
        }

        return availability;
    }

    private ResourceBlock findBlockForRoom(Long roomId, Long blockId) {
        validateRoomExists(roomId);
        return resourceBlockRepository.findByIdAndRoomId(blockId, roomId)
                .orElseThrow(() -> new EntityNotFoundException("ResourceBlock", blockId));
    }

    private void validateRoomExists(Long roomId) {
        roomRepository.findByIdAndDeletedAtIsNull(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room", roomId));
    }

    private void validateTransition(BlockStatus current, BlockStatus target) {
        boolean valid = switch (target) {
            case APPROVED -> current == BlockStatus.REQUESTED;
            case ACTIVE -> current == BlockStatus.APPROVED;
            case RELEASED -> current == BlockStatus.ACTIVE || current == BlockStatus.APPROVED;
            case REQUESTED -> false;
        };

        if (!valid) {
            throw new ValidationException("status",
                    "Invalid state transition from " + current + " to " + target, current);
        }
    }
}
