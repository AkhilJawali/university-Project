package com.utms.masterdata.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CreateCourseRequest {

    @NotBlank
    @Size(min = 2, max = 20)
    @Pattern(regexp = "^[A-Z0-9\\-]+$", message = "Code must be uppercase alphanumeric with hyphens only")
    private String code;

    @NotBlank
    @Size(min = 1, max = 200)
    private String name;

    @NotNull
    private Long departmentId;

    @NotNull
    @Min(0)
    @Max(10)
    private Integer lectureHours;

    @NotNull
    @Min(0)
    @Max(10)
    private Integer tutorialHours;

    @NotNull
    @Min(0)
    @Max(10)
    private Integer practicalHours;

    @NotNull
    @Min(1)
    @Max(20)
    private Integer creditHours;

    @NotNull
    private CourseType courseType;

    private Boolean isCrossListed;

    @Size(max = 10)
    private List<Long> prerequisites;

    @Size(max = 10)
    private List<@Size(max = 50) String> equipmentTags;
}
