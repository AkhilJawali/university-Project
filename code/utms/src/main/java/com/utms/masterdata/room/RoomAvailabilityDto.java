package com.utms.masterdata.room;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
public class RoomAvailabilityDto {

    private Long roomId;
    private String roomName;
    private LocalDate date;
    private List<TimeSlot> availableSlots;
    private List<TimeSlot> blockedSlots;

    @Getter
    @Setter
    @Builder
    public static class TimeSlot {
        private LocalTime startTime;
        private LocalTime endTime;
        private String reason;
    }
}
