package com.utms.masterdata.program;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long>, JpaSpecificationExecutor<Program> {

    Optional<Program> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCodeAndDepartmentIdAndDeletedAtIsNull(String code, Long departmentId);

    boolean existsByCodeAndDepartmentIdAndIdNotAndDeletedAtIsNull(String code, Long departmentId, Long id);

    long countByDepartmentIdAndDeletedAtIsNull(Long departmentId);

    Page<Program> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Program> findAllByDepartmentIdAndDeletedAtIsNull(Long departmentId, Pageable pageable);

    List<Program> findAllByDepartmentIdAndDeletedAtIsNull(Long departmentId);
}
