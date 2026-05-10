package com.smartcivic.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@lombok.EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @lombok.EqualsAndHashCode.Include
    private Long id;

    private String category; // Water, Power, Road, etc.
    private String subCategory; // Specific issue type

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_url")
    private String photoUrl; // Keeping for URL if needed

    @Column(name = "image_name")
    private String imageName; // For stored image name
    private String location;

    private String status; // Pending, Accepted, Rejected, In Progress, Resolved

    private LocalDateTime raisedDate;

    @Column(name = "created_at", updatable = false)
    @org.hibernate.annotations.CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime slaTimeline;

    private String rejectionReason;
    private String resolutionNotes;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reported_uid", nullable = false)
    private User reporter;

    @ManyToOne
    @JoinColumn(name = "assigned_sub_admin_id")
    private User assignedSubAdmin;

    private String assignedHeadName;
    private String assignedHeadPhone;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Builder.Default
    private String priorityLevel = "Not Set"; // Not Set, Primary, Secondary, Ternary
    @Builder.Default
    private boolean isSlaAssigned = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "complaint_assigned_employees", joinColumns = @JoinColumn(name = "complaint_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private java.util.Set<User> assignedEmployees;

    private String completionProof; // String representation of the proof (e.g., image name)
}
