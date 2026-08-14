package com.utms.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter that populates UserContextHolder from the authenticated user's JWT claims.
 * Runs after Spring Security authentication filter.
 * 
 * In production, campusId and departmentId are extracted from JWT custom claims.
 * For now, they can also be passed as headers for development/testing purposes.
 */
@Component
@Slf4j
public class RlsFilter extends OncePerRequestFilter {

    private static final String HEADER_CAMPUS_ID = "X-Campus-Id";
    private static final String HEADER_DEPARTMENT_ID = "X-Department-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                UserContext context = buildUserContext(authentication, request);
                UserContextHolder.setContext(context);
                log.debug("RLS context set: userId={}, campusId={}, departmentId={}, roles={}",
                        context.getUserId(), context.getCampusId(), context.getDepartmentId(), context.getRoles());
            }

            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private UserContext buildUserContext(Authentication authentication, HttpServletRequest request) {
        UserContext context = new UserContext();
        context.setUserId(authentication.getName());

        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        context.setRoles(roles);

        // Extract campus/department from JWT claims (or headers for dev)
        // In production, these come from JWT custom claims parsed by the JWT filter
        String campusIdHeader = request.getHeader(HEADER_CAMPUS_ID);
        String departmentIdHeader = request.getHeader(HEADER_DEPARTMENT_ID);

        if (campusIdHeader != null && !campusIdHeader.isBlank()) {
            try {
                context.setCampusId(Long.parseLong(campusIdHeader));
            } catch (NumberFormatException e) {
                log.warn("Invalid X-Campus-Id header value: {}", campusIdHeader);
            }
        }

        if (departmentIdHeader != null && !departmentIdHeader.isBlank()) {
            try {
                context.setDepartmentId(Long.parseLong(departmentIdHeader));
            } catch (NumberFormatException e) {
                log.warn("Invalid X-Department-Id header value: {}", departmentIdHeader);
            }
        }

        return context;
    }
}
