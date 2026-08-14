package com.utms.masterdata.faculty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
public class FacultyAvailabilityDto {
    private Long id;
    private Long facultyId;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private ConstraintType constraintType;
}
