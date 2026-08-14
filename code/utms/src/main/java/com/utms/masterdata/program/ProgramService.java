package com.utms.masterdata.program;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.batch.BatchRepository;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramService {

    private final ProgramRepository programRepository;
    private final DepartmentRepository departmentRepository;
    private final BatchRepository batchRepository;
    private final ProgramMapper programMapper;

    @Transactional(readOnly = true)
    public Page<ProgramDto> findAll(Pageable pageable) {
        return programRepository.findAllByDeletedAtIsNull(pageable)
                .map(programMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProgramDto> findByDepartmentId(Long departmentId, Pageable pageable) {
        return programRepository.findAllByDepartmentIdAndDeletedAtIsNull(departmentId, pageable)
                .map(programMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ProgramDto findById(Long id) {
        Program program = programRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Program", id));
        return programMapper.toDto(program);
    }

    @Transactional
    public CreateResponse<ProgramDto> create(CreateProgramRequest request) {
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(request.getDepartmentId())
                .orElseThrow(() -> new ValidationException("departmentId",
                        "Department not found or has been deleted", request.getDepartmentId()));

        if (programRepository.existsByCodeAndDepartmentIdAndDeletedAtIsNull(
                request.getCode(), request.getDepartmentId())) {
            throw new ConflictException("Program with code '" + request.getCode()
                    + "' already exists in department " + department.getName());
        }

        Program program = programMapper.toEntity(request);
        program.setDepartment(department);
        program.setIsActive(true);
        Program saved = programRepository.save(program);

        log.info("Program created: id={}, code={}, departmentId={}", saved.getId(), saved.getCode(), department.getId());
        return new CreateResponse<>(programMapper.toDto(saved));
    }

    @Transactional
    public ProgramDto update(Long id, CreateProgramRequest request) {
        Program program = programRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Program", id));

        Department department = departmentRepository.findByIdAndDeletedAtIsNull(request.getDepartmentId())
                .orElseThrow(() -> new ValidationException("departmentId",
                        "Department not found or has been deleted", request.getDepartmentId()));

        if (programRepository.existsByCodeAndDepartmentIdAndIdNotAndDeletedAtIsNull(
                request.getCode(), request.getDepartmentId(), id)) {
            throw new ConflictException("Program with code '" + request.getCode()
                    + "' already exists in department " + department.getName());
        }

        programMapper.updateEntity(request, program);
        program.setDepartment(department);
        Program saved = programRepository.save(program);

        log.info("Program updated: id={}, code={}", saved.getId(), saved.getCode());
        return programMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Program program = programRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Program", id));

        long activeChildren = batchRepository.countByProgramIdAndDeletedAtIsNull(id);
        if (activeChildren > 0) {
            throw new ConflictException("Cannot delete program: " + activeChildren
                    + " active batch(es) still reference it");
        }

        program.softDelete();
        programRepository.save(program);
        log.info("Program soft-deleted: id={}", id);
    }
}
