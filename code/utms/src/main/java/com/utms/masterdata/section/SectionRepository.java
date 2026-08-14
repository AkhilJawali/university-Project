package com.utms.masterdata.section;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long>, JpaSpecificationExecutor<Section> {

    Optional<Section> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByNameAndBatchIdAndDeletedAtIsNull(String name, Long batchId);

    boolean existsByNameAndBatchIdAndIdNotAndDeletedAtIsNull(String name, Long batchId, Long id);

    long countByBatchIdAndDeletedAtIsNull(Long batchId);

    Page<Section> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Section> findAllByBatchIdAndDeletedAtIsNull(Long batchId, Pageable pageable);

    List<Section> findAllByBatchIdAndDeletedAtIsNull(Long batchId);

    @Query("SELECT COALESCE(SUM(s.strength), 0) FROM Section s WHERE s.batch.id = :batchId AND s.deletedAt IS NULL")
    int sumStrengthByBatchId(@Param("batchId") Long batchId);
}
