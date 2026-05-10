package com.smartcivic.backend.dto;

import lombok.Data;
import java.util.Set;

@Data
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String name;
    private String contact;
    private String district;
    private String area;
    private String department;
    private Set<String> roles;
}
