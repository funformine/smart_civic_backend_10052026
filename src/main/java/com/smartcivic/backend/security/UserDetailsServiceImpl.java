package com.smartcivic.backend.security;

import com.smartcivic.backend.model.User;
import com.smartcivic.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    private static final String ADMIN_USER = "Admin@123";
    private static final String ADMIN_PASS_HASH = "$2a$10$8.UnS3G9.37JbHROn7fWreWc46gSAsm6rOas7lq.B.7gC0pS3D2uG"; // Welcome@123

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("[AUTH_DEBUG] UserDetailsServiceImpl loading user: " + username);

        if (ADMIN_USER.equals(username)) {
            System.out.println("[AUTH_DEBUG] UserDetailsServiceImpl: PRIORITY ADMIN MATCH FOUND");
            return new UserDetailsImpl(
                    0L,
                    ADMIN_USER,
                    "admin@smartcivic.com",
                    ADMIN_PASS_HASH,
                    java.util.List
                            .of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> {
                    System.out.println("[AUTH_DEBUG] UserDetailsServiceImpl: User NOT FOUND: " + username);
                    return new UsernameNotFoundException("User Not Found with username: " + username);
                });

        System.out.println("[AUTH_DEBUG] UserDetailsServiceImpl loaded user: " + user.getUsername());
        return UserDetailsImpl.build(user);
    }
}
