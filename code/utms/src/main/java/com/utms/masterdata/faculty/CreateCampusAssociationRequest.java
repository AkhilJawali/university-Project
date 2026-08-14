package com.utms.masterdata.faculty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateCampusAssociationRequest {

    @NotNull
    private Long campusId;

    @NotNull
    @Min(1)
    private Integer travelTimeMinutes;
}
