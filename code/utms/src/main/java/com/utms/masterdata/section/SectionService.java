package com.utms.masterdata.section;

import com.utms.common.dto.CreateResponse;
import com.utms.common.exception.ConflictException;
import com.utms.common.exception.EntityNotFoundException;
import com.utms.common.exception.ValidationException;
import com.utms.masterdata.batch.Batch;
import com.utms.masterdata.batch.BatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectionService {

    private final SectionRepository sectionRepository;
    private final BatchRepository batchRepository;
    private final SectionMapper sectionMapper;

    @Transactional(readOnly = true)
    public Page<SectionDto> findAll(Pageable pageable) {
        return sectionRepository.findAllByDeletedAtIsNull(pageable)
                .map(sectionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<SectionDto> findByBatchId(Long batchId, Pageable pageable) {
        return sectionRepository.findAllByBatchIdAndDeletedAtIsNull(batchId, pageable)
                .map(sectionMapper::toDto);
    }

    @Transactional(readOnly = true)
    public SectionDto findById(Long id) {
        Section section = sectionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Section", id));
        return sectionMapper.toDto(section);
    }

    @Transactional
    public CreateResponse<SectionDto> create(CreateSectionRequest request) {
        Batch batch = batchRepository.findByIdAndDeletedAtIsNull(request.getBatchId())
                .orElseThrow(() -> new ValidationException("batchId",
                        "Batch not found or has been deleted", request.getBatchId()));

        if (sectionRepository.existsByNameAndBatchIdAndDeletedAtIsNull(request.getName(), request.getBatchId())) {
            throw new ConflictException("Section with name '" + request.getName()
                    + "' already exists in batch " + batch.getName());
        }

        Section section = sectionMapper.toEntity(request);
        section.setBatch(batch);
        section.setIsActive(true);
        Section saved = sectionRepository.save(section);

        List<String> warnings = checkStrengthWarning(request.getBatchId(), batch.getStrength());

        log.info("Section created: id={}, name={}, batchId={}", saved.getId(), saved.getName(), batch.getId());
        return new CreateResponse<>(sectionMapper.toDto(saved), warnings);
    }

    @Transactional
    public CreateResponse<SectionDto> update(Long id, CreateSectionRequest request) {
        Section section = sectionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Section", id));

        Batch batch = batchRepository.findByIdAndDeletedAtIsNull(request.getBatchId())
                .orElseThrow(() -> new ValidationException("batchId",
                        "Batch not found or has been deleted", request.getBatchId()));

        if (sectionRepository.existsByNameAndBatchIdAndIdNotAndDeletedAtIsNull(
                request.getName(), request.getBatchId(), id)) {
            throw new ConflictException("Section with name '" + request.getName()
                    + "' already exists in batch " + batch.getName());
        }

        sectionMapper.updateEntity(request, section);
        section.setBatch(batch);
        Section saved = sectionRepository.save(section);

        List<String> warnings = checkStrengthWarning(request.getBatchId(), batch.getStrength());

        log.info("Section updated: id={}, name={}", saved.getId(), saved.getName());
        return new CreateResponse<>(sectionMapper.toDto(saved), warnings);
    }

    @Transactional
    public void delete(Long id) {
        Section section = sectionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Section", id));

        section.softDelete();
        sectionRepository.save(section);
        log.info("Section soft-deleted: id={}", id);
    }

    private List<String> checkStrengthWarning(Long batchId, int batchStrength) {
        List<String> warnings = new ArrayList<>();
        int totalSectionStrength = sectionRepository.sumStrengthByBatchId(batchId);
        if (totalSectionStrength > batchStrength) {
            warnings.add(String.format(
                    "Total section strength (%d) exceeds batch strength (%d). "
                            + "This is allowed but may indicate a configuration issue.",
                    totalSectionStrength, batchStrength));
        }
        return warnings;
    }
}
