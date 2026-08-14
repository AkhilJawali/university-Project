package com.utms.masterdata.faculty;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateCompetencyRequest {

    @NotNull
    private Long courseId;
}
