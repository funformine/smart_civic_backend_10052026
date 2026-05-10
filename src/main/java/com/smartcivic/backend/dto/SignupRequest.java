package com.smartcivic.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class SignupRequest {
    @NotBlank
    @Size(min = 3, max = 20)
    private String username;

    @NotBlank
    private String name;

    @NotBlank
    @Size(max = 50)
    @Email
    private String email;

    private String contact;
    private String district;
    private String area;

    private Set<String> role;

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;
}
