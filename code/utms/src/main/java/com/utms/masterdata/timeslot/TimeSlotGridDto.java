package com.utms.masterdata.timeslot;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class TimeSlotGridDto {

    private Long id;
    private String name;
    private Long campusId;
    private String campusName;
    private LocalDate effectiveFrom;
    private Boolean isActive;
    private List<SlotDefinitionDto> slots;
    private List<WorkingDayDto> workingDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
