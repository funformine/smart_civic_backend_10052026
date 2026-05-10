package com.smartcivic.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    private String department;
    private String area;
    private String district;

    public JwtResponse(String accessToken, Long id, String username, String email, List<String> roles,
            String department, String area, String district) {
        this.token = accessToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.department = department;
        this.area = area;
        this.district = district;
    }
}
