package com.smartcivic.backend.service;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcivic.backend.dto.PasswordChangeRequest;
import com.smartcivic.backend.dto.ProfileUpdateRequest;
import com.smartcivic.backend.dto.UserProfileDTO;
import com.smartcivic.backend.model.User;
import com.smartcivic.backend.repository.UserRepository;

@Service
public class UserService {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        public UserProfileDTO getUserProfile(String username) {
                if ("Admin@123".equals(username)) {
                        return UserProfileDTO.builder()
                                        .id(0L)
                                        .name("System Admin")
                                        .username("Admin@123")
                                        .email("admin@smartcivic.com")
                                        .roles(java.util.Set.of("ROLE_ADMIN"))
                                        .build();
                }

                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Error: User not found."));

                System.out.println("[USER_SERVICE] Found user in DB: " + user.getUsername()
                                + ", Name: " + user.getName()
                                + ", Contact: " + user.getContact()
                                + ", District: " + user.getDistrict()
                                + ", Area: " + user.getArea()
                                + ", Roles: " + user.getRoles().size());

                return UserProfileDTO.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .contact(user.getContact())
                                .district(user.getDistrict())
                                .area(user.getArea())
                                .roles(user.getRoles().stream()
                                                .map(role -> role.getName().name())
                                                .collect(Collectors.toSet()))
                                .build();
        }

        @Transactional
        public UserProfileDTO updateProfile(String username, ProfileUpdateRequest request) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Error: User not found."));

                user.setName(request.getName());
                user.setContact(request.getContact());
                user.setDistrict(request.getDistrict());
                user.setArea(request.getArea());

                User updatedUser = userRepository.save(user);

                return getUserProfile(updatedUser.getUsername());
        }

        @Transactional
        public void changePassword(String username, PasswordChangeRequest request) {
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Error: User not found."));

                if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                        throw new RuntimeException("Error: Current password is incorrect.");
                }

                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                userRepository.save(user);
        }
}
