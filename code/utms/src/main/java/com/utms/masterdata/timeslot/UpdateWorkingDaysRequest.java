package com.utms.masterdata.timeslot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class UpdateWorkingDaysRequest {

    @NotEmpty
    @Size(min = 7, max = 7)
    @Valid
    private List<WorkingDayEntry> days;

    @Getter
    @Setter
    @Builder
    public static class WorkingDayEntry {

        @NotNull
        @Min(1)
        @Max(7)
        private Integer dayOfWeek;

        @NotNull
        private Boolean isWorkingDay;
    }
}
