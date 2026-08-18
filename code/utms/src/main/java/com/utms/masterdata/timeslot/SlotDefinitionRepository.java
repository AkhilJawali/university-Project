package com.utms.masterdata.timeslot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotDefinitionRepository extends JpaRepository<SlotDefinition, Long> {

    Optional<SlotDefinition> findByIdAndGridIdAndDeletedAtIsNull(Long id, Long gridId);

    List<SlotDefinition> findByGridIdAndDeletedAtIsNullOrderBySlotNumberAsc(Long gridId);

    // TODO: DB constraint on (grid_id, slot_number) is not a partial unique index — it includes soft-deleted rows.
    // A migration to replace it with a partial index (WHERE deleted_at IS NULL) should be added in a future sprint.
    @Query("SELECT COUNT(s) > 0 FROM SlotDefinition s WHERE s.grid.id = :gridId AND s.slotNumber = :slotNumber AND s.deletedAt IS NULL")
    boolean existsByGridIdAndSlotNumberAndNotDeleted(@Param("gridId") Long gridId, @Param("slotNumber") Integer slotNumber);

    long countByGridIdAndDeletedAtIsNull(Long gridId);

    @Query("""
        SELECT COUNT(s) > 0 FROM SlotDefinition s
        WHERE s.grid.id = :gridId
          AND s.deletedAt IS NULL
          AND s.startTime < :endTime
          AND s.endTime > :startTime
        """)
    boolean existsOverlapping(
            @Param("gridId") Long gridId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("""
        SELECT COUNT(s) > 0 FROM SlotDefinition s
        WHERE s.grid.id = :gridId
          AND s.deletedAt IS NULL
          AND s.startTime < :endTime
          AND s.endTime > :startTime
          AND s.id <> :excludeId
        """)
    boolean existsOverlappingExcluding(
            @Param("gridId") Long gridId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);
}
