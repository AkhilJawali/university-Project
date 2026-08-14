package com.utms.masterdata.program;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ProgramDto {
    private Long id;
    private String name;
    private String code;
    private Long departmentId;
    private String departmentName;
    private Integer durationYears;
    private Integer totalSemesters;
    private DegreeType degreeType;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
