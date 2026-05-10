package com.smartcivic.backend.config;

import com.smartcivic.backend.model.Complaint;
import com.smartcivic.backend.model.Role;
import com.smartcivic.backend.model.RoleName;
import com.smartcivic.backend.model.User;
import com.smartcivic.backend.repository.ComplaintRepository;
import com.smartcivic.backend.repository.RoleRepository;
import com.smartcivic.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private ComplaintRepository complaintRepository;

        @Autowired
        private PasswordEncoder encoder;

        @Autowired
        private com.smartcivic.backend.repository.IssueTypeRepository issueTypeRepository;

        @Override
        public void run(String... args) throws Exception {
                // Initialize Roles if not exists
                for (RoleName rn : RoleName.values()) {
                        if (roleRepository.findByName(rn).isEmpty()) {
                                roleRepository.save(new Role(null, rn));
                        }
                }

                Role subAdminRole = roleRepository.findByName(RoleName.ROLE_SUB_ADMIN).get();
                Role userRole = roleRepository.findByName(RoleName.ROLE_USER).get();
                Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).get();

                // Admin User is now managed virtually in the code, not in the database.

                // Seed a Sub-Admin for testing
                seedSubAdmin("Sub Admin", "subadmin", "subadmin@smartcivic.com", "Power", subAdminRole);

                // Seed a User to report issues
                User reporter = seedUser("Test User", "user", "user@smartcivic.com", userRole);

                // Seed 10 Tamil Nadu Names as employees
                seedEmployee("Arun Pandian", "arun.p", "arun@smartcivic.com", "9876543210", "Anna Nagar", "Chennai",
                                "Power",
                                "Assigned", subAdminRole);
                seedEmployee("Karthik Raja", "karthik.r", "karthik@smartcivic.com", "9876543211", "Adyar", "Chennai",
                                "Water",
                                "Not Assigned", subAdminRole);
                seedEmployee("Meena Kumari", "meena.k", "meena@smartcivic.com", "9876543212", "T Nagar", "Chennai",
                                "Land",
                                "Not Available", subAdminRole);
                seedEmployee("Priya Dharshini", "priya.d", "priya@smartcivic.com", "9876543213", "Mylapore", "Chennai",
                                "Power",
                                "Not Assigned", subAdminRole);
                seedEmployee("Senthil Kumar", "senthil.k", "senthil@smartcivic.com", "9876543214", "Velachery",
                                "Chennai",
                                "Water", "Assigned", subAdminRole);
                seedEmployee("Anitha Lakshmi", "anitha.l", "anitha@smartcivic.com", "9876543215", "OMR", "Chennai",
                                "Land",
                                "Not Assigned", subAdminRole);
                seedEmployee("Ravichandran", "ravi.c", "ravi@smartcivic.com", "9876543216", "Guindy", "Chennai",
                                "Power",
                                "Not Available", subAdminRole);
                seedEmployee("Muthu Vel", "muthu.v", "muthu@smartcivic.com", "9876543217", "Saidapet", "Chennai",
                                "Water",
                                "Not Assigned", subAdminRole);
                seedEmployee("Deepa Sree", "deepa.s", "deepa@smartcivic.com", "9876543218", "Tambaram", "Chennai",
                                "Land",
                                "Assigned", subAdminRole);
                seedEmployee("Sivakumar", "siva.k", "siva@smartcivic.com", "9876543219", "Chrompet", "Chennai", "Power",
                                "Not Assigned", subAdminRole);

                // Seed sample complaints
                seedComplaint("[Power] Street light not working",
                                "The street light in Anna Nagar is broken and flickering.", "Anna Nagar, Chennai",
                                "Pending", "Power", reporter);
                seedComplaint("[Power] Transformer Sparking", "Sparks observed near the transformer at main road.",
                                "Adyar, Chennai", "Accepted", "Power", reporter);
                seedComplaint("[Water] Pipe Leakage", "Large water pipe leak in T Nagar area.", "T Nagar, Chennai",
                                "Pending", "Water", reporter);

                // Seed Issue Types if empty
                if (issueTypeRepository.count() == 0) {
                        seedIssueType("Power", "No power supply");
                        seedIssueType("Power", "Voltage fluctuation");
                        seedIssueType("Power", "Meter issue");
                        seedIssueType("Power", "Cable damage");

                        seedIssueType("Water", "No water supply");
                        seedIssueType("Water", "Pipeline leakage");
                        seedIssueType("Water", "Dirty water");
                        seedIssueType("Water", "Low pressure");

                        seedIssueType("Land", "Encroachment");
                        seedIssueType("Land", "Property dispute");
                        seedIssueType("Land", "Illegal building");
                        seedIssueType("Land", "Waste dumping");
                }
        }

        private void seedIssueType(String cat, String name) {
                issueTypeRepository.save(com.smartcivic.backend.model.IssueType.builder()
                                .category(cat)
                                .issueName(name)
                                .build());
        }

        private User seedUser(String name, String username, String email, Role role) {
                if (!userRepository.existsByUsername(username)) {
                        User user = new User();
                        user.setName(name);
                        user.setUsername(username);
                        user.setEmail(email);
                        user.setPassword(encoder.encode("Welcome@123"));
                        Set<Role> roles = new HashSet<>();
                        roles.add(role);
                        user.setRoles(roles);
                        return userRepository.save(user);
                }
                return userRepository.findByUsername(username).get();
        }

        private void seedSubAdmin(String name, String username, String email, String dept, Role role) {
                if (!userRepository.existsByUsername(username)) {
                        User user = new User();
                        user.setName(name);
                        user.setUsername(username);
                        user.setEmail(email);
                        user.setDepartment(dept);
                        user.setPassword(encoder.encode("Welcome@123"));
                        Set<Role> roles = new HashSet<>();
                        roles.add(role);
                        user.setRoles(roles);
                        userRepository.save(user);
                }
        }

        private void seedComplaint(String category, String desc, String loc, String status, String dept,
                        User reporter) {
                if (complaintRepository.findByCategoryContainingIgnoreCase(category).isEmpty()) {
                        Complaint c = new Complaint();
                        c.setCategory(category);
                        c.setDescription(desc);
                        c.setLocation(loc);
                        c.setStatus(status);
                        c.setRaisedDate(java.time.LocalDateTime.now());
                        c.setReporter(reporter);
                        complaintRepository.save(c);
                }
        }

        private void seedEmployee(String name, String username, String email, String contact, String area,
                        String district,
                        String dept, String status, Role role) {
                if (!userRepository.existsByUsername(username)) {
                        User user = new User();
                        user.setName(name);
                        user.setUsername(username);
                        user.setEmail(email);
                        user.setContact(contact);
                        user.setArea(area);
                        user.setDistrict(district);
                        user.setDepartment(dept);
                        user.setStatus(status);
                        user.setPassword(encoder.encode("Welcome@123"));

                        Set<Role> roles = new HashSet<>();
                        roles.add(role);
                        user.setRoles(roles);

                        userRepository.save(user);
                }
        }
}
