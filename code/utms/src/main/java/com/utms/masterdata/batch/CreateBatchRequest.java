package com.utms.masterdata.batch;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateBatchRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String name;

    @NotNull
    private Long programId;

    @NotBlank
    private String academicYear;

    @NotNull
    @Min(1)
    @Max(16)
    private Integer semesterNumber;

    @NotNull
    @Min(1)
    @Max(10000)
    private Integer strength;
}
