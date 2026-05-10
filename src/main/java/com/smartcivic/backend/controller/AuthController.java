package com.smartcivic.backend.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcivic.backend.dto.JwtResponse;
import com.smartcivic.backend.dto.LoginRequest;
import com.smartcivic.backend.dto.MessageResponse;
import com.smartcivic.backend.dto.SignupRequest;
import com.smartcivic.backend.model.Role;
import com.smartcivic.backend.model.RoleName;
import com.smartcivic.backend.model.User;
import com.smartcivic.backend.repository.RoleRepository;
import com.smartcivic.backend.repository.UserRepository;
import com.smartcivic.backend.security.JwtUtils;
import com.smartcivic.backend.security.UserDetailsImpl;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
        private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

        @Autowired
        AuthenticationManager authenticationManager;

        @Autowired
        UserRepository userRepository;

        @Autowired
        RoleRepository roleRepository;

        @Autowired
        PasswordEncoder encoder;

        @Autowired
        JwtUtils jwtUtils;

        @PostMapping("/signin")
        public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
                logger.info("[AUTH_DEBUG] Attempting login for user: {}", loginRequest.getUsername());

                // PRIORITIZED: Virtual Admin Check (Not in DB)
                if ("Admin@123".equals(loginRequest.getUsername())
                                && "Welcome@123".equals(loginRequest.getPassword())) {
                        logger.info("[AUTH_DEBUG] Priority Admin Login successful (Virtual Bypass)");
                        List<String> roles = List.of("ROLE_ADMIN");

                        UserDetailsImpl userDetails = new UserDetailsImpl(
                                        0L,
                                        "Admin@123",
                                        "admin@smartcivic.com",
                                        null, // No password needed in principal
                                        roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));

                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        String jwt = jwtUtils.generateJwtToken(authentication);

                        return ResponseEntity.ok(new JwtResponse(jwt,
                                        userDetails.getId(),
                                        userDetails.getUsername(),
                                        userDetails.getEmail(),
                                        roles,
                                        null, null, null));
                }

                // SECONDARY: Check Database for others
                logger.info("[AUTH_DEBUG] Checking DB for user: {}", loginRequest.getUsername());
                Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
                if (userOpt.isEmpty()) {
                        userOpt = userRepository.findByEmail(loginRequest.getUsername());
                }

                if (!userOpt.isPresent()) {
                        logger.error(
                                        "[AUTH_DEBUG] Login failure: User '{}' NOT FOUND in database (checked username and email). This is why authentication is failing.",
                                        loginRequest.getUsername());
                        return ResponseEntity.status(401)
                                        .body(new MessageResponse("Error: User not found with username: "
                                                        + loginRequest.getUsername()));
                }

                try {
                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                                                        loginRequest.getPassword()));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        String jwt = jwtUtils.generateJwtToken(authentication);

                        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                        List<String> roles = userDetails.getAuthorities().stream()
                                        .map(item -> item.getAuthority())
                                        .collect(Collectors.toList());

                        // Fetch profile details from DB for direct dashboard sync
                        User user = userRepository.findByUsername(userDetails.getUsername())
                                        .orElseThrow(() -> new RuntimeException("Error: User not found."));

                        String department = user.getDepartment();
                        String area = user.getArea();
                        String district = user.getDistrict();

                        logger.info("Login successful for user: {}, Area: {}", loginRequest.getUsername(), area);
                        return ResponseEntity.ok(new JwtResponse(jwt,
                                        userDetails.getId(),
                                        userDetails.getUsername(),
                                        userDetails.getEmail(),
                                        roles,
                                        department,
                                        area,
                                        district));
                } catch (Exception e) {
                        logger.error("[AUTH_VERSION_CHECK] Running Code v6 (Optional Logic Fix)");
                        logger.error("[AUTH_DEBUG] Login failure for user {}: {}. Password length: {}",
                                        loginRequest.getUsername(), e.getMessage(),
                                        (loginRequest.getPassword() != null ? loginRequest.getPassword().length()
                                                        : "NULL"));

                        if (e instanceof org.springframework.security.authentication.BadCredentialsException) {
                                return ResponseEntity.status(401)
                                                .body(new MessageResponse("Error: Invalid username or password."));
                        }
                        return ResponseEntity.status(500)
                                        .body(new MessageResponse("Error: Internal server error during login - "
                                                        + e.getMessage()));
                }
        }

        @PostMapping("/signup")
        public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
                if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                        return ResponseEntity
                                        .badRequest()
                                        .body(new MessageResponse("Error: Username is already taken!"));
                }

                if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                        return ResponseEntity
                                        .badRequest()
                                        .body(new MessageResponse("Error: Email is already in use!"));
                }

                // Create new user's account
                User user = User.builder()
                                .username(signUpRequest.getUsername())
                                .name(signUpRequest.getName())
                                .email(signUpRequest.getEmail())
                                .contact(signUpRequest.getContact())
                                .district(signUpRequest.getDistrict())
                                .area(signUpRequest.getArea())
                                .password(encoder.encode(signUpRequest.getPassword()))
                                .build();

                Set<String> strRoles = signUpRequest.getRole();
                Set<Role> roles = new HashSet<>();

                if (strRoles == null) {
                        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                                        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                } else {
                        strRoles.forEach(role -> {
                                switch (role.toLowerCase()) {
                                        case "admin":
                                                Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                                                                .orElseThrow(() -> new RuntimeException(
                                                                                "Error: Role is not found."));
                                                roles.add(adminRole);
                                                break;
                                        case "sub_admin":
                                                Role subAdminRole = roleRepository.findByName(RoleName.ROLE_SUB_ADMIN)
                                                                .orElseThrow(() -> new RuntimeException(
                                                                                "Error: Role is not found."));
                                                roles.add(subAdminRole);
                                                break;
                                        default:
                                                Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                                                                .orElseThrow(() -> new RuntimeException(
                                                                                "Error: Role is not found."));
                                                roles.add(userRole);
                                }
                        });
                }

                user.setRoles(roles);
                User savedUser = userRepository.save(user);
                logger.info("[AUTH_DEBUG] User registered successfully: {}. Password Hash: {}", savedUser.getUsername(),
                                savedUser.getPassword());

                return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
        }
}
