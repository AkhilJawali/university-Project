package com.utms.masterdata.section;

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
public class CreateSectionRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String name;

    @NotNull
    private Long batchId;

    @NotNull
    @Min(1)
    @Max(10000)
    private Integer strength;
}
