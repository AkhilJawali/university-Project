package com.utms.masterdata.academiccalendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialPeriodRepository extends JpaRepository<SpecialPeriod, Long> {

    Optional<SpecialPeriod> findByIdAndCalendarIdAndDeletedAtIsNull(Long id, Long calendarId);

    List<SpecialPeriod> findByCalendarIdAndDeletedAtIsNull(Long calendarId);

    long countByCalendarIdAndDeletedAtIsNull(Long calendarId);
}
