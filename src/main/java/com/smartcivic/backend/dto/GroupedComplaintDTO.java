package com.smartcivic.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupedComplaintDTO {
    private String category;
    private String subCategory;
    private String location;
    private String status;
    private Long reportersCount;
    private List<Long> complaintIds; // Store all IDs in this group
    private String description; // Latest description
    private String imageName; // Latest image
    private String assignedHeadName;
    private java.util.Set<com.smartcivic.backend.dto.UserDTO> assignedEmployees;
    private java.time.LocalDateTime slaTimeline;
}
