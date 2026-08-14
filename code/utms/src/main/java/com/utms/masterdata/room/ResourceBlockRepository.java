package com.utms.masterdata.room;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceBlockRepository extends JpaRepository<ResourceBlock, Long> {

    Optional<ResourceBlock> findByIdAndRoomId(Long id, Long roomId);

    List<ResourceBlock> findAllByRoomIdAndStatus(Long roomId, BlockStatus status);

    Page<ResourceBlock> findAllByRoomIdAndDeletedAtIsNull(Long roomId, Pageable pageable);

    @Query("SELECT rb FROM ResourceBlock rb WHERE rb.room.id = :roomId " +
           "AND rb.deletedAt IS NULL " +
           "AND rb.status IN ('APPROVED', 'ACTIVE') " +
           "AND rb.startDate <= :endDate " +
           "AND rb.endDate >= :startDate")
    List<ResourceBlock> findActiveBlocksInDateRange(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(rb) FROM ResourceBlock rb WHERE rb.room.id = :roomId " +
           "AND rb.deletedAt IS NULL " +
           "AND rb.status IN ('APPROVED', 'ACTIVE') " +
           "AND rb.startDate <= :endDate " +
           "AND rb.endDate >= :startDate")
    long countActiveBlocksInDateRange(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
