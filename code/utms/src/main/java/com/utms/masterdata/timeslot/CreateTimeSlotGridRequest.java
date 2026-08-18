package com.utms.masterdata.timeslot;

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
public class CreateTimeSlotGridRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String name;

    @NotNull
    private Long campusId;

    @NotNull
    private LocalDate effectiveFrom;
}
