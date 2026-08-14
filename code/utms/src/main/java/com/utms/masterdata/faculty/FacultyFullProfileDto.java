package com.utms.masterdata.faculty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class FacultyFullProfileDto {
    private FacultyDto faculty;
    private List<FacultyAvailabilityDto> availabilities;
    private List<CompetencyDto> competencies;
    private List<CampusAssociationDto> campusAssociations;

    @Getter
    @Setter
    @Builder
    public static class CompetencyDto {
        private Long id;
        private Long courseId;
        private String courseCode;
        private String courseName;
    }

    @Getter
    @Setter
    @Builder
    public static class CampusAssociationDto {
        private Long id;
        private Long campusId;
        private String campusName;
        private Integer travelTimeMinutes;
    }
}
