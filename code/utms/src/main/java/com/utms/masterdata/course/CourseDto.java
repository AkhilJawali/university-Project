package com.utms.masterdata.course;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class CourseDto {
    private Long id;
    private String code;
    private String name;
    private Long departmentId;
    private String departmentName;
    private Integer lectureHours;
    private Integer tutorialHours;
    private Integer practicalHours;
    private Integer creditHours;
    private CourseType courseType;
    private Boolean isCrossListed;
    private List<Long> prerequisites;
    private List<String> equipmentTags;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
