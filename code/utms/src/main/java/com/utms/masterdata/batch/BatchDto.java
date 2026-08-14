package com.utms.masterdata.batch;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BatchDto {
    private Long id;
    private String name;
    private Long programId;
    private String programName;
    private String academicYear;
    private Integer semesterNumber;
    private Integer strength;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
