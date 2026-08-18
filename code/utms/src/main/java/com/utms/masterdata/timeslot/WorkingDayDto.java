package com.utms.masterdata.timeslot;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class WorkingDayDto {

    private Integer dayOfWeek;
    private String dayName;
    private Boolean isWorkingDay;
}
