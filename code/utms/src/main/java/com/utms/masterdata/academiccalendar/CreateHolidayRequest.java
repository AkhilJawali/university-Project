package com.utms.masterdata.academiccalendar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class CreateHolidayRequest {

    @NotBlank
    @Size(min = 1, max = 100)
    private String name;

    @NotNull
    private LocalDate date;

    @NotNull
    private DayType dayType;

    @NotNull
    private Boolean isRecurring;
}
