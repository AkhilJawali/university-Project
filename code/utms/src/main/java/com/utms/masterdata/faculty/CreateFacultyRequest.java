package com.utms.masterdata.faculty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CreateFacultyRequest {

    @NotBlank
    @Size(min = 1, max = 20)
    private String employeeId;

    @NotBlank
    @Size(min = 1, max = 100)
    private String firstName;

    @NotBlank
    @Size(min = 1, max = 100)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    @Size(max = 15)
    private String phone;

    @NotNull
    private Long departmentId;

    @NotNull
    private Cadre cadre;

    @Size(max = 200)
    private String qualification;
}
