package com.utms.masterdata.faculty;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long>, JpaSpecificationExecutor<Faculty> {

    Optional<Faculty> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByEmployeeIdAndDeletedAtIsNull(String employeeId);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmployeeIdAndIdNotAndDeletedAtIsNull(String employeeId, Long id);

    boolean existsByEmailAndIdNotAndDeletedAtIsNull(String email, Long id);

    Page<Faculty> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Faculty> findAllByDepartmentIdAndDeletedAtIsNull(Long departmentId, Pageable pageable);

    Page<Faculty> findAllByCadreAndDeletedAtIsNull(Cadre cadre, Pageable pageable);

    @Query("SELECT f FROM Faculty f WHERE f.deletedAt IS NULL AND f.department.id = :departmentId AND f.cadre = :cadre")
    Page<Faculty> findAllByDepartmentIdAndCadreAndDeletedAtIsNull(
            @Param("departmentId") Long departmentId,
            @Param("cadre") Cadre cadre,
            Pageable pageable);

    @Query("SELECT f FROM Faculty f WHERE f.deletedAt IS NULL " +
           "AND (LOWER(f.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(f.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(f.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(f.employeeId) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Faculty> searchByNameOrEmailOrEmployeeId(@Param("search") String search, Pageable pageable);
}
