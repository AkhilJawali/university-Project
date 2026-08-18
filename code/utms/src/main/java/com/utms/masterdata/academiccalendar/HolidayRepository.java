package com.utms.masterdata.academiccalendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    Optional<Holiday> findByIdAndCalendarIdAndDeletedAtIsNull(Long id, Long calendarId);

    List<Holiday> findByCalendarIdAndDeletedAtIsNull(Long calendarId);

    // TODO: DB constraint (uq_holidays_calendar_date) is not a partial unique index — it includes soft-deleted rows.
    // A migration to replace it with a partial index (WHERE deleted_at IS NULL) should be added in a future sprint.
    @Query("SELECT COUNT(h) > 0 FROM Holiday h WHERE h.calendar.id = :calendarId AND h.date = :date AND h.deletedAt IS NULL")
    boolean existsByCalendarIdAndDateAndNotDeleted(@Param("calendarId") Long calendarId, @Param("date") LocalDate date);

    long countByCalendarIdAndDeletedAtIsNull(Long calendarId);
}
