package com.zn.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        log.info("[JWT Filter] Authorization header: {}", authorizationHeader);

        String username = null;
        String jwt = null;

        // First check Authorization header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            log.info("[JWT Filter] JWT found in Authorization header");
        } 
        // If no Authorization header, check cookies
        else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("admin_jwt".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    log.info("[JWT Filter] JWT found in cookie");
                    break;
                }
            }
        }

        if (jwt != null) {
            try {
                username = jwtUtil.extractUsername(jwt);
                log.info("[JWT Filter] Extracted username from JWT: {}", username);
            } catch (Exception e) {
                log.error("[JWT Filter] JWT token extraction failed: {}", e.getMessage());
            }
        } else {
            log.info("[JWT Filter] No JWT found in header or cookies");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            log.info("[JWT Filter] Loaded user details for: {}", username);

            if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                log.info("[JWT Filter] JWT is valid for user: {}", username);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                log.warn("[JWT Filter] JWT is invalid for user: {}", username);
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        // Skip JWT filter for login endpoint
        return "/admin/api/admin/login".equals(path);
    }
}
