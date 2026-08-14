package com.utms.masterdata.campus;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.department.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampusService {

    private final CampusRepository campusRepository;
    private final DepartmentRepository departmentRepository;
    private final CampusMapper campusMapper;

    @Transactional(readOnly = true)
    public Page<CampusDto> findAll(Pageable pageable) {
        return campusRepository.findAllByDeletedAtIsNull(pageable)
                .map(campusMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<CampusDto> search(String search, Pageable pageable) {
        return campusRepository.searchByNameOrCode(search, pageable)
                .map(campusMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CampusDto findById(Long id) {
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Campus", id));
        return campusMapper.toDto(campus);
    }

    @Transactional
    public CreateResponse<CampusDto> create(CreateCampusRequest request) {
        if (campusRepository.existsByCodeAndDeletedAtIsNull(request.getCode())) {
            throw new ConflictException("Campus with code '" + request.getCode() + "' already exists");
        }

        Campus campus = campusMapper.toEntity(request);
        campus.setIsActive(true);
        Campus saved = campusRepository.save(campus);

        log.info("Campus created: id={}, code={}", saved.getId(), saved.getCode());
        return new CreateResponse<>(campusMapper.toDto(saved));
    }

    @Transactional
    public CampusDto update(Long id, UpdateCampusRequest request) {
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Campus", id));

        if (campusRepository.existsByCodeAndIdNotAndDeletedAtIsNull(request.getCode(), id)) {
            throw new ConflictException("Campus with code '" + request.getCode() + "' already exists");
        }

        campusMapper.updateEntity(request, campus);
        Campus saved = campusRepository.save(campus);

        log.info("Campus updated: id={}, code={}", saved.getId(), saved.getCode());
        return campusMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Campus", id));

        long activeChildren = departmentRepository.countByCampusIdAndDeletedAtIsNull(id);
        if (activeChildren > 0) {
            throw new ConflictException("Cannot delete campus: " + activeChildren
                    + " active department(s) still reference it");
        }

        campus.softDelete();
        campusRepository.save(campus);
        log.info("Campus soft-deleted: id={}", id);
    }
}
