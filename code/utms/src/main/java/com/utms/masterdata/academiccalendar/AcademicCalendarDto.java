package com.utms.masterdata.academiccalendar;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AcademicCalendarDto {

    private Long id;
    private String name;
    private Long campusId;
    private String campusName;
    private String academicYear;
    private SemesterType semesterType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private Integer holidayCount;
    private Integer examWindowCount;
    private Integer specialPeriodCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
