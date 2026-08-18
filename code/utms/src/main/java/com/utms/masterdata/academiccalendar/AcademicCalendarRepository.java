package com.utms.masterdata.academiccalendar;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AcademicCalendarRepository extends JpaRepository<AcademicCalendar, Long> {

    Optional<AcademicCalendar> findByIdAndDeletedAtIsNull(Long id);

    Page<AcademicCalendar> findAllByDeletedAtIsNull(Pageable pageable);

    Page<AcademicCalendar> findByCampusIdAndDeletedAtIsNull(Long campusId, Pageable pageable);

    @Query("""
        SELECT c FROM AcademicCalendar c
        WHERE c.deletedAt IS NULL
          AND (:campusId IS NULL OR c.campus.id = :campusId)
          AND (:academicYear IS NULL OR c.academicYear = :academicYear)
          AND (:semesterType IS NULL OR c.semesterType = :semesterType)
        """)
    Page<AcademicCalendar> findWithFilters(
            @Param("campusId") Long campusId,
            @Param("academicYear") String academicYear,
            @Param("semesterType") SemesterType semesterType,
            Pageable pageable);

    @Query("""
        SELECT COUNT(c) > 0 FROM AcademicCalendar c
        WHERE c.campus.id = :campusId
          AND c.semesterType = :semesterType
          AND c.deletedAt IS NULL
          AND c.startDate < :endDate
          AND c.endDate > :startDate
        """)
    boolean existsOverlapping(
            @Param("campusId") Long campusId,
            @Param("semesterType") SemesterType semesterType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT COUNT(c) > 0 FROM AcademicCalendar c
        WHERE c.campus.id = :campusId
          AND c.semesterType = :semesterType
          AND c.deletedAt IS NULL
          AND c.startDate < :endDate
          AND c.endDate > :startDate
          AND c.id <> :excludeId
        """)
    boolean existsOverlappingExcluding(
            @Param("campusId") Long campusId,
            @Param("semesterType") SemesterType semesterType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId);
}
