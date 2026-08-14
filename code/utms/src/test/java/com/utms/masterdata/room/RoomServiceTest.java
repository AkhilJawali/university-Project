package com.utms.masterdata.room;

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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private ResourceBlockRepository resourceBlockRepository;

    @Mock
    private ResourceBlockMapper resourceBlockMapper;

    @InjectMocks
    private RoomService roomService;

    private ResourceBlockService resourceBlockService;

    private Campus campus;
    private Room room;
    private RoomDto roomDto;
    private CreateRoomRequest createRequest;
    private ResourceBlock resourceBlock;
    private ResourceBlockDto resourceBlockDto;

    @BeforeEach
    void setUp() {
        resourceBlockService = new ResourceBlockService(
                resourceBlockRepository, roomRepository, resourceBlockMapper);

        campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");
        campus.setCode("MAIN");
        campus.setIsActive(true);

        room = new Room();
        room.setId(100L);
        room.setCode("LH-101");
        room.setName("Lecture Hall 101");
        room.setCampus(campus);
        room.setBuilding("Block A");
        room.setFloor("1");
        room.setCapacity(120);
        room.setRoomType(RoomType.LECTURE_HALL);
        room.setEquipmentTags(List.of("projector", "whiteboard"));
        room.setIsActive(true);

        roomDto = RoomDto.builder()
                .id(100L)
                .code("LH-101")
                .name("Lecture Hall 101")
                .campusId(1L)
                .campusName("Main Campus")
                .building("Block A")
                .floor("1")
                .capacity(120)
                .roomType(RoomType.LECTURE_HALL)
                .equipmentTags(List.of("projector", "whiteboard"))
                .isActive(true)
                .build();

        createRequest = CreateRoomRequest.builder()
                .code("LH-101")
                .name("Lecture Hall 101")
                .campusId(1L)
                .building("Block A")
                .floor("1")
                .capacity(120)
                .roomType(RoomType.LECTURE_HALL)
                .equipmentTags(List.of("projector", "whiteboard"))
                .build();

        resourceBlock = new ResourceBlock();
        resourceBlock.setId(200L);
        resourceBlock.setRoom(room);
        resourceBlock.setReason("Maintenance work");
        resourceBlock.setStartDate(LocalDate.of(2026, 9, 1));
        resourceBlock.setEndDate(LocalDate.of(2026, 9, 5));
        resourceBlock.setStatus(BlockStatus.REQUESTED);
        resourceBlock.setRequestedBy("coordinator@utms.edu");
        resourceBlock.setIsActive(true);

        resourceBlockDto = ResourceBlockDto.builder()
                .id(200L)
                .roomId(100L)
                .roomName("Lecture Hall 101")
                .reason("Maintenance work")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 5))
                .status(BlockStatus.REQUESTED)
                .requestedBy("coordinator@utms.edu")
                .build();
    }

    // ==================== RoomService Tests ====================

    @Test
    void findById_existingRoom_returnsRoomDto() {
        when(roomRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(roomMapper.toDto(room)).thenReturn(roomDto);

        RoomDto result = roomService.findById(100L);

        assertThat(result.getCode()).isEqualTo("LH-101");
        assertThat(result.getCapacity()).isEqualTo(120);
        assertThat(result.getCampusName()).isEqualTo("Main Campus");
    }

    @Test
    void findById_nonExistent_throwsEntityNotFoundException() {
        when(roomRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_validRequest_returnsRoomDto() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(roomRepository.existsByCodeAndCampusIdAndDeletedAtIsNull("LH-101", 1L)).thenReturn(false);
        when(roomMapper.toEntity(createRequest)).thenReturn(room);
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMapper.toDto(room)).thenReturn(roomDto);

        CreateResponse<RoomDto> result = roomService.create(createRequest);

        assertThat(result.getData().getCode()).isEqualTo("LH-101");
        assertThat(result.getData().getCapacity()).isEqualTo(120);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void create_duplicateCodeInCampus_throwsConflictException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(roomRepository.existsByCodeAndCampusIdAndDeletedAtIsNull("LH-101", 1L)).thenReturn(true);

        assertThatThrownBy(() -> roomService.create(createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("LH-101");

        verify(roomRepository, never()).save(any());
    }

    @Test
    void create_invalidCampusId_throwsValidationException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());
        createRequest.setCampusId(999L);

        assertThatThrownBy(() -> roomService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Campus not found");

        verify(roomRepository, never()).save(any());
    }

    @Test
    void create_equipmentTagsExceedLimit_throwsValidationException() {
        List<String> tags = List.of(
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
                "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u");
        createRequest.setEquipmentTags(tags);
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(roomRepository.existsByCodeAndCampusIdAndDeletedAtIsNull("LH-101", 1L)).thenReturn(false);

        assertThatThrownBy(() -> roomService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Maximum 20 equipment tags");

        verify(roomRepository, never()).save(any());
    }

    @Test
    void create_equipmentTagUppercase_throwsValidationException() {
        createRequest.setEquipmentTags(List.of("Projector"));
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(roomRepository.existsByCodeAndCampusIdAndDeletedAtIsNull("LH-101", 1L)).thenReturn(false);

        assertThatThrownBy(() -> roomService.create(createRequest))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("lowercase");

        verify(roomRepository, never()).save(any());
    }

    @Test
    void delete_existingRoom_softDeletes() {
        when(roomRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));

        roomService.delete(100L);

        assertThat(room.getDeletedAt()).isNotNull();
        assertThat(room.getIsActive()).isFalse();
        verify(roomRepository).save(room);
    }

    @Test
    void delete_nonExistent_throwsEntityNotFoundException() {
        when(roomRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.delete(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== ResourceBlockService Tests ====================

    @Test
    void approveBlock_validTransition_returnsApprovedDto() {
        resourceBlock.setStatus(BlockStatus.REQUESTED);
        ResourceBlockDto approvedDto = ResourceBlockDto.builder()
                .id(200L).roomId(100L).status(BlockStatus.APPROVED).build();

        when(roomRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(resourceBlockRepository.findByIdAndRoomId(200L, 100L)).thenReturn(Optional.of(resourceBlock));
        when(resourceBlockRepository.save(any(ResourceBlock.class))).thenReturn(resourceBlock);
        when(resourceBlockMapper.toDto(resourceBlock)).thenReturn(approvedDto);

        ResourceBlockDto result = resourceBlockService.approve(100L, 200L, "admin@utms.edu");

        assertThat(result.getStatus()).isEqualTo(BlockStatus.APPROVED);
        assertThat(resourceBlock.getApprovedBy()).isEqualTo("admin@utms.edu");
    }

    @Test
    void approveBlock_alreadyActive_throwsValidationException() {
        resourceBlock.setStatus(BlockStatus.ACTIVE);

        when(roomRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(resourceBlockRepository.findByIdAndRoomId(200L, 100L)).thenReturn(Optional.of(resourceBlock));

        assertThatThrownBy(() -> resourceBlockService.approve(100L, 200L, "admin@utms.edu"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid state transition");

        verify(resourceBlockRepository, never()).save(any());
    }

    @Test
    void activateBlock_fromApproved_returnsActiveDto() {
        resourceBlock.setStatus(BlockStatus.APPROVED);
        ResourceBlockDto activeDto = ResourceBlockDto.builder()
                .id(200L).roomId(100L).status(BlockStatus.ACTIVE).build();

        when(roomRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(resourceBlockRepository.findByIdAndRoomId(200L, 100L)).thenReturn(Optional.of(resourceBlock));
        when(resourceBlockRepository.save(any(ResourceBlock.class))).thenReturn(resourceBlock);
        when(resourceBlockMapper.toDto(resourceBlock)).thenReturn(activeDto);

        ResourceBlockDto result = resourceBlockService.activate(100L, 200L);

        assertThat(result.getStatus()).isEqualTo(BlockStatus.ACTIVE);
    }

    @Test
    void activateBlock_fromRequested_throwsValidationException() {
        resourceBlock.setStatus(BlockStatus.REQUESTED);

        when(roomRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(resourceBlockRepository.findByIdAndRoomId(200L, 100L)).thenReturn(Optional.of(resourceBlock));

        assertThatThrownBy(() -> resourceBlockService.activate(100L, 200L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid state transition");

        verify(resourceBlockRepository, never()).save(any());
    }

    @Test
    void releaseBlock_fromActive_returnsReleasedDto() {
        resourceBlock.setStatus(BlockStatus.ACTIVE);
        ResourceBlockDto releasedDto = ResourceBlockDto.builder()
                .id(200L).roomId(100L).status(BlockStatus.RELEASED).build();

        when(roomRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(room));
        when(resourceBlockRepository.findByIdAndRoomId(200L, 100L)).thenReturn(Optional.of(resourceBlock));
        when(resourceBlockRepository.save(any(ResourceBlock.class))).thenReturn(resourceBlock);
        when(resourceBlockMapper.toDto(resourceBlock)).thenReturn(releasedDto);

        ResourceBlockDto result = resourceBlockService.release(100L, 200L);

        assertThat(result.getStatus()).isEqualTo(BlockStatus.RELEASED);
    }
}
