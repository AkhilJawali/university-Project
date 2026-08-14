package com.utms.masterdata.department;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long>, JpaSpecificationExecutor<Department> {

    Optional<Department> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCodeAndCampusIdAndDeletedAtIsNull(String code, Long campusId);

    boolean existsByCodeAndCampusIdAndIdNotAndDeletedAtIsNull(String code, Long campusId, Long id);

    long countByCampusIdAndDeletedAtIsNull(Long campusId);

    Page<Department> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Department> findAllByCampusIdAndDeletedAtIsNull(Long campusId, Pageable pageable);

    @Query("SELECT d FROM Department d WHERE d.deletedAt IS NULL " +
           "AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Department> searchByNameOrCode(@Param("search") String search, Pageable pageable);

    List<Department> findAllByCampusIdAndDeletedAtIsNull(Long campusId);
}
