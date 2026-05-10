package com.smartcivic.backend.controller;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.smartcivic.backend.repository.ComplaintRepository;
import com.smartcivic.backend.dto.EditUserRequest;
import com.smartcivic.backend.dto.CreateUserRequest;
import com.smartcivic.backend.dto.AssignSubAdminRequest;
import com.smartcivic.backend.dto.MessageResponse;
import com.smartcivic.backend.model.Complaint;
import org.springframework.web.bind.annotation.PutMapping;
import java.util.Map;
import com.smartcivic.backend.model.Role;
import com.smartcivic.backend.model.RoleName;
import com.smartcivic.backend.model.User;
import com.smartcivic.backend.repository.RoleRepository;
import com.smartcivic.backend.repository.UserRepository;
import com.smartcivic.backend.service.ComplaintService;
import org.springframework.security.crypto.password.PasswordEncoder;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@SuppressWarnings("null") @PathVariable Long id) {
        if (id != null) {
            userRepository.deleteById(id);
        }
        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }

    @GetMapping("/subadmins")
    public List<User> getAllSubAdmins() {
        Role subAdminRole = roleRepository.findByName(RoleName.ROLE_SUB_ADMIN)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(subAdminRole))
                .collect(Collectors.toList());
    }

    @PostMapping("/complaints/{id}/assign")
    public ResponseEntity<?> assignComplaint(@PathVariable Long id, @RequestBody AssignSubAdminRequest request) {
        LocalDateTime sla = LocalDateTime.parse(request.getSlaDate());
        complaintService.assignSubAdmin(id, request.getSubAdminId(), sla);
        return ResponseEntity.ok(new MessageResponse("Complaint assigned successfully"));
    }

    @DeleteMapping("/subadmins/{id}")
    public ResponseEntity<?> deleteSubAdmin(@PathVariable Long id) {
        if (id != null) {
            userRepository.deleteById(id);
        }
        return ResponseEntity.ok(new MessageResponse("Sub Admin deleted successfully"));
    }

    @PutMapping("/complaints/{id}/priority")
    public ResponseEntity<?> updatePriority(@PathVariable Long id, @RequestBody Map<String, String> body) {
        if (id == null) {
            throw new RuntimeException("ID cannot be null");
        }
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        String priority = body.get("priority");
        String status = body.get("status");
        String slaDate = body.get("slaDate");

        if (priority != null && !priority.isBlank()) {
            complaint.setPriorityLevel(priority);
        }

        if (status != null && !status.isBlank()) {
            complaint.setStatus(status);
        }

        if (slaDate != null && !slaDate.isBlank()) {
            try {
                // Ensure the date string from frontend is parsed into LocalDateTime
                // Try ISO_DATE_TIME first, then fallback if needed
                try {
                    complaint.setSlaTimeline(LocalDateTime.parse(slaDate));
                } catch (Exception e) {
                    // Start of day fallback if only date provided
                    try {
                        complaint.setSlaTimeline(java.time.LocalDate.parse(slaDate).atStartOfDay());
                    } catch (Exception ex) {
                        System.err.println("Failed to parse SLA Date: " + slaDate);
                    }
                }
                complaint.setSlaAssigned(true);
            } catch (Exception e) {
                // Ignore parse errors or log as needed
            }
        }

        // Final safety check for SLA assignment
        if (complaint.getSlaTimeline() != null) {
            complaint.setSlaAssigned(true);
        }

        return ResponseEntity.ok(complaintRepository.save(complaint));
    }

    @GetMapping("/complaints/stats")
    public ResponseEntity<?> getOversightStats() {
        List<Complaint> all = complaintRepository.findAll();
        long total = all.size();
        long slaAssigned = all.stream().filter(Complaint::isSlaAssigned).count();
        long slaNotAssigned = total - slaAssigned;

        // DEBUG: Log breakdown of assigned employees
        all.forEach(c -> {
            if (c.getAssignedEmployees() != null && !c.getAssignedEmployees().isEmpty()) {
                System.out.println(
                        "Complaint #" + c.getId() + " has " + c.getAssignedEmployees().size() + " assigned employees.");
            }
        });

        return ResponseEntity.ok(Map.of(
                "totalSubmissions", total,
                "slaAssigned", slaAssigned,
                "slaNotAssigned", slaNotAssigned));
    }

    @PostMapping("/users/create")
    public ResponseEntity<?> createAdministrativeUser(@RequestBody CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role not found."));
            roles.add(userRole);
        } else {
            request.getRoles().forEach(role -> {
                switch (role.toUpperCase()) {
                    case "ADMIN":
                        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role not found."));
                        roles.add(adminRole);
                        break;
                    case "SUB_ADMIN":
                        Role subAdminRole = roleRepository.findByName(RoleName.ROLE_SUB_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role not found."));
                        roles.add(subAdminRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role not found."));
                        roles.add(userRole);
                }
            });
        }

        @SuppressWarnings("null")
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .contact(request.getContact())
                .district(request.getDistrict())
                .area(request.getArea())
                .department(request.getDepartment())
                .roles(roles)
                .build();

        if (user != null) {
            userRepository.save(user);
        }
        return ResponseEntity.ok(new MessageResponse("User created successfully by Administrator"));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody EditUserRequest request) {
        if (id == null) {
            throw new RuntimeException("ID cannot be null");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: User not found."));

        user.setName(request.getName());
        user.setContact(request.getContact());
        user.setDistrict(request.getDistrict());
        user.setArea(request.getArea());

        // Department update logic (optional if you want to restrict)
        if (request.getDepartment() != null && !request.getDepartment().isEmpty()) {
            user.setDepartment(request.getDepartment());
        }

        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("User updated successfully"));
    }
}
