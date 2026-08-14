package com.utms.masterdata.program;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateProgramRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String name;

    @NotBlank
    @Size(min = 2, max = 20)
    @Pattern(regexp = "^[A-Z0-9\\-]+$", message = "Code must be uppercase alphanumeric with hyphens only")
    private String code;

    @NotNull
    private Long departmentId;

    @NotNull
    @Min(1)
    @Max(8)
    private Integer durationYears;

    @NotNull
    @Min(1)
    @Max(16)
    private Integer totalSemesters;

    @NotNull
    private DegreeType degreeType;
}
