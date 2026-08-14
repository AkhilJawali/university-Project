package com.utms.masterdata.room;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    Optional<Room> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCodeAndCampusIdAndDeletedAtIsNull(String code, Long campusId);

    boolean existsByCodeAndCampusIdAndIdNotAndDeletedAtIsNull(String code, Long campusId, Long id);

    Page<Room> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.deletedAt IS NULL " +
           "AND (:campusId IS NULL OR r.campus.id = :campusId) " +
           "AND (:building IS NULL OR r.building = :building) " +
           "AND (:roomType IS NULL OR r.roomType = :roomType) " +
           "AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)")
    Page<Room> findAllWithFilters(
            @Param("campusId") Long campusId,
            @Param("building") String building,
            @Param("roomType") RoomType roomType,
            @Param("minCapacity") Integer minCapacity,
            Pageable pageable);

    @Query("SELECT r FROM Room r WHERE r.deletedAt IS NULL " +
           "AND (LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(r.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(r.building) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Room> searchByNameOrCodeOrBuilding(@Param("search") String search, Pageable pageable);
}
