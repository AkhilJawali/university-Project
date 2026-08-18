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
public class CreateSpecialPeriodRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String name;

    @NotNull
    private PeriodType periodType;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
