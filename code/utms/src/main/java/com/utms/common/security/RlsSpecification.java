package com.utms.common.security;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

/**
 * Provides JPA Specifications for Row-Level Security (RLS) filtering.
 * Scoped users (HOD, Coordinator) only see data within their campus/department.
 * Admin and Registrar users see all data.
 *
 * Usage in service layer:
 *   Specification<Campus> spec = RlsSpecification.campusScope();
 *   campusRepository.findAll(spec.and(otherSpec), pageable);
 */
public final class RlsSpecification {

    private RlsSpecification() {
    }

    /**
     * Restricts Campus queries to the user's assigned campus.
     * Admin/Registrar bypass this filter (see all campuses).
     */
    public static <T> Specification<T> campusScope() {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (UserContextHolder.hasInstitutionWideAccess()) {
                return cb.conjunction(); // no restriction
            }
            Long campusId = UserContextHolder.getCampusId();
            if (campusId == null) {
                return cb.conjunction(); // no campus context, return all (fail-open for dev)
            }
            return cb.equal(root.get("id"), campusId);
        };
    }

    /**
     * Restricts entity queries to the user's assigned campus via a campus_id FK.
     * Use for Department, Room, etc. that have a direct campus_id reference.
     */
    public static <T> Specification<T> byCampusId() {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (UserContextHolder.hasInstitutionWideAccess()) {
                return cb.conjunction();
            }
            Long campusId = UserContextHolder.getCampusId();
            if (campusId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("campus").get("id"), campusId);
        };
    }

    /**
     * Restricts entity queries to the user's assigned department via a department_id FK.
     * Use for Program, Course, Faculty, etc. that have a direct department_id reference.
     */
    public static <T> Specification<T> byDepartmentId() {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (UserContextHolder.hasInstitutionWideAccess()) {
                return cb.conjunction();
            }
            Long departmentId = UserContextHolder.getDepartmentId();
            if (departmentId == null) {
                // Fall back to campus-level scoping if no department
                Long campusId = UserContextHolder.getCampusId();
                if (campusId == null) {
                    return cb.conjunction();
                }
                return cb.equal(root.get("department").get("campus").get("id"), campusId);
            }
            return cb.equal(root.get("department").get("id"), departmentId);
        };
    }

    /**
     * Combines soft-delete filter with RLS campus scope.
     */
    public static <T> Specification<T> activeAndCampusScoped() {
        Specification<T> notDel = notDeleted();
        Specification<T> campus = byCampusId();
        return Specification.where(notDel).and(campus);
    }

    /**
     * Combines soft-delete filter with RLS department scope.
     */
    public static <T> Specification<T> activeAndDepartmentScoped() {
        Specification<T> notDel = notDeleted();
        Specification<T> dept = byDepartmentId();
        return Specification.where(notDel).and(dept);
    }

    /**
     * Filters out soft-deleted records.
     */
    public static <T> Specification<T> notDeleted() {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) ->
                cb.isNull(root.get("deletedAt"));
    }
}
