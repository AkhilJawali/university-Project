package com.utms.masterdata.faculty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyAvailabilityRepository extends JpaRepository<FacultyAvailability, Long> {

    List<FacultyAvailability> findAllByFacultyIdAndDeletedAtIsNull(Long facultyId);

    Optional<FacultyAvailability> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT fa FROM FacultyAvailability fa WHERE fa.faculty.id = :facultyId " +
           "AND fa.dayOfWeek = :dayOfWeek " +
           "AND fa.startTime < :endTime AND fa.endTime > :startTime " +
           "AND fa.deletedAt IS NULL")
    List<FacultyAvailability> findOverlapping(
            @Param("facultyId") Long facultyId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}
