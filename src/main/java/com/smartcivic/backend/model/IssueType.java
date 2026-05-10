package com.smartcivic.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "issue_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category; // Power, Water, Land
    private String issueName;
}
