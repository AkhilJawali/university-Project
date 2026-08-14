package com.utms.masterdata.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CreateRoomRequest {

    @NotBlank
    @Size(min = 1, max = 20)
    private String code;

    @NotBlank
    @Size(min = 1, max = 200)
    private String name;

    @NotNull
    private Long campusId;

    @NotBlank
    @Size(min = 1, max = 100)
    private String building;

    @Size(max = 20)
    private String floor;

    @NotNull
    @Min(1)
    @Max(5000)
    private Integer capacity;

    @NotNull
    private RoomType roomType;

    @Size(max = 20)
    private List<String> equipmentTags;
}
