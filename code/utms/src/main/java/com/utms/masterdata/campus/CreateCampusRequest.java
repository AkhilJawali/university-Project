package com.utms.masterdata.campus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateCampusRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String name;

    @NotBlank
    @Size(min = 2, max = 20)
    @Pattern(regexp = "^[A-Z0-9\\-]+$", message = "Code must be uppercase alphanumeric with hyphens only")
    private String code;

    @Size(max = 500)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @NotBlank
    private String timezone;
}
