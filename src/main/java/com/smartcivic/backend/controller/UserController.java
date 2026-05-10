package com.smartcivic.backend.controller;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartcivic.backend.dto.MessageResponse;
import com.smartcivic.backend.dto.PasswordChangeRequest;
import com.smartcivic.backend.dto.ProfileUpdateRequest;
import com.smartcivic.backend.dto.UserProfileDTO;
import com.smartcivic.backend.service.UserService;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUB_ADMIN')")
    public ResponseEntity<UserProfileDTO> getProfile(Principal principal) {
        logger.info("[PROFILE_DEBUG] Fetching profile for user: {}", principal.getName());
        UserProfileDTO profile = userService.getUserProfile(principal.getName());
        logger.info("[PROFILE_DEBUG] Profile data found: Name={}, Email={}", profile.getName(), profile.getEmail());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUB_ADMIN')")
    public ResponseEntity<UserProfileDTO> updateProfile(@RequestBody ProfileUpdateRequest request,
            Principal principal) {
        logger.info("[PROFILE_DEBUG] Updating profile for user: {}", principal.getName());
        return ResponseEntity.ok(userService.updateProfile(principal.getName(), request));
    }

    @PutMapping("/password")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasRole('SUB_ADMIN')")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest request, Principal principal) {
        logger.info("[PROFILE_DEBUG] Changing password for user: {}", principal.getName());
        try {
            userService.changePassword(principal.getName(), request);
            return ResponseEntity.ok(new MessageResponse("Password changed successfully!"));
        } catch (Exception e) {
            logger.error("[PROFILE_DEBUG] Password change failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}
