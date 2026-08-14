package com.utms.masterdata.batch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long>, JpaSpecificationExecutor<Batch> {

    Optional<Batch> findByIdAndDeletedAtIsNull(Long id);

    long countByProgramIdAndDeletedAtIsNull(Long programId);

    Page<Batch> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Batch> findAllByProgramIdAndDeletedAtIsNull(Long programId, Pageable pageable);

    List<Batch> findAllByProgramIdAndDeletedAtIsNull(Long programId);
}
