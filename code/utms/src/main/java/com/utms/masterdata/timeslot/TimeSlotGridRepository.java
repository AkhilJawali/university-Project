package com.utms.masterdata.timeslot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TimeSlotGridRepository extends JpaRepository<TimeSlotGrid, Long> {

    Optional<TimeSlotGrid> findByIdAndDeletedAtIsNull(Long id);

    Page<TimeSlotGrid> findAllByDeletedAtIsNull(Pageable pageable);

    Page<TimeSlotGrid> findByCampusIdAndDeletedAtIsNull(Long campusId, Pageable pageable);

    @Query("""
        SELECT g FROM TimeSlotGrid g
        WHERE g.deletedAt IS NULL
          AND (:campusId IS NULL OR g.campus.id = :campusId)
          AND (:isActive IS NULL OR g.isActive = :isActive)
        """)
    Page<TimeSlotGrid> findWithFilters(
            @Param("campusId") Long campusId,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    Optional<TimeSlotGrid> findByCampusIdAndIsActiveTrueAndDeletedAtIsNull(Long campusId);

    @Modifying
    @Query("""
        UPDATE TimeSlotGrid g SET g.isActive = false, g.updatedAt = CURRENT_TIMESTAMP, g.updatedBy = :updatedBy
        WHERE g.campus.id = :campusId AND g.isActive = true AND g.deletedAt IS NULL
        """)
    void deactivateAllForCampus(@Param("campusId") Long campusId, @Param("updatedBy") String updatedBy);
}
