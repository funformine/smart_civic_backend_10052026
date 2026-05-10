package com.smartcivic.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartcivic.backend.dto.MessageResponse;
import com.smartcivic.backend.model.Complaint;
import com.smartcivic.backend.service.ComplaintService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {
    @Autowired
    private ComplaintService complaintService;

    @PostMapping("/raise")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> raiseComplaint(@RequestBody Complaint complaint, Principal principal) {
        return ResponseEntity.ok(complaintService.raiseComplaint(complaint, principal.getName()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public List<Complaint> getMyComplaints(Principal principal) {
        if (principal == null) {
            System.out
                    .println("[CONTROLLER_DEBUG] Principal is NULL in /my - User might not be authenticated properly");
            throw new RuntimeException("User not authenticated");
        }
        System.out.println("[CONTROLLER_DEBUG] Fetching complaints for user: " + principal.getName());
        return complaintService.getComplaintsByUser(principal.getName());
    }

    @GetMapping("/my/paginated")
    @PreAuthorize("hasRole('USER')")
    public Page<Complaint> getMyComplaintsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            Principal principal) {
        return complaintService.getComplaintsByUserPaginated(principal.getName(), page, size);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getDashboardSummary(Principal principal) {
        return ResponseEntity.ok(complaintService.getDashboardSummary(principal.getName()));
    }

    @GetMapping("/area")
    public List<Complaint> getComplaintsByArea(@RequestParam String location) {
        return complaintService.getComplaintsInArea(location);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Complaint> getAllComplaints() {
        List<Complaint> all = complaintService.getAllComplaints();
        System.out.println("[COMPLAINT_DEBUG] Fetching all complaints. Count: " + all.size());
        all.forEach(c -> {
            if (c.getAssignedEmployees() != null) {
                System.out.println("[COMPLAINT_DEBUG] Complaint #" + c.getId() + " has "
                        + c.getAssignedEmployees().size() + " employees.");
                c.getAssignedEmployees().forEach(e -> System.out.println("   - " + e.getName()));
            } else {
                System.out.println("[COMPLAINT_DEBUG] Complaint #" + c.getId() + " has NULL assignedEmployees set.");
            }
        });
        return all;
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUB_ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, status, notes));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectComplaint(@PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(complaintService.rejectComplaint(id, reason));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateComplaint(@PathVariable Long id, @RequestBody Complaint complaint,
            Principal principal) {
        return ResponseEntity.ok(complaintService.updateComplaint(id, complaint, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteComplaint(@PathVariable Long id, Principal principal) {
        complaintService.deleteComplaint(id, principal.getName());
        return ResponseEntity.ok(new MessageResponse("Complaint deleted successfully"));
    }
}
