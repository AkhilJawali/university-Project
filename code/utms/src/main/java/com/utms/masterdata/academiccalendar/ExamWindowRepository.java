package com.utms.masterdata.academiccalendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamWindowRepository extends JpaRepository<ExamWindow, Long> {

    Optional<ExamWindow> findByIdAndCalendarIdAndDeletedAtIsNull(Long id, Long calendarId);

    List<ExamWindow> findByCalendarIdAndDeletedAtIsNull(Long calendarId);

    long countByCalendarIdAndDeletedAtIsNull(Long calendarId);
}
