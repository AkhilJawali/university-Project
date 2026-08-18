package com.utms.masterdata.timeslot;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class SlotDefinitionDto {

    private Long id;
    private Long gridId;
    private Integer slotNumber;
    private LocalTime startTime;
    private LocalTime endTime;
    private SlotType slotType;
    private Integer durationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
