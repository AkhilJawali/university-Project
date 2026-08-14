package com.utms.masterdata.section;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class SectionDto {
    private Long id;
    private String name;
    private Long batchId;
    private String batchName;
    private Integer strength;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
