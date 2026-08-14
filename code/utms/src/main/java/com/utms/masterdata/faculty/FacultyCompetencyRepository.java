package com.utms.masterdata.faculty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyCompetencyRepository extends JpaRepository<FacultyCompetency, Long> {

    List<FacultyCompetency> findAllByFacultyIdAndDeletedAtIsNull(Long facultyId);

    Optional<FacultyCompetency> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByFacultyIdAndCourseIdAndDeletedAtIsNull(Long facultyId, Long courseId);
}
