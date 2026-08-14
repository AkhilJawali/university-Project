package com.utms.masterdata.campus;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.department.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusServiceTest {

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CampusMapper campusMapper;

    @InjectMocks
    private CampusService campusService;

    private Campus campus;
    private CampusDto campusDto;
    private CreateCampusRequest createRequest;
    private UpdateCampusRequest updateRequest;

    @BeforeEach
    void setUp() {
        campus = new Campus();
        campus.setId(1L);
        campus.setName("Main Campus");
        campus.setCode("MAIN");
        campus.setTimezone("Asia/Kolkata");
        campus.setIsActive(true);
        campus.setCreatedAt(LocalDateTime.now());
        campus.setUpdatedAt(LocalDateTime.now());

        campusDto = CampusDto.builder()
                .id(1L)
                .name("Main Campus")
                .code("MAIN")
                .timezone("Asia/Kolkata")
                .isActive(true)
                .build();

        createRequest = CreateCampusRequest.builder()
                .name("Main Campus")
                .code("MAIN")
                .timezone("Asia/Kolkata")
                .build();

        updateRequest = UpdateCampusRequest.builder()
                .name("Main Campus Updated")
                .code("MAIN")
                .timezone("Asia/Kolkata")
                .build();
    }

    @Test
    void findAll_returnsPagedResults() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Campus> page = new PageImpl<>(List.of(campus), pageable, 1);
        when(campusRepository.findAllByDeletedAtIsNull(pageable)).thenReturn(page);
        when(campusMapper.toDto(campus)).thenReturn(campusDto);

        Page<CampusDto> result = campusService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCode()).isEqualTo("MAIN");
    }

    @Test
    void findById_existingCampus_returnsCampusDto() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(campusMapper.toDto(campus)).thenReturn(campusDto);

        CampusDto result = campusService.findById(1L);

        assertThat(result.getCode()).isEqualTo("MAIN");
        assertThat(result.getName()).isEqualTo("Main Campus");
    }

    @Test
    void findById_nonExistentCampus_throwsEntityNotFoundException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campusService.findById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Campus")
                .hasMessageContaining("999");
    }

    @Test
    void create_validRequest_returnsCampusDto() {
        when(campusRepository.existsByCodeAndDeletedAtIsNull("MAIN")).thenReturn(false);
        when(campusMapper.toEntity(createRequest)).thenReturn(campus);
        when(campusRepository.save(any(Campus.class))).thenReturn(campus);
        when(campusMapper.toDto(campus)).thenReturn(campusDto);

        CreateResponse<CampusDto> result = campusService.create(createRequest);

        assertThat(result.getData().getCode()).isEqualTo("MAIN");
        assertThat(result.getWarnings()).isEmpty();
        verify(campusRepository).save(any(Campus.class));
    }

    @Test
    void create_duplicateCode_throwsConflictException() {
        when(campusRepository.existsByCodeAndDeletedAtIsNull("MAIN")).thenReturn(true);

        assertThatThrownBy(() -> campusService.create(createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("MAIN")
                .hasMessageContaining("already exists");

        verify(campusRepository, never()).save(any());
    }

    @Test
    void update_existingCampus_returnsUpdatedDto() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(campusRepository.existsByCodeAndIdNotAndDeletedAtIsNull("MAIN", 1L)).thenReturn(false);
        when(campusRepository.save(any(Campus.class))).thenReturn(campus);
        when(campusMapper.toDto(campus)).thenReturn(campusDto);

        CampusDto result = campusService.update(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(campusMapper).updateEntity(updateRequest, campus);
        verify(campusRepository).save(campus);
    }

    @Test
    void update_nonExistentCampus_throwsEntityNotFoundException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campusService.update(999L, updateRequest))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void update_duplicateCodeOnDifferentCampus_throwsConflictException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(campusRepository.existsByCodeAndIdNotAndDeletedAtIsNull("MAIN", 1L)).thenReturn(true);

        assertThatThrownBy(() -> campusService.update(1L, updateRequest))
                .isInstanceOf(ConflictException.class);

        verify(campusRepository, never()).save(any());
    }

    @Test
    void delete_campusWithNoChildren_softDeletes() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(departmentRepository.countByCampusIdAndDeletedAtIsNull(1L)).thenReturn(0L);

        campusService.delete(1L);

        assertThat(campus.getDeletedAt()).isNotNull();
        assertThat(campus.getIsActive()).isFalse();
        verify(campusRepository).save(campus);
    }

    @Test
    void delete_campusWithActiveDepartments_throwsConflictException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(campus));
        when(departmentRepository.countByCampusIdAndDeletedAtIsNull(1L)).thenReturn(3L);

        assertThatThrownBy(() -> campusService.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("3")
                .hasMessageContaining("active department");

        assertThat(campus.getDeletedAt()).isNull();
        verify(campusRepository, never()).save(any());
    }

    @Test
    void delete_nonExistentCampus_throwsEntityNotFoundException() {
        when(campusRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campusService.delete(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
