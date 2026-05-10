package com.smartcivic.backend.dto;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String contact;
    private String district;
    private String area;
    private Set<String> roles;
}
