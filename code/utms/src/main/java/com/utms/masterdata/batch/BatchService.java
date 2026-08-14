package com.utms.masterdata.batch;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.program.Program;
import com.utms.masterdata.program.ProgramRepository;
import com.utms.masterdata.section.SectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchService {

    private final BatchRepository batchRepository;
    private final ProgramRepository programRepository;
    private final SectionRepository sectionRepository;
    private final BatchMapper batchMapper;

    @Transactional(readOnly = true)
    public Page<BatchDto> findAll(Pageable pageable) {
        return batchRepository.findAllByDeletedAtIsNull(pageable)
                .map(batchMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<BatchDto> findByProgramId(Long programId, Pageable pageable) {
        return batchRepository.findAllByProgramIdAndDeletedAtIsNull(programId, pageable)
                .map(batchMapper::toDto);
    }

    @Transactional(readOnly = true)
    public BatchDto findById(Long id) {
        Batch batch = batchRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Batch", id));
        return batchMapper.toDto(batch);
    }

    @Transactional
    public CreateResponse<BatchDto> create(CreateBatchRequest request) {
        Program program = programRepository.findByIdAndDeletedAtIsNull(request.getProgramId())
                .orElseThrow(() -> new ValidationException("programId",
                        "Program not found or has been deleted", request.getProgramId()));

        Batch batch = batchMapper.toEntity(request);
        batch.setProgram(program);
        batch.setIsActive(true);
        Batch saved = batchRepository.save(batch);

        log.info("Batch created: id={}, name={}, programId={}", saved.getId(), saved.getName(), program.getId());
        return new CreateResponse<>(batchMapper.toDto(saved));
    }

    @Transactional
    public BatchDto update(Long id, CreateBatchRequest request) {
        Batch batch = batchRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Batch", id));

        Program program = programRepository.findByIdAndDeletedAtIsNull(request.getProgramId())
                .orElseThrow(() -> new ValidationException("programId",
                        "Program not found or has been deleted", request.getProgramId()));

        batchMapper.updateEntity(request, batch);
        batch.setProgram(program);
        Batch saved = batchRepository.save(batch);

        log.info("Batch updated: id={}, name={}", saved.getId(), saved.getName());
        return batchMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Batch batch = batchRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Batch", id));

        long activeChildren = sectionRepository.countByBatchIdAndDeletedAtIsNull(id);
        if (activeChildren > 0) {
            throw new ConflictException("Cannot delete batch: " + activeChildren
                    + " active section(s) still reference it");
        }

        batch.softDelete();
        batchRepository.save(batch);
        log.info("Batch soft-deleted: id={}", id);
    }
}
