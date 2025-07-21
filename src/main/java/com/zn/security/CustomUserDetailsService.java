package com.zn.security;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.zn.adminentity.Admin;
import com.zn.adminrepo.IAdminRepo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private IAdminRepo adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Attempting to load admin by email: {}", email);
        Admin admin = adminRepository.findByEmail(email);
        
        if (admin == null) {
            log.warn("Admin not found with email: {}", email);
            throw new UsernameNotFoundException("Admin not found with email: " + email);
        }        log.info("Admin found: {} (role: {})", admin.getEmail(), admin.getRole());
        return User.builder()
                .username(admin.getEmail())
                .password(admin.getPassword()) // Password should be encoded
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + admin.getRole())))
                .build();
    }
}
