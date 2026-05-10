package com.smartcivic.backend.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.smartcivic.backend.dto.DashboardSummaryDTO;
import com.smartcivic.backend.dto.GroupedComplaintDTO;
import com.smartcivic.backend.model.Complaint;
import com.smartcivic.backend.model.User;
import com.smartcivic.backend.repository.ComplaintRepository;
import com.smartcivic.backend.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintService {
    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.smartcivic.backend.repository.IssueTypeRepository issueTypeRepository;

    public ComplaintRepository getComplaintRepository() {
        return complaintRepository;
    }

    public Complaint raiseComplaint(Complaint complaint, String username) {
        System.out.println("[COMPLAINT_DEBUG] Raising complaint for category: " + complaint.getCategory());
        System.out.println("[COMPLAINT_DEBUG] Image Name received: " + complaint.getImageName());

        User reporter = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println(
                "[COMPLAINT_DEBUG] Setting reporter: " + reporter.getUsername() + " (ID: " + reporter.getId() + ")");
        complaint.setReporter(reporter);

        // Ensure raisedDate is also set in case user looks for it
        if (complaint.getRaisedDate() == null) {
            complaint.setRaisedDate(LocalDateTime.now());
        }

        complaint.setStatus("Pending");
        System.out.println("[COMPLAINT_DEBUG] Attempting to save complaint for user: " + username);
        Complaint saved = complaintRepository.save(complaint);

        String reporterIdStr = (saved.getReporter() != null) ? String.valueOf(saved.getReporter().getId()) : "NULL";
        System.out
                .println("[COMPLAINT_DEBUG] Saved Complaint ID: " + saved.getId() + " | Reporter ID: " + reporterIdStr);
        System.out.println("[COMPLAINT_DEBUG] Created At: " + saved.getCreatedAt());
        return saved;
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    public List<Complaint> getComplaintsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Complaint> complaints = complaintRepository.findByReporter(user);
        System.out.println("[DASHBOARD_DEBUG] User " + username + " (ID: " + user.getId() + ") has " + complaints.size()
                + " complaints.");
        return complaints;
    }

    public Page<Complaint> getComplaintsByUserPaginated(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Pageable pageable = PageRequest.of(page, size, Sort.by("raisedDate").descending());
        return complaintRepository.findByReporter(user, pageable);
    }

    public List<Complaint> getComplaintsInArea(String location) {
        return complaintRepository.findByLocationContainingIgnoreCase(location);
    }

    public Complaint getComplaintById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Complaint ID cannot be null");
        }
        return complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    public Complaint updateComplaintStatus(Long id, String status, String notes) {
        return updateComplaintStatus(id, status, notes, null);
    }

    @SuppressWarnings("null")
    public Complaint updateComplaintStatus(Long id, String status, String notes, String proof) {
        Complaint complaint = getComplaintById(id);
        complaint.setStatus(status);
        if ("In Progress".equals(status) || "Resolved".equals(status)) {
            complaint.setResolutionNotes(notes);
        }
        if (proof != null) {
            complaint.setCompletionProof(proof);
        }

        // Release employees if resolved
        if ("Resolved".equals(status)) {
            if (complaint.getAssignedEmployees() != null) {
                for (User employee : complaint.getAssignedEmployees()) {
                    employee.setStatus("Not Assigned");
                    userRepository.save(employee);
                }
            }

            // Automatically add new issue names to the global list for the department
            if (complaint.getSubCategory() != null && !complaint.getSubCategory().isEmpty()) {
                String cat = complaint.getCategory();
                String sub = complaint.getSubCategory();
                if (issueTypeRepository.findByCategoryAndIssueName(cat, sub).isEmpty()) {
                    System.out.println("[COMPLAINT_DEBUG] Auto-cataloging new issue type: " + sub + " for " + cat);
                    issueTypeRepository.save(com.smartcivic.backend.model.IssueType.builder()
                            .category(cat)
                            .issueName(sub)
                            .build());
                }
            }
        }

        return complaintRepository.save(complaint);
    }

    public Complaint assignSubAdmin(Long complaintId, Long subAdminId, LocalDateTime sla) {
        Complaint complaint = getComplaintById(complaintId);
        User subAdmin = userRepository.findById(subAdminId)
                .orElseThrow(() -> new RuntimeException("Sub Admin not found"));
        complaint.setAssignedSubAdmin(subAdmin);
        complaint.setSlaTimeline(sla);
        complaint.setStatus("Accepted");
        return complaintRepository.save(complaint);
    }

    public Complaint rejectComplaint(Long id, String reason) {
        Complaint complaint = getComplaintById(id);
        complaint.setStatus("Rejected");
        complaint.setRejectionReason(reason);
        return complaintRepository.save(complaint);
    }

    public DashboardSummaryDTO getDashboardSummary(String username) {
        long start = System.currentTimeMillis();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Object[]> statusCounts = complaintRepository.findStatusCountsByReporter(user);

        long total = 0;
        long resolved = 0;
        long pending = 0;

        for (Object[] row : statusCounts) {
            String status = (String) row[0];
            long count = (Long) row[1];
            total += count;
            if ("Resolved".equalsIgnoreCase(status))
                resolved = count;
            if ("Pending".equalsIgnoreCase(status))
                pending = count;
        }

        DashboardSummaryDTO summary = DashboardSummaryDTO.builder()
                .totalReports(total)
                .resolvedReports(resolved)
                .pendingReports(pending)
                .build();

        long duration = System.currentTimeMillis() - start;
        System.out.println("[PERF_DEBUG] Dashboard Summary for " + username + " calculated in " + duration + "ms");
        return summary;
    }

    public void deleteComplaint(Long id, String username) {
        Complaint complaint = getComplaintById(id);
        if (!complaint.getReporter().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized to delete this complaint");
        }
        if (!"Pending".equalsIgnoreCase(complaint.getStatus())) {
            throw new RuntimeException("Only pending complaints can be deleted");
        }
        complaintRepository.delete(complaint);
    }

    public Complaint updateComplaint(Long id, Complaint updateDetails, String username) {
        Complaint complaint = getComplaintById(id);
        if (!complaint.getReporter().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized to edit this complaint");
        }
        if (!"Pending".equalsIgnoreCase(complaint.getStatus())) {
            throw new RuntimeException("Only pending complaints can be edited");
        }

        if (updateDetails.getCategory() != null)
            complaint.setCategory(updateDetails.getCategory());
        if (updateDetails.getSubCategory() != null)
            complaint.setSubCategory(updateDetails.getSubCategory());
        if (updateDetails.getDescription() != null)
            complaint.setDescription(updateDetails.getDescription());
        if (updateDetails.getLocation() != null)
            complaint.setLocation(updateDetails.getLocation());
        if (updateDetails.getImageName() != null)
            complaint.setImageName(updateDetails.getImageName());

        return complaintRepository.save(complaint);
    }

    public List<GroupedComplaintDTO> getGroupedComplaints(String dept) {
        List<Complaint> allDeptComplaints = complaintRepository.findByCategoryBidirectional(dept);

        java.util.Map<String, GroupedComplaintDTO> groupedMap = new java.util.HashMap<>();

        for (Complaint c : allDeptComplaints) {
            String sub = (c.getSubCategory() != null ? c.getSubCategory() : "General").trim();
            String loc = (c.getLocation() != null ? c.getLocation() : "Unknown").trim();

            // Use normalized category (dept) to ensure reports group together for the
            // sub-admin
            String key = dept.trim().toLowerCase() + "|" +
                    sub.toLowerCase() + "|" +
                    loc.toLowerCase();

            if (groupedMap.containsKey(key)) {
                GroupedComplaintDTO dto = groupedMap.get(key);
                dto.setReportersCount(dto.getReportersCount() + 1);
                dto.getComplaintIds().add(c.getId());

                // Status priority: In Progress > Pending > Accepted > Resolved > Rejected
                String cs = dto.getStatus();
                String ns = c.getStatus();
                if (ns.equals("In Progress")) {
                    dto.setStatus("In Progress");
                } else if (ns.equals("Pending") && !cs.equals("In Progress")) {
                    dto.setStatus("Pending");
                } else if (ns.equals("Accepted") && !cs.equals("In Progress") && !cs.equals("Pending")) {
                    dto.setStatus("Accepted");
                } else if (ns.equals("Resolved") && !cs.equals("In Progress") && !cs.equals("Pending")
                        && !cs.equals("Accepted")) {
                    dto.setStatus("Resolved");
                }

                if (c.getDescription() != null && c.getDescription().length() > dto.getDescription().length()) {
                    dto.setDescription(c.getDescription());
                }

                // If this complaint has assignment info, capture it for the group
                if (c.getAssignedHeadName() != null
                        || (c.getAssignedEmployees() != null && !c.getAssignedEmployees().isEmpty())) {
                    populateAssignmentInfo(dto, c);
                }
            } else {
                GroupedComplaintDTO dto = GroupedComplaintDTO.builder()
                        .category(dept.trim()) // Use normalized department name
                        .subCategory(sub)
                        .location(loc)
                        .status(c.getStatus())
                        .reportersCount(1L)
                        .complaintIds(new java.util.ArrayList<>(java.util.List.of(c.getId())))
                        .description(c.getDescription())
                        .imageName(c.getImageName())
                        .build();

                if (c.getAssignedHeadName() != null
                        || (c.getAssignedEmployees() != null && !c.getAssignedEmployees().isEmpty())) {
                    populateAssignmentInfo(dto, c);
                }
                groupedMap.put(key, dto);
            }
        }
        return new java.util.ArrayList<>(groupedMap.values());
    }

    private void populateAssignmentInfo(GroupedComplaintDTO dto, Complaint c) {
        dto.setAssignedHeadName(c.getAssignedHeadName());
        dto.setSlaTimeline(c.getSlaTimeline());
        if (c.getAssignedEmployees() != null) {
            dto.setAssignedEmployees(c.getAssignedEmployees().stream()
                    .map(emp -> com.smartcivic.backend.dto.UserDTO.builder()
                            .id(emp.getId())
                            .name(emp.getName())
                            .username(emp.getUsername())
                            .contact(emp.getContact())
                            .build())
                    .collect(java.util.stream.Collectors.toSet()));
        }
    }

    @Transactional
    public void updateGroupedStatus(List<Long> ids, String status, String notes, String proof) {
        System.out.println("[COMPLAINT_DEBUG] Bulk updating " + ids.size() + " complaints to " + status);
        for (Long id : ids) {
            updateComplaintStatus(id, status, notes, proof);
        }
    }
}
