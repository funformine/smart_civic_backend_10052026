package com.smartcivic.backend.dto;

import lombok.Data;

@Data
public class EditUserRequest {
    private String name;
    private String contact;
    private String district;
    private String area;
    private String department;
}
