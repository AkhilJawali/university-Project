package com.utms.masterdata.room;

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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final CampusRepository campusRepository;
    private final RoomMapper roomMapper;

    @Transactional(readOnly = true)
    public Page<RoomDto> findAll(Pageable pageable) {
        return roomRepository.findAllByDeletedAtIsNull(pageable)
                .map(roomMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RoomDto> findAllWithFilters(Long campusId, String building,
                                            RoomType roomType, Integer minCapacity,
                                            Pageable pageable) {
        return roomRepository.findAllWithFilters(campusId, building, roomType, minCapacity, pageable)
                .map(roomMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<RoomDto> search(String search, Pageable pageable) {
        return roomRepository.searchByNameOrCodeOrBuilding(search, pageable)
                .map(roomMapper::toDto);
    }

    @Transactional(readOnly = true)
    public RoomDto findById(Long id) {
        Room room = roomRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Room", id));
        return roomMapper.toDto(room);
    }

    @Transactional
    public CreateResponse<RoomDto> create(CreateRoomRequest request) {
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(request.getCampusId())
                .orElseThrow(() -> new ValidationException("campusId",
                        "Campus not found or has been deleted", request.getCampusId()));

        if (roomRepository.existsByCodeAndCampusIdAndDeletedAtIsNull(request.getCode(), request.getCampusId())) {
            throw new ConflictException("Room with code '" + request.getCode()
                    + "' already exists in this campus");
        }

        validateEquipmentTags(request.getEquipmentTags());

        Room room = roomMapper.toEntity(request);
        room.setCampus(campus);
        room.setIsActive(true);
        if (room.getEquipmentTags() == null) {
            room.setEquipmentTags(List.of());
        }
        Room saved = roomRepository.save(room);

        log.info("Room created: id={}, code={}, campusId={}", saved.getId(), saved.getCode(), campus.getId());
        return new CreateResponse<>(roomMapper.toDto(saved));
    }

    @Transactional
    public RoomDto update(Long id, CreateRoomRequest request) {
        Room room = roomRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Room", id));

        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(request.getCampusId())
                .orElseThrow(() -> new ValidationException("campusId",
                        "Campus not found or has been deleted", request.getCampusId()));

        if (roomRepository.existsByCodeAndCampusIdAndIdNotAndDeletedAtIsNull(
                request.getCode(), request.getCampusId(), id)) {
            throw new ConflictException("Room with code '" + request.getCode()
                    + "' already exists in this campus");
        }

        validateEquipmentTags(request.getEquipmentTags());

        roomMapper.updateEntity(request, room);
        room.setCampus(campus);
        Room saved = roomRepository.save(room);

        log.info("Room updated: id={}, code={}", saved.getId(), saved.getCode());
        return roomMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Room room = roomRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Room", id));

        room.softDelete();
        roomRepository.save(room);
        log.info("Room soft-deleted: id={}", id);
    }

    private void validateEquipmentTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        if (tags.size() > 20) {
            throw new ValidationException("equipmentTags",
                    "Maximum 20 equipment tags allowed", tags.size());
        }
        for (String tag : tags) {
            if (tag == null || tag.isBlank()) {
                throw new ValidationException("equipmentTags",
                        "Equipment tags must not be blank", tag);
            }
            if (tag.length() > 50) {
                throw new ValidationException("equipmentTags",
                        "Each equipment tag must be at most 50 characters", tag);
            }
            if (!tag.equals(tag.toLowerCase())) {
                throw new ValidationException("equipmentTags",
                        "Equipment tags must be lowercase", tag);
            }
        }
    }
}
