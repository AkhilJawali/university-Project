package com.utms.masterdata.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class CreateResourceBlockRequest {

    @NotNull
    private Long roomId;

    @NotBlank
    @Size(min = 1, max = 500)
    private String reason;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private LocalTime startTime;

    private LocalTime endTime;
}
