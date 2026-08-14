package com.utms.masterdata.department;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.campus.Campus;
import com.utms.masterdata.campus.CampusRepository;
import com.utms.masterdata.program.ProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CampusRepository campusRepository;
    private final ProgramRepository programRepository;
    private final DepartmentMapper departmentMapper;

    @Transactional(readOnly = true)
    public Page<DepartmentDto> findAll(Pageable pageable) {
        return departmentRepository.findAllByDeletedAtIsNull(pageable)
                .map(departmentMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<DepartmentDto> findByCampusId(Long campusId, Pageable pageable) {
        return departmentRepository.findAllByCampusIdAndDeletedAtIsNull(campusId, pageable)
                .map(departmentMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<DepartmentDto> search(String search, Pageable pageable) {
        return departmentRepository.searchByNameOrCode(search, pageable)
                .map(departmentMapper::toDto);
    }

    @Transactional(readOnly = true)
    public DepartmentDto findById(Long id) {
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Department", id));
        return departmentMapper.toDto(department);
    }

    @Transactional
    public CreateResponse<DepartmentDto> create(CreateDepartmentRequest request) {
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(request.getCampusId())
                .orElseThrow(() -> new ValidationException("campusId",
                        "Campus not found or has been deleted", request.getCampusId()));

        if (departmentRepository.existsByCodeAndCampusIdAndDeletedAtIsNull(request.getCode(), request.getCampusId())) {
            throw new ConflictException("Department with code '" + request.getCode()
                    + "' already exists in campus " + campus.getName());
        }

        Department department = departmentMapper.toEntity(request);
        department.setCampus(campus);
        department.setIsActive(true);
        Department saved = departmentRepository.save(department);

        log.info("Department created: id={}, code={}, campusId={}", saved.getId(), saved.getCode(), campus.getId());
        return new CreateResponse<>(departmentMapper.toDto(saved));
    }

    @Transactional
    public DepartmentDto update(Long id, CreateDepartmentRequest request) {
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Department", id));

        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(request.getCampusId())
                .orElseThrow(() -> new ValidationException("campusId",
                        "Campus not found or has been deleted", request.getCampusId()));

        if (departmentRepository.existsByCodeAndCampusIdAndIdNotAndDeletedAtIsNull(
                request.getCode(), request.getCampusId(), id)) {
            throw new ConflictException("Department with code '" + request.getCode()
                    + "' already exists in campus " + campus.getName());
        }

        departmentMapper.updateEntity(request, department);
        department.setCampus(campus);
        Department saved = departmentRepository.save(department);

        log.info("Department updated: id={}, code={}", saved.getId(), saved.getCode());
        return departmentMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Department", id));

        long activeChildren = programRepository.countByDepartmentIdAndDeletedAtIsNull(id);
        if (activeChildren > 0) {
            throw new ConflictException("Cannot delete department: " + activeChildren
                    + " active program(s) still reference it");
        }

        department.softDelete();
        departmentRepository.save(department);
        log.info("Department soft-deleted: id={}", id);
    }
}
