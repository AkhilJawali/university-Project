package com.utms.masterdata.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    Optional<Course> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndIdNotAndDeletedAtIsNull(String code, Long id);

    Page<Course> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Course> findAllByDepartmentIdAndDeletedAtIsNull(Long departmentId, Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.deletedAt IS NULL AND c.courseType = :type")
    Page<Course> findAllByCourseTypeAndDeletedAtIsNull(@Param("type") CourseType type, Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.deletedAt IS NULL " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Course> searchByNameOrCode(@Param("search") String search, Pageable pageable);
}
