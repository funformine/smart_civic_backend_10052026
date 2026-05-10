package com.smartcivic.backend.dto;

import lombok.Data;

@Data
public class AssignSubAdminRequest {
    private Long subAdminId;
    private String slaDate; // ISO format or LocalDateTime
}
