package com.utms.common.security;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Holds the current authenticated user's context for RLS scoping.
 * Populated from JWT claims by the security filter.
 */
@Getter
@Setter
public class UserContext {

    private String userId;
    private String email;
    private Set<String> roles;
    private Long campusId;
    private Long departmentId;

    /**
     * Returns true if the user has institution-wide access (Admin, Registrar).
     * These users bypass campus/department scoping.
     */
    public boolean hasInstitutionWideAccess() {
        return roles != null && (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_REGISTRAR"));
    }

    /**
     * Returns true if the user is scoped to a specific campus/department (HOD, Coordinator).
     */
    public boolean isScopedUser() {
        return !hasInstitutionWideAccess();
    }
}
