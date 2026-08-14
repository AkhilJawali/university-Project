package com.utms.common.security;

/**
 * Thread-local holder for the current user's context.
 * Set by the security filter on each request, cleared after response.
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void setContext(UserContext context) {
        CONTEXT.set(context);
    }

    public static UserContext getContext() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Convenience: get campusId from context, or null if not set / institution-wide user.
     */
    public static Long getCampusId() {
        UserContext ctx = getContext();
        return ctx != null ? ctx.getCampusId() : null;
    }

    /**
     * Convenience: get departmentId from context, or null if not set / institution-wide user.
     */
    public static Long getDepartmentId() {
        UserContext ctx = getContext();
        return ctx != null ? ctx.getDepartmentId() : null;
    }

    /**
     * Returns true if the current user has institution-wide access and should bypass RLS.
     */
    public static boolean hasInstitutionWideAccess() {
        UserContext ctx = getContext();
        return ctx != null && ctx.hasInstitutionWideAccess();
    }
}
