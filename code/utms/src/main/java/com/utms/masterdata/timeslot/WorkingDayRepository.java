package com.utms.masterdata.timeslot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkingDayRepository extends JpaRepository<WorkingDay, Long> {

    List<WorkingDay> findByGridIdOrderByDayOfWeekAsc(Long gridId);

    Optional<WorkingDay> findByGridIdAndDayOfWeek(Long gridId, Integer dayOfWeek);

    long countByGridIdAndIsWorkingDayTrue(Long gridId);
}
