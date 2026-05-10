package com.smartcivic.backend.controller;

import com.smartcivic.backend.dto.GroupedComplaintDTO;
import com.smartcivic.backend.model.Complaint;
import com.smartcivic.backend.model.User;
import com.smartcivic.backend.repository.ComplaintRepository;
import com.smartcivic.backend.repository.UserRepository;
import com.smartcivic.backend.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/subadmin")
@PreAuthorize("hasRole('SUB_ADMIN')")
public class SubAdminController {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    /** Returns all complaints whose category matches the sub-admin's department */
    @SuppressWarnings("null")
    @GetMapping("/issues")
    public ResponseEntity<List<Complaint>> getDepartmentIssues(Principal principal) {
        User subAdmin = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String dept = subAdmin.getDepartment();
        if (dept == null || dept.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(complaintRepository.findByCategoryBidirectional(dept.trim()));
    }

    /** Returns grouped complaints for the department */
    @GetMapping("/grouped-issues")
    public ResponseEntity<List<GroupedComplaintDTO>> getGroupedIssues(Principal principal) {
        User subAdmin = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String dept = subAdmin.getDepartment();
        if (dept == null || dept.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(complaintService.getGroupedComplaints(dept.trim()));
    }

    /** Update status for a group of complaints */
    @PutMapping("/grouped-issues/status")
    public ResponseEntity<?> updateGroupedStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> idsInt = (List<Integer>) body.get("ids");
        List<Long> ids = idsInt.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList());
        String status = (String) body.get("status");
        String notes = (String) body.get("resolutionNotes");
        String proof = (String) body.get("completionProof");

        complaintService.updateGroupedStatus(ids, status, notes, proof);
        return ResponseEntity.ok(Map.of("message", "Grouped status updated successfully"));
    }

    /** Accept a complaint — sets status to Accepted */
    @SuppressWarnings("null")
    @PutMapping("/issues/{id}/accept")
    public ResponseEntity<?> acceptIssue(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, "Accepted", null));
    }

    /** Reject a complaint — sets status to Rejected with a reason */
    @SuppressWarnings("null")
    @PutMapping("/issues/{id}/reject")
    public ResponseEntity<?> rejectIssue(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, "Rejected", reason));
    }

    /** Assign a team to a complaint */
    @SuppressWarnings("null")
    @PostMapping("/issues/{id}/assign")
    public ResponseEntity<?> assignIssue(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        String headName = (String) body.get("headName");
        String headPhone = (String) body.get("headPhone");
        String startDateStr = (String) body.get("startDate");
        String endDateStr = (String) body.get("endDate");
        @SuppressWarnings("unchecked")
        List<Integer> memberIds = (List<Integer>) body.get("memberIds");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        if (startDateStr != null)
            complaint.setStartDate(LocalDateTime.parse(startDateStr, formatter));
        if (endDateStr != null) {
            LocalDateTime endDateTime = LocalDateTime.parse(endDateStr, formatter);
            complaint.setEndDate(endDateTime);
            // Automatically sync SLA Timeline with the projected End Date
            complaint.setSlaTimeline(endDateTime);
            complaint.setSlaAssigned(true);
        }

        complaint.setAssignedHeadName(headName);
        complaint.setAssignedHeadPhone(headPhone);
        complaint.setStatus("In Progress");

        // Keep reference to old employees to release them if they are dropped
        java.util.Set<User> oldEmployees = complaint.getAssignedEmployees();

        // Update employee statuses to 'Assigned' and link to complaint
        if (memberIds != null) {
            java.util.Set<User> newEmployees = new java.util.HashSet<>();

            // Set state for NEW members
            for (Integer mid : memberIds) {
                if (mid == null)
                    continue;
                User u = userRepository.findById(Long.valueOf(mid.longValue())).orElse(null);
                if (u != null) {
                    u.setStatus("Assigned");
                    userRepository.save(u); // Update status
                    newEmployees.add(u);
                }
            }

            // Release OLD members who are not in the new team
            if (oldEmployees != null) {
                for (User oldEmp : oldEmployees) {
                    if (!newEmployees.contains(oldEmp)) {
                        oldEmp.setStatus("Not Assigned");
                        userRepository.save(oldEmp);
                    }
                }
            }

            if (complaint.getAssignedEmployees() == null) {
                complaint.setAssignedEmployees(new java.util.HashSet<>());
            }
            complaint.getAssignedEmployees().clear();
            complaint.getAssignedEmployees().addAll(newEmployees);
        }

        return ResponseEntity.ok(complaintRepository.save(complaint));
    }

    /** Returns list of employees for the sub-admin's department */
    @SuppressWarnings("null")
    @GetMapping("/employees")
    public ResponseEntity<List<User>> getDepartmentEmployees(Principal principal) {
        User subAdmin = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(userRepository.findByDepartment(subAdmin.getDepartment()));
    }

    /** Create a new employee for the department */
    @SuppressWarnings("null")
    @PostMapping("/employees")
    public ResponseEntity<?> addEmployee(@RequestBody User employee, Principal principal) {
        User subAdmin = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        employee.setDepartment(subAdmin.getDepartment());
        if (employee.getStatus() == null) {
            employee.setStatus("Not Assigned");
        }
        // In a real app, we'd assign ROLE_OFFICIAL here
        return ResponseEntity.ok(userRepository.save(employee));
    }

    /** Update employee details */
    @SuppressWarnings("null")
    @PutMapping("/employees/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody User employeeDetails) {
        User employee = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        employee.setName(employeeDetails.getName());
        employee.setContact(employeeDetails.getContact());
        employee.setArea(employeeDetails.getArea());
        employee.setStatus(employeeDetails.getStatus());

        return ResponseEntity.ok(userRepository.save(employee));
    }

    /** Delete employee */
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Employee deleted successfully"));
    }

    /** Update issue status with completion proof */
    @SuppressWarnings("null")
    @PutMapping("/issues/{id}/status")
    public ResponseEntity<?> updateIssueStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String notes = body.get("resolutionNotes");
        String proof = body.get("completionProof");
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, status, notes, proof));
    }

    /** Bulk assignment for a group of complaints */
    @PostMapping("/grouped-issues/assign")
    public ResponseEntity<?> assignGroupedIssue(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> idsInt = (List<Integer>) body.get("ids");
        List<Long> ids = idsInt.stream().map(Integer::longValue).collect(java.util.stream.Collectors.toList());

        // Extract assignment details
        String headName = (String) body.get("headName");
        String headPhone = (String) body.get("headPhone");
        String startDateStr = (String) body.get("startDate");
        String endDateStr = (String) body.get("endDate");
        @SuppressWarnings("unchecked")
        List<Integer> memberIds = (List<Integer>) body.get("memberIds");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime start = startDateStr != null ? LocalDateTime.parse(startDateStr, formatter) : null;
        LocalDateTime end = endDateStr != null ? LocalDateTime.parse(endDateStr, formatter) : null;

        for (Long id : ids) {
            Complaint complaint = complaintRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));

            if (start != null)
                complaint.setStartDate(start);
            if (end != null) {
                complaint.setEndDate(end);
                complaint.setSlaTimeline(end);
                complaint.setSlaAssigned(true);
            }
            complaint.setAssignedHeadName(headName);
            complaint.setAssignedHeadPhone(headPhone);
            complaint.setStatus("In Progress");

            // Update employees
            if (memberIds != null) {
                java.util.Set<User> newEmployees = new java.util.HashSet<>();
                for (Integer mid : memberIds) {
                    User u = userRepository.findById(Long.valueOf(mid.longValue())).orElse(null);
                    if (u != null) {
                        u.setStatus("Assigned");
                        userRepository.save(u);
                        newEmployees.add(u);
                    }
                }
                complaint.setAssignedEmployees(newEmployees);
            }
            complaintRepository.save(complaint);
        }

        return ResponseEntity.ok(Map.of("message", "Grouped assignment completed successfully"));
    }
}
