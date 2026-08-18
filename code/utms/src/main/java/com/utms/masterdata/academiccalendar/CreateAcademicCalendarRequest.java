package com.utms.masterdata.academiccalendar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class CreateAcademicCalendarRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String name;

    @NotNull
    private Long campusId;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{4}$", message = "Must be format YYYY-YYYY")
    private String academicYear;

    @NotNull
    private SemesterType semesterType;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
