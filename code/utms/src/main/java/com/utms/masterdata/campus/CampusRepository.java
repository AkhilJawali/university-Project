package com.utms.masterdata.campus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Long>, JpaSpecificationExecutor<Campus> {

    Optional<Campus> findByIdAndDeletedAtIsNull(Long id);

    Optional<Campus> findByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndIdNotAndDeletedAtIsNull(String code, Long id);

    Page<Campus> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT c FROM Campus c WHERE c.deletedAt IS NULL AND c.isActive = :isActive")
    Page<Campus> findAllByIsActiveAndDeletedAtIsNull(@Param("isActive") Boolean isActive, Pageable pageable);

    @Query("SELECT c FROM Campus c WHERE c.deletedAt IS NULL " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Campus> searchByNameOrCode(@Param("search") String search, Pageable pageable);
}
